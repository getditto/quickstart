"""A tiny in-process stand-in for ``rclpy`` so the demos run and are tested
without a ROS 2 installation.

It implements just the surface the demos use -- ``init`` / ``shutdown`` / ``ok``
/ ``create_node`` / ``spin_once`` and a ``Node`` with ``create_publisher`` /
``create_subscription`` -- with faithful names and signatures.

Two design choices make the demos meaningful:

* **Delivery happens on spin, not on publish.** ``publish()`` only enqueues;
  ``spin_once(node)`` dispatches that node's queued messages to its callbacks,
  exactly as a real node must be spun to receive.
* **Delivery is per node.** A publisher reaches only its own node's
  subscribers, the way two robots on separate DDS domains stay isolated until
  something bridges them. Here that bridge is Ditto -- so the only path from one
  robot's topic to another robot's subscriber is through Ditto sync, which is
  the whole point of these demos.

This is a test/dev double, not a ROS 2 implementation: no networking, no real
executor, no DDS QoS matching.
"""

from __future__ import annotations

import logging
from collections import deque
from collections.abc import Callable
from typing import Any

_log = logging.getLogger("ditto_ros2.rclpy_shim")

_initialized = False


def init(args: list[str] | None = None, *, context: Any = None) -> None:
    global _initialized
    _initialized = True


def shutdown(*, context: Any = None) -> None:
    global _initialized
    _initialized = False


def ok(context: Any = None) -> bool:
    return _initialized


class Publisher:
    def __init__(self, node: Node, msg_type: type[Any], topic: str, qos: Any) -> None:
        self._node = node
        self.msg_type = msg_type
        self.topic = topic
        self.qos = qos

    def publish(self, message: Any) -> None:
        if not isinstance(message, self.msg_type):
            raise TypeError(
                f"Publisher on {self.topic!r} expected {self.msg_type.__name__}, "
                f"got {type(message).__name__}"
            )
        # Enqueue for delivery on the next spin_once of this node. Only this
        # node's own subscribers see it (per-node isolation, see module docstring).
        for subscription in list(self._node._subscriptions.get(self.topic, ())):
            self._node._inbox.append((subscription.callback, message))


class Subscription:
    def __init__(self, topic: str, callback: Callable[[Any], None]) -> None:
        self.topic = topic
        self.callback = callback


class Node:
    def __init__(self, name: str) -> None:
        self.name = name
        self._subscriptions: dict[str, list[Subscription]] = {}
        self._publishers: list[Publisher] = []
        self._inbox: deque[tuple[Callable[[Any], None], Any]] = deque()

    def get_name(self) -> str:
        return self.name

    def create_publisher(self, msg_type: type[Any], topic: str, qos: Any = 10) -> Publisher:
        publisher = Publisher(self, msg_type, topic, qos)
        self._publishers.append(publisher)
        return publisher

    def create_subscription(
        self,
        msg_type: type[Any],
        topic: str,
        callback: Callable[[Any], None],
        qos: Any = 10,
    ) -> Subscription:
        subscription = Subscription(topic, callback)
        self._subscriptions.setdefault(topic, []).append(subscription)
        return subscription

    def destroy_subscription(self, subscription: Subscription) -> None:
        peers = self._subscriptions.get(subscription.topic)
        if peers:
            self._subscriptions[subscription.topic] = [s for s in peers if s is not subscription]

    def destroy_node(self) -> None:
        self._subscriptions.clear()
        self._publishers.clear()
        self._inbox.clear()


def create_node(name: str) -> Node:
    return Node(name)


def spin_once(node: Node, *, executor: Any = None, timeout_sec: float | None = None) -> None:
    """Dispatch the messages queued for ``node`` at entry to their callbacks.

    Only the batch present when the call begins is delivered, so a callback that
    publishes back to its own node cannot spin forever within a single call.
    """

    for _ in range(len(node._inbox)):
        callback, message = node._inbox.popleft()
        try:
            callback(message)
        except Exception:
            _log.exception("Unhandled exception in a ROS subscription callback")


__all__ = [
    "Node",
    "Publisher",
    "Subscription",
    "create_node",
    "init",
    "ok",
    "shutdown",
    "spin_once",
]
