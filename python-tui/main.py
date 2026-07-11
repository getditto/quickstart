#!/usr/bin/env python3
"""Ditto Python quickstart — a Tasks console app that syncs peer-to-peer.

This mirrors the other quickstart apps (rust-tui, go-tui, javascript-tui): it
manages a shared "tasks" collection with the canonical cross-SDK schema so it
interoperates with every other quickstart app.

    { "_id": str, "title": str, "done": bool, "deleted": bool }

Two modes are selected by environment (see the README):

  * cloud   — Big Peer sync via an Online Playground identity
              (DITTO_APP_ID, DITTO_PLAYGROUND_TOKEN, DITTO_AUTH_URL)
  * offline — small-peers-only P2P using an offline license token
              (DITTO_OFFLINE_TOKEN), no cloud required
"""

from __future__ import annotations

import argparse
import asyncio
import os
import shutil
import sys
import tempfile
import uuid
from dataclasses import dataclass
from datetime import timedelta
from typing import Any

from dotenv import load_dotenv

from ditto import (
    AuthenticationProvider,
    Ditto,
    DittoConfig,
    DittoConfigConnect,
    DittoLogger,
    DittoTransportConfig,
    LogLevel,
    QueryResult,
)

# The observer view hides soft-deleted tasks; the subscription syncs everything
# so tombstones propagate to other peers.
TASKS_QUERY = "SELECT * FROM tasks WHERE deleted = false ORDER BY title"
SUBSCRIPTION_QUERY = "SELECT * FROM tasks"


@dataclass
class Task:
    id: str
    title: str
    done: bool

    @classmethod
    def from_value(cls, value: dict[str, Any]) -> "Task":
        return cls(
            id=str(value["_id"]),
            title=str(value.get("title", "")),
            done=bool(value.get("done", False)),
        )


@dataclass
class Settings:
    mode: str  # "cloud" or "offline"
    database_id: str
    persistence_directory: str
    auth_url: str | None = None
    playground_token: str | None = None
    offline_token: str | None = None


def load_settings(*, force_offline: bool = False) -> Settings:
    """Read configuration from the environment (and a nearest .env file).

    Cloud mode is chosen when DITTO_APP_ID is set; otherwise the app runs
    offline with DITTO_OFFLINE_TOKEN. ``force_offline`` is used by --smoke.
    """

    load_dotenv()  # walks up from the current directory to find a .env

    app_id = os.environ.get("DITTO_APP_ID", "").strip()
    offline_token = os.environ.get("DITTO_OFFLINE_TOKEN", "").strip()
    persistence_directory = tempfile.mkdtemp(prefix="ditto-python-tui-")

    use_cloud = bool(app_id) and not force_offline
    if use_cloud:
        token = os.environ.get("DITTO_PLAYGROUND_TOKEN", "").strip()
        auth_url = os.environ.get("DITTO_AUTH_URL", "").strip()
        if not token or not auth_url:
            raise SystemExit(
                "Cloud mode needs DITTO_PLAYGROUND_TOKEN and DITTO_AUTH_URL "
                "(set DITTO_OFFLINE_TOKEN instead to run offline)."
            )
        return Settings(
            mode="cloud",
            database_id=app_id,
            persistence_directory=persistence_directory,
            auth_url=auth_url,
            playground_token=token,
        )

    if not offline_token:
        raise SystemExit(
            "No credentials found. Set DITTO_APP_ID + DITTO_PLAYGROUND_TOKEN + "
            "DITTO_AUTH_URL for cloud sync, or DITTO_OFFLINE_TOKEN for offline "
            "peer-to-peer. See the README."
        )
    return Settings(
        mode="offline",
        # An explicit APP_ID lets offline peers share a database; otherwise fall
        # back to the SDK default so two local peers still mesh.
        database_id=app_id or DittoConfig.DEFAULT_DATABASE_ID,
        persistence_directory=persistence_directory,
        offline_token=offline_token,
    )


def build_config(settings: Settings) -> DittoConfig:
    connect = (
        DittoConfigConnect.server(settings.auth_url or "")
        if settings.mode == "cloud"
        else DittoConfigConnect.small_peers_only()
    )
    return DittoConfig(
        database_id=settings.database_id,
        connect=connect,
        persistence_directory=settings.persistence_directory,
    )


