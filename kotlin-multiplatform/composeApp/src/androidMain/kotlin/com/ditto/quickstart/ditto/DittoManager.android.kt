package com.ditto.quickstart.ditto

import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoIdentity
import com.ditto.quickstart.App

actual fun createDittoConfig(identity: DittoIdentity): DittoConfig =
    DittoConfig(
        identity = identity,
        context = App.instance
    )
