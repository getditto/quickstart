"""Helpers for opening offline Ditto peers wired into a loopback mesh.

The demos run several peers in one process and connect them over loopback TCP so
they genuinely sync peer-to-peer (no cloud, no broker) — the same code path a
real multi-robot deployment uses over Wi-Fi/LAN/BLE.
"""

from __future__ import annotations

import contextlib
import os
from collections.abc import AsyncIterator, Iterable

from ditto import Ditto, DittoConfig, DittoConfigConnect, DittoTransportConfig

LICENSE_ENV = "DITTO_OFFLINE_TOKEN"


def offline_token() -> str | None:
    """The offline-only license token from the environment, if set."""

    token = os.environ.get(LICENSE_ENV, "").strip()
    return token or None


def _loopback_transport(peer_port: int, connect_ports: Iterable[int]) -> DittoTransportConfig | None:
    if not peer_port:
        return None
    transport = DittoTransportConfig()
    transport.listen.tcp.enabled = True
    transport.listen.tcp.interface_ip = "127.0.0.1"
    transport.listen.tcp.port = peer_port
    for connect_port in connect_ports:
        transport.connect.tcp_servers.add(f"127.0.0.1:{connect_port}")
    return transport


@contextlib.asynccontextmanager
async def open_peer(
    *,
    database_id: str,
    directory: str,
    token: str | None = None,
    peer_port: int = 0,
    connect_ports: Iterable[int] = (),
) -> AsyncIterator[Ditto]:
    """Open an offline peer (optionally on a loopback TCP mesh) and close it on exit.

    Peers that share ``database_id`` and are linked by matching listen/connect
    ports sync with each other. ``token`` is the offline-only license; without
    it the peer runs local-only (fine for a single-process demo).
    """
    peer = await Ditto.open(
        DittoConfig(
            database_id=database_id,
            connect=DittoConfigConnect.small_peers_only(),
            persistence_directory=directory,
        )
    )
    try:
        if token:
            peer.set_offline_only_license_token(token)
        transport = _loopback_transport(peer_port, connect_ports)
        if transport is not None:
            peer.transport_config = transport
        yield peer
    finally:
        await peer.close()
