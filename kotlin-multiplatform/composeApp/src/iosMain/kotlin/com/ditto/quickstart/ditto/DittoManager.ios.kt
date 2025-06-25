package com.ditto.quickstart.ditto

import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoIdentity

actual fun createDittoConfig(identity: DittoIdentity): DittoConfig {
    return DittoConfig(identity = identity)
}

