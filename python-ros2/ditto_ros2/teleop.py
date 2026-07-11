"""Demo 2 — teleop: drive a robot from a separate control station over Ditto.

A control station publishes ``geometry_msgs/Twist`` velocity commands on
``/cmd_vel``. The robot subscribes to ``/cmd_vel`` — but it's a separate peer on
the mesh, so the commands reach it only through Ditto. Bridge the control
station's ``/cmd_vel`` into Ditto, sync, and republish onto the robot's
``/cmd_vel``; the robot acts on each command as it arrives.

Takeaway: command-and-control survives an intermittent link — commands queue in
Ditto and converge when the robot reconnects, with no central broker.
"""

from __future__ import annotations

import asyncio
import contextlib
import math
import tempfile
import uuid

from .bridge import DittoRos2Bridge, Route
from .peers import offline_token, open_peer
from .ros_compat import get_message_type, get_rclpy, use_sim


async def run(
    *, count: int = 6, interval: float = 0.4, sim: bool | None = None, token: str | None = None
) -> list[tuple[float, float]]:
    """Send ``count`` velocity commands; return the (linear.x, angular.z) the robot acted on."""
    resolved_sim = use_sim(sim)
    rclpy = get_rclpy(resolved_sim)
    twist_type = get_message_type("Twist", resolved_sim)
    database_id = str(uuid.uuid4())
    token = token or offline_token()
    driven: list[tuple[float, float]] = []

    rclpy.init()
    async with contextlib.AsyncExitStack() as stack:
        with tempfile.TemporaryDirectory(prefix="ditto-ros2-control-") as dir_c, tempfile.TemporaryDirectory(
            prefix="ditto-ros2-robot-"
        ) as dir_r:
            control_peer = await stack.enter_async_context(
                open_peer(database_id=database_id, directory=dir_c, token=token, peer_port=4111)
            )
            robot_peer = await stack.enter_async_context(
                open_peer(
                    database_id=database_id,
                    directory=dir_r,
                    token=token,
                    peer_port=4112,
                    connect_ports=[4111],
                )
            )

            control_node = rclpy.create_node("control_station")
            robot_node = rclpy.create_node("robot")

            def on_cmd_vel(message: object) -> None:
                linear = getattr(message, "linear", None)
                angular = getattr(message, "angular", None)
                lx = round(float(getattr(linear, "x", 0.0)), 3)
                az = round(float(getattr(angular, "z", 0.0)), 3)
                driven.append((lx, az))
                print(f"[robot] driving: linear.x={lx} angular.z={az}", flush=True)

            robot_node.create_subscription(twist_type, "/cmd_vel", on_cmd_vel, 10)
            cmd_vel = control_node.create_publisher(twist_type, "/cmd_vel", 10)

            control_bridge = DittoRos2Bridge(
                control_peer,
                control_node,
                [Route("cmd_vel", "/cmd_vel", "Twist", "ros_to_ditto")],
                robot_id="control",
                sim=resolved_sim,
            )
            robot_bridge = DittoRos2Bridge(
                robot_peer,
                robot_node,
                [Route("cmd_vel", "/cmd_vel", "Twist", "ditto_to_ros")],
                robot_id="robot",
                sim=resolved_sim,
            )
            try:
                await control_bridge.start()
                await robot_bridge.start()
                for index in range(count):
                    twist = twist_type()
                    twist.linear.x = round(0.5 + 0.5 * math.sin(index * interval), 3)
                    twist.angular.z = round(0.3 * math.cos(index * interval), 3)
                    cmd_vel.publish(twist)
                    print(
                        f"[control] send: linear.x={twist.linear.x} angular.z={twist.angular.z}",
                        flush=True,
                    )
                    await asyncio.sleep(interval)
                deadline = count * interval + 3.0
                while len(driven) < count and deadline > 0:
                    await asyncio.sleep(0.05)
                    deadline -= 0.05
            finally:
                await control_bridge.stop()
                await robot_bridge.stop()
                control_node.destroy_node()
                robot_node.destroy_node()
    rclpy.shutdown()
    print(f"[teleop] robot acted on {len(driven)}/{count} commands via Ditto", flush=True)
    return driven
