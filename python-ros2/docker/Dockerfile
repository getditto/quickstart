# Real ROS 2 (Humble) image with the Ditto Python SDK + this quickstart
# installed, running a demo against genuine rclpy + geometry_msgs/std_msgs.
FROM ros:humble-ros-base

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3-pip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/ditto-ros2
# Build context is the python-ros2 quickstart directory (see docker-compose.yml).
# The SDK bundles libdittoffi for the image's platform (amd64 or arm64 wheel).
COPY . /opt/ditto-ros2/
RUN pip3 install --no-cache-dir .

# `--sim` is intentionally omitted: with ROS 2 present, ros_compat uses real rclpy.
# Set DITTO_OFFLINE_TOKEN in the environment (see docker-compose.yml) to activate.
CMD ["bash", "-lc", "source /opt/ros/humble/setup.bash && ditto-ros2 fleet --robots 3"]
