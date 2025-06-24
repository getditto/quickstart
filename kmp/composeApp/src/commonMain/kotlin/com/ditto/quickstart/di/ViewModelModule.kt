package com.ditto.quickstart.di

import com.ditto.quickstart.ui.AppViewModel
import com.ditto.quickstart.ui.MainScreenViewModel
import com.ditto.quickstart.ui.TaskAddEditScreenViewModel
import com.ditto.quickstart.ui.TaskListScreenViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun viewModelModule() = module {
    viewModelOf(::AppViewModel)
    viewModelOf(::MainScreenViewModel)
    viewModelOf(::TaskListScreenViewModel)
    viewModelOf(::TaskAddEditScreenViewModel)
}
