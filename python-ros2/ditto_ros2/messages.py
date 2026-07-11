"""Minimal message types mirroring the real ROS 2 messages the demos use.

These mirror the field layout of ``std_msgs/msg/String``,
``geometry_msgs/msg/Twist``, and ``geometry_msgs/msg/Pose`` so the demo code is
identical whether it runs against the ``rclpy_shim`` (no ROS install) or a real
ROS 2 stack. Each type provides plain-dict (de)serialization so messages
round-trip cleanly through Ditto (CBOR).
"""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any


@dataclass
class Vector3:
    """geometry_msgs/msg/Vector3 — an x/y/z triple."""

    x: float = 0.0
    y: float = 0.0
    z: float = 0.0


@dataclass
class Twist:
    """geometry_msgs/msg/Twist — linear + angular velocity."""

    linear: Vector3 = field(default_factory=Vector3)
    angular: Vector3 = field(default_factory=Vector3)

    def to_dict(self) -> dict[str, Any]:
        return {"linear": asdict(self.linear), "angular": asdict(self.angular)}

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> Twist:
        linear = value.get("linear") or {}
        angular = value.get("angular") or {}
        return cls(
            linear=Vector3(**{k: float(v) for k, v in linear.items() if k in ("x", "y", "z")}),
            angular=Vector3(**{k: float(v) for k, v in angular.items() if k in ("x", "y", "z")}),
        )


@dataclass
class String:
    """std_msgs/msg/String."""

    data: str = ""

    def to_dict(self) -> dict[str, Any]:
        return {"data": self.data}

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> String:
        return cls(data=str(value.get("data", "")))


@dataclass
class Pose:
    """A pose on the plane: position ``(x, y)`` and heading ``theta`` (radians).

    A deliberately small stand-in for ``geometry_msgs/msg/Pose`` so the fleet
    demo stays readable; the real message nests position/orientation the same
    way and round-trips through the same ``to_dict``/``from_dict`` contract.
    """

    x: float = 0.0
    y: float = 0.0
    theta: float = 0.0

    def to_dict(self) -> dict[str, Any]:
        return {"x": self.x, "y": self.y, "theta": self.theta}

    @classmethod
    def from_dict(cls, value: dict[str, Any]) -> Pose:
        return cls(
            x=float(value.get("x", 0.0)),
            y=float(value.get("y", 0.0)),
            theta=float(value.get("theta", 0.0)),
        )


# Registry so a route/demo can name its message type as a string (as ROS does).
MESSAGE_TYPES: dict[str, type] = {"Twist": Twist, "String": String, "Pose": Pose}
