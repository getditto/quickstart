#!/usr/bin/env python3
"""Ditto Python quickstart — a Tasks console app that syncs peer-to-peer.

This mirrors the other quickstart apps (go-tui, rust-tui, javascript-tui): it
manages a shared "tasks" collection using the canonical cross-SDK schema, so it
interoperates with every other quickstart app.

    { "_id": str, "title": str, "created_at": int, "done": bool, "deleted": bool }

Configuration comes from the environment (or a .env file in this directory):

    DITTO_DATABASE_ID       Database ID from the Ditto Portal
    DITTO_DEVELOPMENT_TOKEN Development token from the Ditto Portal
    DITTO_SERVER_URL        Server URL from the Ditto Portal

Run with --smoke to exercise the full CRUD path against a temporary local
peer, with no credentials and no network.
"""

from __future__ import annotations

import argparse
import asyncio
from contextlib import contextmanager
import os
import sys
import tempfile
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from time import time_ns
from typing import Any

from ditto import (
    AuthenticationProvider,
    Ditto,
    DittoConfig,
    DittoConfigConnect,
    DittoLogger,
    LogLevel,
    QueryResult,
)

# The observer view hides soft-deleted tasks; the subscription syncs everything
# so that tombstones propagate to other peers.
TASKS_QUERY = "SELECT * FROM tasks WHERE deleted = false ORDER BY created_at"
SUBSCRIPTION_QUERY = "SELECT * FROM tasks"
TASK_COLUMN_WIDTH = 50
CREATED_AT_COLUMN_WIDTH = 19
TABLE_WIDTH = TASK_COLUMN_WIDTH + CREATED_AT_COLUMN_WIDTH + 3


@dataclass
class Task:
    id: str
    title: str
    created_at: int | None
    done: bool

    @classmethod
    def from_value(cls, value: dict[str, Any]) -> "Task":
        return cls(
            id=str(value["_id"]),
            title=str(value.get("title", "")),
            created_at=(
                int(value["created_at"])
                if value.get("created_at") is not None
                else None
            ),
            done=bool(value.get("done", False)),
        )


@dataclass
class Settings:
    database_id: str
    persistence_directory: str
    server_url: str | None = None
    development_token: str | None = None

    @property
    def offline(self) -> bool:
        return self.server_url is None


def load_dotenv(path: Path) -> None:
    """Load KEY=VALUE pairs from a .env file into os.environ.

    Written out rather than pulled from python-dotenv so that the quickstart's
    only dependency is the Ditto SDK itself. Existing environment variables
    always win, matching python-dotenv's default behavior.
    """

    if not path.is_file():
        return
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[len("export ") :].lstrip()
        key, sep, value = line.partition("=")
        if not sep:
            continue
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key and key not in os.environ:
            os.environ[key] = value


def load_settings(*, offline: bool) -> Settings:
    """Read configuration from the environment (and a local .env file)."""

    # Prefer this directory's .env, then fall back to the one at the repo root
    # that the other quickstarts share.
    here = Path(__file__).resolve().parent
    load_dotenv(here / ".env")
    load_dotenv(here.parent / ".env")

    # Use a temp directory for persistence, matching go-tui and rust-tui. Data
    # is not persistent between runs, but it lets multiple instances run
    # concurrently on the same machine — which is how the sync demo works.
    persistence_directory = tempfile.mkdtemp(prefix="ditto-python-tui-")

    if offline:
        # No credentials required: a local-only peer with the SDK's default
        # database ID is enough to exercise the store.
        return Settings(
            database_id=DittoConfig.DEFAULT_DATABASE_ID,
            persistence_directory=persistence_directory,
        )

    database_id = os.environ.get("DITTO_DATABASE_ID", "").strip()
    token = os.environ.get("DITTO_DEVELOPMENT_TOKEN", "").strip()
    server_url = os.environ.get("DITTO_SERVER_URL", "").strip()

    missing = [
        name
        for name, value in (
            ("DITTO_DATABASE_ID", database_id),
            ("DITTO_DEVELOPMENT_TOKEN", token),
            ("DITTO_SERVER_URL", server_url),
        )
        if not value
    ]
    if missing:
        raise SystemExit(
            "Missing required environment variables: "
            + ", ".join(missing)
            + "\nCopy .env.sample to .env and fill in the values from "
            "https://portal.ditto.live (or run with --smoke to try the app "
            "locally without credentials)."
        )

    return Settings(
        database_id=database_id,
        persistence_directory=persistence_directory,
        server_url=server_url,
        development_token=token,
    )


def build_config(settings: Settings) -> DittoConfig:
    connect = (
        DittoConfigConnect.small_peers_only()
        if settings.offline
        else DittoConfigConnect.server(settings.server_url or "")
    )
    return DittoConfig(
        database_id=settings.database_id,
        connect=connect,
        persistence_directory=settings.persistence_directory,
    )


