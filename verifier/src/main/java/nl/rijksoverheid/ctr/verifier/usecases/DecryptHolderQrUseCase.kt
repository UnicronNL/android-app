package nl.rijksoverheid.ctr.verifier.usecases

import android.util.Base64
import clmobile.Clmobile
import com.squareup.moshi.Moshi
import nl.rijksoverheid.ctr.shared.ext.verify
import nl.rijksoverheid.ctr.shared.util.CryptoUtil
import timber.log.Timber

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class DecryptHolderQrUseCase {

    fun decrypt(
        content: String
    ): Long {
        val decodedContent = Base64.decode(content.toByteArray(), Base64.NO_WRAP)
        val result =
            Clmobile.verify(nl.rijksoverheid.ctr.shared.util.CryptoUtil.ISSUER_PK_XML.toByteArray(), decodedContent).verify()
        Timber.i("QR Code created at ${result.unixTimeSeconds}")
        return result.unixTimeSeconds
    }
}