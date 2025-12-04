# AIDL Implmentation with Quickstart Tasks app

This is the Quickstart tasks app where the Ditto functions have been moved out to a separate module.
This module is intended to be built as a standalone APK that can be interacted with via AIDL.

## About dittowrapper

The dittowrapper module is designed to be installed as an independent APK and run as a background service. It includes:
- A blank `MainActivity` that immediately finishes after launch (required for APK installation)
- AIDL service interfaces for inter-process communication with other applications
- Kotlin 2.1.0 for modern language features
