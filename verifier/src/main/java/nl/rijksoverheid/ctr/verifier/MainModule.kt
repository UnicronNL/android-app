package nl.rijksoverheid.ctr.verifier

import androidx.preference.PreferenceManager
import nl.rijksoverheid.ctr.holder.persistence.PersistenceManager
import nl.rijksoverheid.ctr.holder.persistence.SharedPreferencesPersistenceManager
import nl.rijksoverheid.ctr.holder.usecase.IntroductionUseCase
import nl.rijksoverheid.ctr.verifier.introduction.IntroductionViewModel
import nl.rijksoverheid.ctr.verifier.scanqr.ScanQrViewModel
import nl.rijksoverheid.ctr.verifier.status.StatusViewModel
import nl.rijksoverheid.ctr.verifier.usecases.DecryptHolderQrUseCase
import nl.rijksoverheid.ctr.verifier.usecases.TestResultValidUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
val mainModule = module {

    single<PersistenceManager> {
        SharedPreferencesPersistenceManager(
            PreferenceManager.getDefaultSharedPreferences(
                androidContext()
            )
        )
    }

    // Use cases
    single {
        DecryptHolderQrUseCase(get())
    }
    single {
        IntroductionUseCase(get())
    }
    single {
        TestResultValidUseCase(get(), get(), get())
    }

    // ViewModels
    viewModel { StatusViewModel(get()) }
    viewModel { IntroductionViewModel(get()) }
    viewModel { ScanQrViewModel(get()) }
}
