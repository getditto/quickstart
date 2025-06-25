package com.ditto.quickstart.di

import com.ditto.quickstart.usecases.CreateDittoUseCase
import com.ditto.quickstart.usecases.DestroyDittoUseCase
import com.ditto.quickstart.usecases.GetPersistedSyncStatusUseCase
import com.ditto.quickstart.usecases.IsDittoCreatedUseCase
import com.ditto.quickstart.usecases.IsDittoSyncingUseCase
import com.ditto.quickstart.usecases.SetSyncStatusUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun useCaseModule() = module {
    singleOf(::SetSyncStatusUseCase)
    singleOf(::GetPersistedSyncStatusUseCase)
    singleOf(::IsDittoCreatedUseCase)
    singleOf(::IsDittoSyncingUseCase)
    singleOf(::CreateDittoUseCase)
    singleOf(::DestroyDittoUseCase)
}
