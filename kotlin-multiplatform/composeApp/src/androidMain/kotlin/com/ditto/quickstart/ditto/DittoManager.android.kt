package com.ditto.quickstart.ditto

import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoFactory
import com.ditto.quickstart.App

actual fun createDitto(config: DittoConfig): Ditto =
    DittoFactory.create(
        context = App.instance,
        config = config,
    )
