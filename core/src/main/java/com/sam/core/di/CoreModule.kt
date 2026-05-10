package com.sam.core.di

import com.sam.core.navigation.Navigator
import com.sam.core.navigation.NavigatorImpl
import org.koin.dsl.module

val coreModule = module {
    single<Navigator> { NavigatorImpl() }
}
