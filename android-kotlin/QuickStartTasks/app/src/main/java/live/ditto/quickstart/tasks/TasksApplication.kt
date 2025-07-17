package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.quickstart.tasks.data.DataManager
import live.ditto.quickstart.tasks.data.DittoManager
import live.ditto.quickstart.tasks.edit.EditScreenViewModel
import live.ditto.quickstart.tasks.list.TasksListScreenViewModel
import live.ditto.quickstart.tasks.models.DittoConfig
import live.ditto.quickstart.tasks.services.ErrorService
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class TasksApplication : Application() {

    companion object {
        private var instance: TasksApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        // Start Koin dependency injection
        // https://insert-koin.io/docs/reference/koin-android/start
        GlobalContext.startKoin {
            androidLogger()
            androidContext(this@TasksApplication)
            modules(registerDependencies())
        }
    }

    private fun registerDependencies() : Module {
        return module {
            val appId = BuildConfig.DITTO_APP_ID
            val token = BuildConfig.DITTO_PLAYGROUND_TOKEN
            val authUrl = BuildConfig.DITTO_AUTH_URL
            val webSocketURL = BuildConfig.DITTO_WEBSOCKET_URL

            // Create DittoConfig as a single instance
            single {
                DittoConfig(
                    authUrl,
                    webSocketURL,
                    appId,
                    token
                )
            }

            // Create DittoManager with injected dependencies
             single<DataManager> {
                DittoManager(
                    dittoConfig = get(),     // Koin will provide the DittoConfig instance
                    androidDittoDependencies = DefaultAndroidDittoDependencies(this@TasksApplication),
                    errorService = get()
                )
            }

            // Create TasksListScreenViewModel with injected DittoManager
            viewModel { TasksListScreenViewModel(get(), this@TasksApplication) }

            // Create EditScreenViewModel with injected DittoManager
            viewModel { EditScreenViewModel(get()) }

            // add in the ErrorService which is used to display errors in the app
            single { ErrorService() }
        }
    }
}
