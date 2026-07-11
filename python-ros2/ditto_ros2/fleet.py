"""Demo 3 — a fleet sharing a live world model through Ditto.

This is the demo that shows what Ditto adds *over* ROS 2 pub/sub. Each robot
publishes its ``Pose`` on ``/pose`` (its odometry). Instead of streaming those
poses as transient messages, each robot **upserts its own current pose into a
shared ``fleet`` collection, keyed by ``robot_id``**, and **observes the whole
collection**. The collection is a CRDT-merged, offline-durable shared world
model: it always holds exactly one current pose per robot, every robot sees
every other robot, and a robot that drops off and rejoins simply reconverges.

Contrast with DDS pub/sub: there is no retained "latest value per robot", no
persistence across disconnects, and no state once a publisher goes away. Here
the shared state *is* the collection.
"""

from __future__ import annotations

import asyncio
import contextlib
import math
import tempfile
import uuid
from typing import Any

from ditto import Ditto
from .peers import offline_token, open_peer
from .ros_compat import get_message_type, get_rclpy, use_sim

_BASE_PORT = 4121


class FleetMember:
    """One robot: publishes its pose on ``/pose`` and mirrors the shared fleet state."""

    def __init__(self, robot_id: str, peer: Ditto, node: Any, sim: bool) -> None:
        self.robot_id = robot_id
        self._peer = peer
        self._node = node
        self._sim = sim
        self._pose_type = get_message_type("Pose", sim)
        self._pose_pub = node.create_publisher(self._pose_type, "/pose", 10)
        self._subscription: Any = None
        self._observer: Any = None
        self._spin_task: asyncio.Task[None] | None = None
        self._upserts: set[asyncio.Task[None]] = set()
        # The shared world as this robot currently sees it: robot_id -> (x, y, theta).
        self.world: dict[str, tuple[float, float, float]] = {}

    async def start(self) -> None:
        self._subscription = self._peer.sync.register_subscription("SELECT * FROM fleet")
        # Ingest this robot's own /pose topic into the shared collection.
        self._node.create_subscription(self._pose_type, "/pose", self._on_pose, 10)
        # Observe the whole fleet — every robot's latest pose, synced from peers.
        self._observer = self._peer.store.register_observer("SELECT * FROM fleet", self._on_fleet)
        self._spin_task = asyncio.create_task(self._spin())
        self._peer.sync.start()

    def publish_pose(self, x: float, y: float, theta: float) -> None:
        pose = self._pose_type()
        pose.x, pose.y, pose.theta = round(x, 3), round(y, 3), round(theta, 3)
        self._pose_pub.publish(pose)

    def _on_pose(self, message: Any) -> None:
        # Runs on the loop (via the spin task). Upsert our current pose, keyed by
        # robot_id so the collection keeps one live row per robot, not a stream.
        task = asyncio.create_task(
            self._upsert(float(message.x), float(message.y), float(message.theta))
        )
        self._upserts.add(task)
        task.add_done_callback(self._upserts.discard)

    async def _upsert(self, x: float, y: float, theta: float) -> None:
        result = await self._peer.store.execute(
            "INSERT INTO fleet DOCUMENTS (:doc) ON ID CONFLICT DO UPDATE",
            {"doc": {"_id": self.robot_id, "robot": self.robot_id, "x": x, "y": y, "theta": theta}},
        )
        result.close()

    def _on_fleet(self, result: Any) -> None:
        try:
            rows = [dict(item.value) for item in result]
        finally:
            result.close()
        self.world = {
            str(row["robot"]): (float(row["x"]), float(row["y"]), float(row["theta"]))
            for row in rows
            if "robot" in row
        }

    async def _spin(self) -> None:
        rclpy = get_rclpy(self._sim)
        while True:
            rclpy.spin_once(self._node, timeout_sec=0.0)
            await asyncio.sleep(0.005)

    async def stop(self) -> None:
        if self._spin_task is not None:
            self._spin_task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self._spin_task
        for task in list(self._upserts):
            task.cancel()
        if self._observer is not None:
            self._observer.cancel()
            self._observer.close()
        if self._subscription is not None:
            self._subscription.cancel()
            self._subscription.close()


async def run(
    *,
    robots: int = 3,
    count: int = 6,
    interval: float = 0.4,
    sim: bool | None = None,
    token: str | None = None,
) -> dict[str, dict[str, tuple[float, float, float]]]:
    """Drive ``robots`` robots for ``count`` ticks; return each robot's view of the fleet."""
    resolved_sim = use_sim(sim)
    rclpy = get_rclpy(resolved_sim)
    database_id = str(uuid.uuid4())
    token = token or offline_token()
    rclpy.init()

    members: list[FleetMember] = []
    async with contextlib.AsyncExitStack() as stack:
        for index in range(robots):
            robot_id = f"robot-{index + 1}"
            directory = stack.enter_context(tempfile.TemporaryDirectory(prefix=f"ditto-{robot_id}-"))
            # Star topology: robot-1 is the hub; everyone else connects to it.
            peer = await stack.enter_async_context(
                open_peer(
                    database_id=database_id,
                    directory=directory,
                    token=token,
                    peer_port=_BASE_PORT + index,
                    connect_ports=[] if index == 0 else [_BASE_PORT],
                )
            )
            members.append(FleetMember(robot_id, peer, rclpy.create_node(robot_id), resolved_sim))

        try:
            for member in members:
                await member.start()
            for tick in range(count):
                for index, member in enumerate(members):
                    # Each robot follows its own little trajectory.
                    member.publish_pose(x=tick * 0.1 + index, y=float(index), theta=tick * 0.2)
                await asyncio.sleep(interval)
                hub = members[0]
                print(
                    f"[fleet] tick {tick}: robot-1 sees "
                    + ", ".join(f"{r}={xy[0]:.2f},{xy[1]:.2f}" for r, xy in sorted(hub.world.items())),
                    flush=True,
                )
            # Drain until every robot sees the whole fleet (or we time out).
            deadline = count * interval + 4.0
            while deadline > 0 and not all(len(m.world) == robots for m in members):
                await asyncio.sleep(0.05)
                deadline -= 0.05
            views = {m.robot_id: dict(m.world) for m in members}
        finally:
            for member in members:
                await member.stop()
            for member in members:
                member._node.destroy_node()
    rclpy.shutdown()
    seen = min((len(v) for v in views.values()), default=0)
    print(f"[fleet] every robot now shares one world model of {seen}/{robots} robots", flush=True)
    return views
