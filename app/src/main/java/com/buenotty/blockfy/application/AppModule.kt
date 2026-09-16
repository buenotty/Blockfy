package com.buenotty.blockfy.application

import com.buenotty.blockfy.datastore.DataStoreManager
import com.buenotty.blockfy.feature_preferences.OverviewViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.androidx.workmanager.dsl.workerOf
import com.buenotty.blockfy.worker.FeatureToggleWorker

object AppModule {
    fun modules() = commonModule+ workerModule + viewModelModule
}

val viewModelModule = module {
    viewModel { OverviewViewModel(dataStoreManager = get()) }
}

val commonModule = module {
    single { DataStoreManager(androidContext()) }
}

val workerModule = module {
    workerOf(::FeatureToggleWorker)
}