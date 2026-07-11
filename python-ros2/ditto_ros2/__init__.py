"""Canonical ROS 2 examples bridged through Ditto.

Three small demos show Ditto carrying robot state where ROS 2's DDS pub/sub
can't reach — across separate ROS graphs, offline, with no central broker:

* ``talker_listener`` — the ROS 2 "hello world" (``std_msgs/String`` on
  ``/chatter``) with the talker and listener in separate graphs, joined only by
  Ditto.
* ``teleop`` — a control station drives a robot (``geometry_msgs/Twist`` on
  ``/cmd_vel``) over the Ditto mesh.
* ``fleet`` — every robot writes its pose into a shared Ditto collection and
  observes the whole fleet: a CRDT-merged shared world model instead of
  transient pub/sub.
"""
