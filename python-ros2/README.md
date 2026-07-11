# Ditto ↔ ROS 2 quickstart

Three small, canonical ROS 2 examples that use **Ditto as the shared-state layer
instead of DDS pub/sub**. ROS 2's pub/sub is transient and lives on a single DDS
domain; Ditto gives robots a **persistent, CRDT-merged, offline-first** store
that syncs peer-to-peer over Wi-Fi/LAN/BLE with **no central broker** — so state
crosses separate ROS graphs and survives dropped links.

Every demo runs with **no ROS 2 installed** via a tiny in-process `rclpy` shim
(`--sim`); drop `--sim` on a machine with ROS 2 to use real `rclpy` and the
standard message packages.

## The three demos

| Demo | ROS message / topic | What Ditto adds |
| ---- | ------------------- | --------------- |
| **talker / listener** | `std_msgs/String` on `/chatter` | The ROS 2 "hello world," but talker and listener are in **separate ROS graphs** joined only by Ditto — pub/sub across a boundary DDS can't cross. |
| **teleop** | `geometry_msgs/Twist` on `/cmd_vel` | A control station drives a robot over the mesh; commands **queue and converge** if the link drops — command/control without a broker. |
| **fleet** | `geometry_msgs/Pose2D` on `/pose` | Every robot writes its pose into a shared `fleet` collection keyed by robot id and observes the whole fleet: a **shared world model** (one live row per robot), not a stream of transient messages. |

The `fleet` demo is the clearest contrast: DDS has no retained "latest value per
robot," no persistence across disconnects, and no state once a publisher exits —
the Ditto collection has all three.

## Run it (no ROS 2 needed)

```bash
# from the quickstart repo root
just python-ros2 fleet          # or: talker-listener | teleop | all

# or directly
cd python-ros2
python3 -m venv .venv && source .venv/bin/activate
pip install -e .
export DITTO_OFFLINE_TOKEN="<your-offline-license-token>"   # or put it in .env
ditto-ros2 all --sim
ditto-ros2 fleet --sim --robots 4
```

Get an offline-only license token from the [Ditto Portal](https://portal.ditto.live).
Without a token the peers can't activate sync; a local `.env` file with
`DITTO_OFFLINE_TOKEN=...` is loaded automatically.

Expected `fleet` output (abridged):

```
[fleet] tick 0: robot-1 sees robot-1=0.00,0.00, robot-2=1.00,1.00, robot-3=2.00,2.00
...
[fleet] every robot now shares one world model of 3/3 robots
```

## Run against real ROS 2

`rclpy` and the message packages come from your ROS 2 install (not pip). With
ROS 2 sourced, omit `--sim` and the demos use real `rclpy` automatically:

```bash
source /opt/ros/humble/setup.bash
pip install -e .
ditto-ros2 teleop            # real rclpy, real geometry_msgs/Twist
```

A ready-to-run container is in [`docker/`](docker/) (`docker compose -f
docker/docker-compose.yml up`), which builds `ros:humble` with the SDK installed.

## How it works

- `ditto_ros2/bridge.py` — `DittoRos2Bridge`: maps a ROS topic ↔ a Ditto
  collection per `Route` (used by talker/listener and teleop).
- `ditto_ros2/fleet.py` — the shared-world-model demo (upsert-keyed-by-robot +
  observe), written directly rather than through the streaming bridge.
- `ditto_ros2/rclpy_shim.py` + `ros_compat.py` — the in-process ROS stand-in and
  the single switch between it and real `rclpy`.
- `ditto_ros2/peers.py` — opens offline peers wired into a loopback TCP mesh so
  a single-process demo genuinely syncs peer-to-peer.

## Test

```bash
pip install -e . pytest
DITTO_OFFLINE_TOKEN=<token> pytest -q     # runs each demo in --sim and asserts it synced
```