@contextmanager
def suppress_native_logger_startup() -> Any:
    """Silence preview-runtime logs emitted before DittoLogger is configured.

    Stderr is restored as soon as configuration finishes.
    """

    stderr_fd = sys.stderr.fileno()
    saved_stderr_fd = os.dup(stderr_fd)
    try:
        with open(os.devnull, "w", encoding="utf-8") as null:
            os.dup2(null.fileno(), stderr_fd)
            yield
    finally:
        os.dup2(saved_stderr_fd, stderr_fd)
        os.close(saved_stderr_fd)


async def authenticate(peer: Ditto, settings: Settings) -> None:
    """Log in and keep the session refreshed.

    Server connections require an expiration handler: the SDK raises
    DittoExpirationHandlerMissingError if sync starts without one.
    """

    if settings.offline:
        return

    authenticator = peer.auth
    if authenticator is None:
        raise SystemExit("Server configuration did not create an authenticator.")

    token = settings.development_token or ""

    async def refresh(_peer: Ditto, _time_until_expiration: timedelta) -> None:
        await authenticator.login(token, AuthenticationProvider.DEVELOPMENT)

    authenticator.expiration_handler = refresh
    await authenticator.login(token, AuthenticationProvider.DEVELOPMENT)


class TasksApp:
    """Owns the Ditto subscription and observer, and the tasks CRUD."""

    def __init__(self, peer: Ditto, settings: Settings) -> None:
        self.peer = peer
        self.settings = settings
        self.tasks: list[Task] = []
        self._subscription: Any | None = None
        self._observer: Any | None = None
        # The observer callback is delivered on this event loop, so a plain
        # Event is enough to notice that a snapshot has arrived.
        self._updated = asyncio.Event()

    async def start(self) -> None:
        # Subscriptions request data from other peers; observers react to
        # changes in the local store.
        self._subscription = self.peer.sync.register_subscription(SUBSCRIPTION_QUERY)
        self._observer = self.peer.store.register_observer(TASKS_QUERY, self._on_change)
        # The observer first reports the local store, which may be empty.
        await self._wait_for_update(timeout=10.0)
        if not self.settings.offline:
            # sync.start() requires an activated instance: without a license
            # token the SDK raises DittoError <activation>. The local store,
            # subscriptions, and observers all work regardless — only
            # replication with other peers needs activation.
            self._updated.clear()
            self.peer.sync.start()
            # Wait for the first synced change before rendering the task list.
            await self._wait_for_update(timeout=10.0)

    def _on_change(self, result: QueryResult) -> None:
        # The QueryResult owns native resources; close it when done.
        try:
            self.tasks = [Task.from_value(item.value) for item in result.items]
        finally:
            result.close()
        self._updated.set()

    async def _wait_for_update(self, timeout: float) -> bool:
        try:
            await asyncio.wait_for(self._updated.wait(), timeout=timeout)
            return True
        except asyncio.TimeoutError:
            return False

    async def _mutate(self, coro: Any) -> None:
        """Run a write, then wait for the observer to reflect it."""

        self._updated.clear()
        await coro
        await self._wait_for_update(timeout=2.0)

    async def add(self, title: str) -> None:
        task = {
            "_id": str(uuid.uuid4()),
            "title": title,
            "created_at": time_ns(),
            "done": False,
            "deleted": False,
        }
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
        # Tasks are soft-deleted so the removal syncs to other peers.
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

    @staticmethod
    def _format_created_at(task: Task) -> str:
        if task.created_at is None:
            return "—"
        return datetime.fromtimestamp(task.created_at / 1_000_000_000).strftime(
            "%Y-%m-%d %H:%M:%S"
        )

    def render(self, show_help: bool = False) -> None:
        mode = "offline" if self.settings.offline else "syncing"
        print("\n" + "=" * TABLE_WIDTH)
        print(f"  Ditto Tasks  ({mode})")
        print("=" * TABLE_WIDTH)
        if not self.tasks:
            print("  (no tasks yet — try 'add Buy milk')")
        else:
            print(f"  {'Task':<{TASK_COLUMN_WIDTH}} {'Created At':<{CREATED_AT_COLUMN_WIDTH}}")
            print(f"  {'-' * TASK_COLUMN_WIDTH} {'-' * CREATED_AT_COLUMN_WIDTH}")
            for index, task in enumerate(self.tasks, start=1):
                box = "[x]" if task.done else "[ ]"
                label = f"{index}. {box} {task.title}"
                if len(label) > TASK_COLUMN_WIDTH:
                    label = f"{label[:TASK_COLUMN_WIDTH - 3]}..."
                created_at = self._format_created_at(task)
                print(
                    f"  {label:<{TASK_COLUMN_WIDTH}} "
                    f"{created_at:<{CREATED_AT_COLUMN_WIDTH}}"
                )
        print("-" * TABLE_WIDTH)
        print("  add <title>   toggle <n>   edit <n> <title>   del <n>")
        print("  list          help       quit")
        if show_help:
            self._print_help()

    def _print_help(self) -> None:
        print(
            "\n  add <title>        Create a task\n"
            "  toggle <n>         Toggle a task's completed state\n"
            "  edit <n> <title>   Rename a task\n"
            "  del <n>            Delete a task\n"
            "  list               Refresh the list\n"
            "  quit               Exit"
        )

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
        # input() blocks, so run it off the event loop to keep observer
        # callbacks flowing while we wait for the user.
        loop = asyncio.get_running_loop()
        return await loop.run_in_executor(None, lambda: input(prompt))

    async def repl(self) -> None:
        print(
            "\n" + "=" * TABLE_WIDTH + "\n\n"
            "Ready. Changes from other peers appear when you refresh "
            "(press Enter or type 'list')."
        )
        show_help = False
        while True:
            self.render(show_help=show_help)
            show_help = False
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
            if command in ("list", "l"):
                continue
            if command in ("help", "h", "?"):
                show_help = True
            elif command in ("add", "a"):
                if rest:
                    await self._mutate(self.add(rest))
                else:
                    print("  Usage: add <title>")
            elif command in ("toggle", "t"):
                task = self._resolve(rest)
                if task is not None:
                    await self._mutate(self.toggle(task))
            elif command in ("edit", "e"):
                edit_parts = rest.split(maxsplit=1)
                if len(edit_parts) == 2:
                    task = self._resolve(edit_parts[0])
                    if task is not None:
                        await self._mutate(self.edit(task, edit_parts[1]))
                else:
                    print("  Usage: edit <n> <title>")
            elif command in ("del", "delete", "rm", "d"):
                task = self._resolve(rest)
                if task is not None:
                    await self._mutate(self.delete(task))
            else:
                print(f"  Unknown command '{command}'. Type 'help'.")


