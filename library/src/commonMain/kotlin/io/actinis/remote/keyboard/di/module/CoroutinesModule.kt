package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.di.name.DispatchersNames
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val coroutinesModule = module {
    single(named(DispatchersNames.DEFAULT)) { Dispatchers.Default }
    single(named(DispatchersNames.IO)) { Dispatchers.IO }
    single<CoroutineDispatcher>(named(DispatchersNames.MAIN)) { Dispatchers.Main }
}