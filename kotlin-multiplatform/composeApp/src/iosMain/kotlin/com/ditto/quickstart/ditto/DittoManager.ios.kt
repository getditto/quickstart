package com.ditto.quickstart.ditto

import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoFactory

actual fun createDitto(config: DittoConfig): Ditto =
    DittoFactory.create(
        config = config
    )
