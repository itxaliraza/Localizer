package di

import data.translator.MyTranslatorRepoImpl
import data.translator.apis.TranslatorApi1Impl
import data.translator.apis.TranslatorApi2Impl
import data.translator.apis.TranslatorApi3Impl
import home_screen.HomeScreenViewModel
import data.translator.TranslationManager
import org.koin.dsl.module

val SharedModule = module{
    factory {
        HomeScreenViewModel(get())
    }
    factory {
        MyTranslatorRepoImpl(get(),get(),get())
    }
    factory {
        TranslationManager(get())
    }
    factory {
        TranslatorApi1Impl()
    }

    factory {
        TranslatorApi2Impl()
    }
    factory {
        TranslatorApi3Impl()
    }
}