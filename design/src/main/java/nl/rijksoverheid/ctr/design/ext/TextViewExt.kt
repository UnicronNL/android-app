package nl.rijksoverheid.ctr.design.ext

import android.text.method.LinkMovementMethod
import android.widget.TextView
import nl.rijksoverheid.ctr.design.spans.LinkTransformationMethod

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
fun TextView.enableHtmlLinks() {
    this.transformationMethod = LinkTransformationMethod()
    this.movementMethod = LinkMovementMethod.getInstance()
}
