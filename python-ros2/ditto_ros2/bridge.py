"""The Ditto ↔ ROS 2 topic bridge.

Each :class:`Route` maps a ROS topic to a Ditto collection in one direction:

* ``ros_to_ditto`` — subscribe to the topic and INSERT every message into the
  collection (keyed by ``robot_id`` + a monotonic sequence). Ditto then syncs it
  to every other peer in the mesh — over Wi-Fi/BLE/LAN, with no cloud required.
* ``ditto_to_ros`` — observe the collection and publish newly-arrived documents
  onto the topic.

Run the bridge on several robots and a topic published on one appears on the
others, offline, with CRDT conflict resolution — ROS 2 pub/sub carried across
separate ROS graphs by Ditto. (For *shared current state* keyed per robot rather
than a stream of messages, see ``fleet.py``.)
"""

from __future__ import annotations

import asyncio
import itertools
from dataclasses import dataclass
from typing import Any

from ditto import Ditto

from .ros_compat import get_message_type, get_rclpy

# Bound the per-collection "already published to ROS" cache so a long-running
# bridge doesn't grow memory without limit. Message ids are monotonic, so
# evicting the oldest entry is safe (a re-published stale command is harmless).
_PUBLISHED_CACHE_LIMIT = 4096


@dataclass(frozen=True)
class Route:
    collection: str
    topic: str
    message: str  # short message-type name, e.g. "Twist"
    direction: str  # "ros_to_ditto" | "ditto_to_ros"


def serialize(message: Any) -> dict[str, Any]:
    """Convert a ROS message (real or shim) to a plain dict for Ditto."""

    to_dict = getattr(message, "to_dict", None)
    if callable(to_dict):
        result: dict[str, Any] = to_dict()
        return result
    from rosidl_runtime_py.convert import message_to_ordereddict

    return dict(message_to_ordereddict(message))


def deserialize(message_type: type, value: dict[str, Any]) -> Any:
    """Rebuild a ROS message (real or shim) from a Ditto document payload."""

    from_dict = getattr(message_type, "from_dict", None)
    if callable(from_dict):
        return from_dict(value)
    from rosidl_runtime_py.set_message import set_message_fields

    message = message_type()
    set_message_fields(message, value)
    return message


class DittoRos2Bridge:
    def __init__(
        self,
        ditto: Ditto,
        node: Any,
        routes: list[Route],
        *,
        robot_id: str,
        sim: bool | None = None,
    ) -> None:
        self._ditto = ditto
        self._node = node
        self._routes = routes
        self._robot_id = robot_id
        self._sim = sim
        self._seq = itertools.count()
        self._subscriptions: list[Any] = []
        self._ros_subscriptions: list[Any] = []
        self._observers: list[Any] = []
        self._pumps: list[asyncio.Task[None]] = []
        self._spin_task: asyncio.Task[None] | None = None
        # Last payload published to ROS per document id, so the observer only
        # re-publishes documents that are new or whose value actually changed.
        self._published: dict[str, Any] = {}

    async def start(self) -> None:
        loop = asyncio.get_running_loop()
        for route in self._routes:
            self._subscriptions.append(
                self._ditto.sync.register_subscription(f"SELECT * FROM {route.collection}")
            )
            if route.direction == "ros_to_ditto":
                self._wire_ros_to_ditto(route, loop)
            elif route.direction == "ditto_to_ros":
                self._wire_ditto_to_ros(route)
            else:
                raise ValueError(f"Unknown route direction: {route.direction}")
        # Spin the ROS node so inbound messages reach subscriptions and outbound
        # publishes reach local subscribers, the way rclpy.spin() would.
        self._spin_task = asyncio.create_task(self._spin_ros())
        self._ditto.sync.start()

    def _wire_ros_to_ditto(self, route: Route, loop: asyncio.AbstractEventLoop) -> None:
        queue: asyncio.Queue[Any] = asyncio.Queue()

        def on_ros_message(message: Any) -> None:
            # Called synchronously from rclpy on publish; hand off to the loop.
            loop.call_soon_threadsafe(queue.put_nowait, message)

        self._ros_subscriptions.append(
            self._node.create_subscription(
                get_message_type(route.message, self._sim), route.topic, on_ros_message, 10
            )
        )
        self._pumps.append(asyncio.create_task(self._pump_to_ditto(route, queue)))

    async def _pump_to_ditto(self, route: Route, queue: asyncio.Queue[Any]) -> None:
        while True:
            message = await queue.get()
            payload = serialize(message)
            document = {
                "_id": f"{self._robot_id}:{next(self._seq)}",
                "robot": self._robot_id,
                "topic": route.topic,
                "payload": payload,
            }
            result = await self._ditto.store.execute(
                f"INSERT INTO {route.collection} DOCUMENTS (:doc) ON ID CONFLICT DO UPDATE",
                {"doc": document},
            )
            result.close()

    def _wire_ditto_to_ros(self, route: Route) -> None:
        message_type = get_message_type(route.message, self._sim)
        publisher = self._node.create_publisher(message_type, route.topic, 10)

        def on_change(result: Any) -> None:
            try:
                rows = [dict(item.value) for item in result]
            finally:
                result.close()
            for row in rows:
                document_id = str(row.get("_id"))
                payload = row.get("payload")
                if not isinstance(payload, dict):
                    continue
                if self._published.get(document_id) == payload:
                    continue
                self._published[document_id] = payload
                if len(self._published) > _PUBLISHED_CACHE_LIMIT:
                    del self._published[next(iter(self._published))]
                publisher.publish(deserialize(message_type, payload))

        self._observers.append(
            self._ditto.store.register_observer(f"SELECT * FROM {route.collection}", on_change)
        )

    async def _spin_ros(self) -> None:
        rclpy = get_rclpy(self._sim)
        while True:
            rclpy.spin_once(self._node, timeout_sec=0.0)
            await asyncio.sleep(0.005)

    async def stop(self) -> None:
        if self._spin_task is not None:
            self._spin_task.cancel()
            try:
                await self._spin_task
            except asyncio.CancelledError:
                pass
            self._spin_task = None
        for pump in self._pumps:
            pump.cancel()
        for pump in self._pumps:
            try:
                await pump
            except asyncio.CancelledError:
                pass
        self._pumps.clear()
        for observer in self._observers:
            observer.cancel()
            observer.close()
        for ros_subscription in self._ros_subscriptions:
            self._node.destroy_subscription(ros_subscription)
        for subscription in self._subscriptions:
            subscription.cancel()
            subscription.close()
        self._observers.clear()
        self._ros_subscriptions.clear()
        self._subscriptions.clear()