async def activate(peer: Ditto, settings: Settings) -> None:
    """Authenticate (cloud) or install the offline license before syncing."""

    if settings.mode == "offline":
        peer.set_offline_only_license_token(settings.offline_token or "")
        # Enable local peer-to-peer transports so peers on the same network mesh.
        peer.transport_config = DittoTransportConfig().enable_all_peer_to_peer()
        return

    authenticator = peer.auth
    if authenticator is None:
        raise SystemExit("Cloud configuration did not create an authenticator.")

    async def refresh(_peer: Ditto, _remaining: timedelta) -> None:
        await authenticator.login(
            settings.playground_token or "", AuthenticationProvider.DEVELOPMENT
        )

    authenticator.expiration_handler = refresh
    await authenticator.login(
        settings.playground_token or "", AuthenticationProvider.DEVELOPMENT
    )


class TasksApp:
    """Owns the Ditto subscription/observer and the tasks CRUD operations."""

    def __init__(self, peer: Ditto, settings: Settings) -> None:
        self.peer = peer
        self.settings = settings
        self.tasks: list[Task] = []
        self._subscription: Any | None = None
        self._observer: Any | None = None
        # The observer callback is delivered on this loop, so a plain Event is
        # enough to notice the first snapshot.
        self._updated = asyncio.Event()

    async def start(self) -> None:
        self._subscription = self.peer.sync.register_subscription(SUBSCRIPTION_QUERY)
        self._observer = self.peer.store.register_observer(TASKS_QUERY, self._on_change)
        self.peer.sync.start()

    def _on_change(self, result: QueryResult) -> None:
        try:
            self.tasks = [Task.from_value(item.value) for item in result]
        finally:
            result.close()
        self._updated.set()

    async def _mutate(self, coro: Any) -> None:
        """Run a write, then wait for the observer to reflect it.

        The observer callback lands on this event loop slightly after the
        write commits, so waiting keeps the next render in sync with the store.
        """

        self._updated.clear()
        await coro
        try:
            await asyncio.wait_for(self._updated.wait(), timeout=2.0)
        except asyncio.TimeoutError:
            pass

    async def add(self, title: str) -> None:
        task = {"_id": str(uuid.uuid4()), "title": title, "done": False, "deleted": False}
        with await self.peer.store.execute(
            "INSERT INTO tasks DOCUMENTS (:task)", {"task": task}
        ):
            pass

    async def toggle(self, task: Task) -> None:
        with await self.peer.store.execute(
            "UPDATE tasks SET done = :done WHERE _id = :id",
            {"done": not task.done, "id": task.id},
        ):
            pass

    async def edit(self, task: Task, title: str) -> None:
        with await self.peer.store.execute(
            "UPDATE tasks SET title = :title WHERE _id = :id",
            {"title": title, "id": task.id},
        ):
            pass

    async def delete(self, task: Task) -> None:
        with await self.peer.store.execute(
            "UPDATE tasks SET deleted = true WHERE _id = :id", {"id": task.id}
        ):
            pass

    async def stop(self) -> None:
        if self._observer is not None:
            self._observer.cancel()
            self._observer.close()
        if self._subscription is not None:
            self._subscription.cancel()
            self._subscription.close()

    # --- Console UI -------------------------------------------------------

    def render(self) -> None:
        print("\n" + "=" * 52)
        print(f"  Ditto Tasks  ({self.settings.mode} mode)")
        print("=" * 52)
        if not self.tasks:
            print("  (no tasks yet — try 'add Buy milk')")
        else:
            for index, task in enumerate(self.tasks, start=1):
                box = "[x]" if task.done else "[ ]"
                print(f"  {index:>2}. {box} {task.title}")
        print("-" * 52)
        print("  add <title>   done <n>   edit <n> <title>   del <n>")
        print("  list          help       quit")

    def _resolve(self, token: str) -> Task | None:
        try:
            index = int(token)
        except ValueError:
            print(f"  '{token}' is not a task number.")
            return None
        if 1 <= index <= len(self.tasks):
            return self.tasks[index - 1]
        print(f"  No task #{index}.")
        return None

    async def _read(self, prompt: str) -> str:
        loop = asyncio.get_running_loop()
        return await loop.run_in_executor(None, lambda: input(prompt))

    async def repl(self) -> None:
        print(
            "\nConnected. Changes from other peers appear when you refresh "
            "(press Enter or type 'list')."
        )
        while True:
            self.render()
            try:
                line = (await self._read("\n> ")).strip()
            except (EOFError, KeyboardInterrupt):
                print()
                return

            if not line:
                continue

            parts = line.split(maxsplit=1)
            command = parts[0].lower()
            rest = parts[1].strip() if len(parts) > 1 else ""

            if command in ("quit", "q", "exit"):
                return
            elif command in ("list", "l"):
                continue
            elif command in ("help", "h", "?"):
                self._print_help()
            elif command in ("add", "a"):
                if rest:
                    await self._mutate(self.add(rest))
                else:
                    print("  Usage: add <title>")
            elif command in ("done", "toggle", "t"):
                task = self._resolve(rest)
                if task is not None:
                    await self._mutate(self.toggle(task))
            elif command in ("edit", "e"):
                edit_parts = rest.split(maxsplit=1)
                if len(edit_parts) == 2:
                    task = self._resolve(edit_parts[0])
                    if task is not None:
                        await self._mutate(self.edit(task, edit_parts[1].strip()))
                else:
                    print("  Usage: edit <n> <new title>")
            elif command in ("del", "delete", "d"):
                task = self._resolve(rest)
                if task is not None:
                    await self._mutate(self.delete(task))
            else:
                print(f"  Unknown command: {command} (try 'help')")

    @staticmethod
    def _print_help() -> None:
        print(
            "\n  Commands:\n"
            "    add <title>         create a task\n"
            "    done <n>            toggle task #n complete/incomplete\n"
            "    edit <n> <title>    rename task #n\n"
            "    del <n>             delete task #n\n"
            "    list                refresh the list (also: press Enter)\n"
            "    quit                exit"
        )


