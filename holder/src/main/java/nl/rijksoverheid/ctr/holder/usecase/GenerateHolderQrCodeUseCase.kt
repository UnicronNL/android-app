package nl.rijksoverheid.ctr.holder.usecase

import android.graphics.Bitmap
import com.squareup.moshi.Moshi
import nl.rijksoverheid.ctr.shared.util.CryptoUtil
import nl.rijksoverheid.ctr.shared.util.QrCodeScannerUtil

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class GenerateHolderQrCodeUseCase(
    private val cryptoUtil: CryptoUtil,
    private val moshi: Moshi,
    private val qrCodeScannerUtil: QrCodeScannerUtil
) {

    fun bitmap(
        data: String,
        qrCodeWidth: Int,
        qrCodeHeight: Int
    ): Bitmap {
        return qrCodeScannerUtil.createQrCode(
            data,
            qrCodeWidth,
            qrCodeHeight
        )
    }
}
