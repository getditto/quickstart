"""The one place that chooses between real ROS 2 (``rclpy``) and the shim.

Everything else imports ``rclpy`` and message types from here, so the demos are
identical in simulation and on a robot. Use ``--sim`` (or set ``DITTO_ROS_SIM=1``)
to force the shim even when a real ROS 2 is importable.
"""

from __future__ import annotations

import os
from typing import Any, cast

from . import messages as sim_messages


def use_sim(explicit: bool | None = None) -> bool:
    if explicit is not None:
        return explicit
    if os.environ.get("DITTO_ROS_SIM", "").strip() in ("1", "true", "yes", "on"):
        return True
    try:
        import rclpy  # noqa: F401
    except ImportError:
        return True
    return False


def get_rclpy(sim: bool | None = None) -> Any:
    if use_sim(sim):
        from . import rclpy_shim

        return rclpy_shim
    import rclpy

    return rclpy


def get_message_type(name: str, sim: bool | None = None) -> type:
    """Resolve a message type by short name (``"String"``, ``"Twist"``, ``"Pose"``)."""

    if use_sim(sim):
        return sim_messages.MESSAGE_TYPES[name]
    # Real ROS 2: map short names to their standard packages. "Pose" maps to
    # geometry_msgs/Pose2D, whose (x, y, theta) fields match our simplified Pose.
    if name == "String":
        import std_msgs.msg as std

        return cast(type, std.String)
    import geometry_msgs.msg as geometry

    return cast(type, getattr(geometry, "Pose2D" if name == "Pose" else name))
