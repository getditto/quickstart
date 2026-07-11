"""Demo 1 — talker / listener, the ROS 2 "hello world", across two graphs.

A talker publishes ``std_msgs/String`` on ``/chatter``. A listener subscribes to
``/chatter`` — but it runs in a *separate* ROS graph (a second peer), so DDS
pub/sub alone would never connect them. The only link is Ditto: the talker's
node bridges ``/chatter`` into a Ditto collection, Ditto syncs it to the second
peer, and that peer republishes it onto its own ``/chatter`` for the listener.

Takeaway: Ditto carries ROS 2 pub/sub across graphs and networks a single DDS
domain can't span — offline and broker-free.
"""

from __future__ import annotations

import asyncio
import contextlib
import tempfile
import uuid

from .bridge import DittoRos2Bridge, Route
from .peers import offline_token, open_peer
from .ros_compat import get_message_type, get_rclpy, use_sim


async def run(
    *, count: int = 6, interval: float = 0.4, sim: bool | None = None, token: str | None = None
) -> list[str]:
    """Publish ``count`` messages on the talker; return what the listener heard."""
    resolved_sim = use_sim(sim)
    rclpy = get_rclpy(resolved_sim)
    string_type = get_message_type("String", resolved_sim)
    database_id = str(uuid.uuid4())
    token = token or offline_token()
    heard: list[str] = []

    rclpy.init()
    async with contextlib.AsyncExitStack() as stack:
        with tempfile.TemporaryDirectory(prefix="ditto-ros2-talker-") as dir_a, tempfile.TemporaryDirectory(
            prefix="ditto-ros2-listener-"
        ) as dir_b:
            talker_peer = await stack.enter_async_context(
                open_peer(database_id=database_id, directory=dir_a, token=token, peer_port=4101)
            )
            listener_peer = await stack.enter_async_context(
                open_peer(
                    database_id=database_id,
                    directory=dir_b,
                    token=token,
                    peer_port=4102,
                    connect_ports=[4101],
                )
            )

            talker_node = rclpy.create_node("talker")
            listener_node = rclpy.create_node("listener")

            def on_chatter(message: object) -> None:
                text = getattr(message, "data", "")
                heard.append(text)
                print(f"[listener] heard: {text!r}", flush=True)

            listener_node.create_subscription(string_type, "/chatter", on_chatter, 10)
            chatter = talker_node.create_publisher(string_type, "/chatter", 10)

            talker_bridge = DittoRos2Bridge(
                talker_peer,
                talker_node,
                [Route("chatter", "/chatter", "String", "ros_to_ditto")],
                robot_id="talker",
                sim=resolved_sim,
            )
            listener_bridge = DittoRos2Bridge(
                listener_peer,
                listener_node,
                [Route("chatter", "/chatter", "String", "ditto_to_ros")],
                robot_id="listener",
                sim=resolved_sim,
            )
            try:
                await talker_bridge.start()
                await listener_bridge.start()
                for index in range(count):
                    chatter.publish(string_type(data=f"hello world {index}"))
                    print(f"[talker] said: 'hello world {index}'", flush=True)
                    await asyncio.sleep(interval)
                # Drain: give the last messages time to sync + republish.
                deadline = count * interval + 3.0
                while len(heard) < count and deadline > 0:
                    await asyncio.sleep(0.05)
                    deadline -= 0.05
            finally:
                await talker_bridge.stop()
                await listener_bridge.stop()
                talker_node.destroy_node()
                listener_node.destroy_node()
    rclpy.shutdown()
    print(f"[talker/listener] delivered {len(heard)}/{count} messages via Ditto", flush=True)
    return heard