async def smoke(app: TasksApp) -> None:
    """Exercise the full CRUD path without a terminal or credentials."""

    def titles() -> list[str]:
        return [t.title for t in app.tasks]

    assert titles() == [], f"expected an empty store, got {titles()}"
    print("  ✓ opened, subscribed, observer fired with an empty result")

    await app._mutate(app.add("Buy milk"))
    assert titles() == ["Buy milk"], titles()
    print("  ✓ insert observed:", titles())

    await app._mutate(app.add("Walk the dog"))
    assert titles() == ["Buy milk", "Walk the dog"], titles()
    print("  ✓ second insert, creation order applied:", titles())

    task = app.tasks[0]
    await app._mutate(app.toggle(task))
    assert app.tasks[0].done is True, app.tasks[0]
    print("  ✓ update observed: 'Buy milk' done =", app.tasks[0].done)

    await app._mutate(app.edit(app.tasks[0], "Buy oat milk"))
    assert "Buy oat milk" in titles(), titles()
    print("  ✓ rename observed:", titles())

    await app._mutate(app.delete(app.tasks[0]))
    assert titles() == ["Walk the dog"], titles()
    print("  ✓ soft-delete observed:", titles())

    print("\n  All checks passed.")


async def run(settings: Settings, *, smoke_test: bool) -> None:
    config = build_config(settings)

    # Ditto.open() is awaitable and an async context manager; `async with`
    # guarantees the peer is closed on the way out.
    async with Ditto.open(config) as peer:
        await authenticate(peer, settings)
        app = TasksApp(peer, settings)
        await app.start()
        try:
            if smoke_test:
                await smoke(app)
            else:
                await app.repl()
        finally:
            await app.stop()


def cli() -> None:
    parser = argparse.ArgumentParser(description="Ditto Python quickstart — Tasks")
    parser.add_argument(
        "--smoke",
        action="store_true",
        help="run a local, credential-free CRUD self-test and exit",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="show Ditto SDK logs (suppressed by default)",
    )
    args = parser.parse_args()

    if args.verbose:
        # Keep startup diagnostics visible while troubleshooting.
        DittoLogger.minimum_log_level = LogLevel.INFO
    else:
        # The preview runtime logs before this public setting takes effect.
        with suppress_native_logger_startup():
            DittoLogger.minimum_log_level = LogLevel.ERROR

    settings = load_settings(offline=args.smoke)
    try:
        asyncio.run(run(settings, smoke_test=args.smoke))
    except KeyboardInterrupt:
        pass

    # NOTE: deliberately not removing the persistence directory here. Ditto
    # keeps writing to its rotating log directory — rooted at the persistence
    # directory of the most recently opened instance — after the instance is
    # closed, and that directory is not safe to delete until the process
    # exits. It lives under the system temp directory, so the OS reclaims it.


if __name__ == "__main__":
    sys.exit(cli())
