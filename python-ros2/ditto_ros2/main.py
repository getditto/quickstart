"""CLI for the Ditto ↔ ROS 2 quickstart demos.

    ditto-ros2 talker-listener --sim
    ditto-ros2 teleop --sim
    ditto-ros2 fleet --sim --robots 4
    ditto-ros2 all --sim

Set an offline-only license token in ``DITTO_OFFLINE_TOKEN`` (a local ``.env``
file is loaded automatically) so the peers activate; without it they run
local-only. Drop ``--sim`` on a machine with ROS 2 installed to use real
``rclpy`` and the standard message packages.
"""

from __future__ import annotations

import argparse
import asyncio

from dotenv import load_dotenv

from . import fleet, talker_listener, teleop

_DEMOS = {
    "talker-listener": "ROS 2 hello world across two graphs, joined by Ditto",
    "teleop": "drive a robot from a separate control station over Ditto",
    "fleet": "robots share a live world model through a Ditto collection",
    "all": "run all three demos in sequence",
}


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="ditto-ros2", description="Canonical ROS 2 examples bridged through Ditto."
    )
    subparsers = parser.add_subparsers(dest="demo", required=True)
    for name, help_text in _DEMOS.items():
        sub = subparsers.add_parser(name, help=help_text)
        sub.add_argument(
            "--sim", action="store_true", help="force the in-process rclpy shim (no ROS 2 needed)"
        )
        sub.add_argument("--count", type=int, default=6, help="messages/ticks to send")
        sub.add_argument("--interval", type=float, default=0.4, help="seconds between messages")
        if name in ("fleet", "all"):
            sub.add_argument("--robots", type=int, default=3, help="number of robots (fleet demo)")
    return parser


async def _dispatch(args: argparse.Namespace) -> None:
    sim = True if args.sim else None
    robots = getattr(args, "robots", 3)
    if args.demo in ("talker-listener", "all"):
        print("== talker / listener ==", flush=True)
        await talker_listener.run(count=args.count, interval=args.interval, sim=sim)
    if args.demo in ("teleop", "all"):
        print("== teleop ==", flush=True)
        await teleop.run(count=args.count, interval=args.interval, sim=sim)
    if args.demo in ("fleet", "all"):
        print("== fleet ==", flush=True)
        await fleet.run(robots=robots, count=args.count, interval=args.interval, sim=sim)


def main(argv: list[str] | None = None) -> int:
    load_dotenv()
    args = _parser().parse_args(argv)
    try:
        asyncio.run(_dispatch(args))
    except KeyboardInterrupt:
        return 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
