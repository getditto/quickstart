"""Smoke tests: each demo runs in --sim and proves data crossed via Ditto.

Requires an offline-only license token in ``DITTO_OFFLINE_TOKEN`` (the peers
must activate to sync); the tests skip without it so the suite stays portable.
"""

from __future__ import annotations

import asyncio
import os

import pytest

from ditto_ros2 import fleet, talker_listener, teleop

_TOKEN = os.environ.get("DITTO_OFFLINE_TOKEN", "").strip()
requires_token = pytest.mark.skipif(not _TOKEN, reason="set DITTO_OFFLINE_TOKEN to run the demos")


@requires_token
def test_talker_listener_crosses_graphs() -> None:
    heard = asyncio.run(talker_listener.run(count=4, interval=0.2, sim=True, token=_TOKEN))
    assert heard == [f"hello world {i}" for i in range(4)]


@requires_token
def test_teleop_commands_reach_the_robot() -> None:
    driven = asyncio.run(teleop.run(count=4, interval=0.2, sim=True, token=_TOKEN))
    assert len(driven) == 4


@requires_token
def test_fleet_shares_one_world_model() -> None:
    views = asyncio.run(fleet.run(robots=3, count=4, interval=0.2, sim=True, token=_TOKEN))
    # Every robot sees every robot's latest pose.
    assert set(views) == {"robot-1", "robot-2", "robot-3"}
    for view in views.values():
        assert set(view) == {"robot-1", "robot-2", "robot-3"}