async def run_tui(settings: Settings) -> int:
    DittoLogger.minimum_log_level = LogLevel.ERROR
    config = build_config(settings)
    async with Ditto.open(config) as peer:
        await activate(peer, settings)
        app = TasksApp(peer, settings)
        await app.start()
        try:
            # Give the first local snapshot a moment to arrive before drawing.
            await asyncio.wait_for(app._updated.wait(), timeout=5)
        except asyncio.TimeoutError:
            pass
        try:
            await app.repl()
        finally:
            await app.stop()
    return 0


async def run_smoke(settings: Settings) -> int:
    """Non-interactive check: open offline, insert, observe, print, exit 0."""

    DittoLogger.minimum_log_level = LogLevel.ERROR
    config = build_config(settings)
    async with Ditto.open(config) as peer:
        peer.set_offline_only_license_token(settings.offline_token or "")
        peer.sync.start()

        changes: asyncio.Queue[list[dict[str, Any]]] = asyncio.Queue()

        def on_change(result: QueryResult) -> None:
            try:
                changes.put_nowait([item.value for item in result])
            finally:
                result.close()

        subscription = peer.sync.register_subscription(SUBSCRIPTION_QUERY)
        observer = peer.store.register_observer(TASKS_QUERY, on_change)
        try:
            print("Initial tasks:", await asyncio.wait_for(changes.get(), timeout=5))
            task = {
                "_id": str(uuid.uuid4()),
                "title": "Buy milk",
                "done": False,
                "deleted": False,
            }
            with await peer.store.execute(
                "INSERT INTO tasks DOCUMENTS (:task)", {"task": task}
            ):
                pass
            observed = await asyncio.wait_for(changes.get(), timeout=5)
            print("After insert:", observed)
            assert any(t["title"] == "Buy milk" for t in observed), "inserted task not observed"
            print("SMOKE OK")
        finally:
            observer.cancel()
            observer.close()
            subscription.cancel()
            subscription.close()
    return 0


def cli(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Ditto Python Tasks quickstart.")
    parser.add_argument(
        "--smoke",
        action="store_true",
        help="Run a non-interactive offline insert/observe check and exit.",
    )
    args = parser.parse_args(argv)

    settings = load_settings(force_offline=args.smoke)
    persistence_directory = settings.persistence_directory
    try:
        if args.smoke:
            return asyncio.run(run_smoke(settings))
        return asyncio.run(run_tui(settings))
    finally:
        shutil.rmtree(persistence_directory, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(cli())
