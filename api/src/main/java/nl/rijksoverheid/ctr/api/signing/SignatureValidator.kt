/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.ctr.signing

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.jcajce.JcaCertStoreBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms.CMSSignedDataParser
import org.bouncycastle.cms.CMSTypedStream
import org.bouncycastle.cms.SignerId
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.cert.*


class SignatureValidator private constructor(
    private val signingCertificate: X509Certificate?,
    private val trustAnchors: Set<TrustAnchor>,
    private val matchingString: String?,
) {

    class Builder {
        private var trustAnchors = mutableSetOf<TrustAnchor>()
        private var signingCertificate: X509Certificate? = null
        private var matchingString: String? = null

        /**
         * The subject key identifier of the root certificate that is used as the trust anchor. If unset the default value will be used.
         */
        fun addTrustedCertificate(certificate: X509Certificate): Builder {
            trustAnchors.add(TrustAnchor(certificate, null))
            return this
        }

        fun addTrustedCertificate(certificatePem: String): Builder {
            val factory = CertificateFactory.getInstance("X509")
            return addTrustedCertificate(
                factory.generateCertificate(
                    ByteArrayInputStream(
                        certificatePem.toByteArray()
                    )
                ) as X509Certificate
            )
        }

        /**
         * The signing certificate that needs to be for the signature. If set, the signing certificate needs to match this certificate
         * in order to pass the signature validation.
         */
        fun signingCertificate(signingCertificate: X509Certificate): Builder {
            this.signingCertificate = signingCertificate
            this.matchingString = null
            return this
        }

        fun signingCertificate(signingCertificateBytes: ByteArray): Builder {
            val x509 = CertificateFactory.getInstance("X509")
                .generateCertificate(ByteArrayInputStream(signingCertificateBytes)) as X509Certificate
            signingCertificate(
                x509
            )
            return this
        }

        /**
         * Set a substring that the CN of the signing certificate should match. Not used when [signingCertificate] is set.
         */
        fun cnMatching(substring: String): Builder {
            if (signingCertificate != null) {
                throw IllegalStateException("CN matching string cannot be used if signing certificate is set")
            }
            this.matchingString = substring
            return this
        }

        fun build(): SignatureValidator {
            return SignatureValidator(
                signingCertificate,
                trustAnchors,
                matchingString
            )
        }
    }

    private val provider = BouncyCastleProvider()

    fun verifySignature(content: InputStream, signature: ByteArray) {

        try {
            val sp = CMSSignedDataParser(
                JcaDigestCalculatorProviderBuilder().setProvider(provider)
                    .build(),
                CMSTypedStream(BufferedInputStream(content)), signature
            )

            sp.signedContent.drain()

            val certs = sp.certificates

            val store: CertStore =
                JcaCertStoreBuilder().setProvider(provider)
                    .apply {
                        for (anchor in trustAnchors) {
                            addCertificate(JcaX509CertificateHolder(anchor.trustedCert))
                        }
                    }
                    .addCertificates(certs)
                    .build()

            val signer =
                sp.signerInfos.signers.firstOrNull()
                    ?: throw SignatureValidationException("No signing certificate found")

            val result = checkCertPath(trustAnchors, signer.sid, store)
            val signingCertificate = result.certPath.certificates[0] as X509Certificate

            if (this.signingCertificate != null && this.signingCertificate != signingCertificate) {
                throw SignatureValidationException("Signing certificate does not match expected certificate")
            }

            if (matchingString != null) {
                val subjectRDNs = JcaX509CertificateHolder(signingCertificate).subject.getRDNs(BCStyle.CN).map { IETFUtils.valueToString(it.first.value) }
                val alternativeNames = getSubjectAlternativeNames(signingCertificate)
                val acceptableNames = subjectRDNs + alternativeNames
                if (!acceptableNames.any {
                        it.endsWith(matchingString)
                    }) {
                    throw SignatureValidationException("Signing certificate does not match expected CN")
                }
            }

            if (!signer.verify(
                    JcaSimpleSignerInfoVerifierBuilder().setProvider(provider)
                        .build(signingCertificate)
                )
            ) {
                throw SignatureValidationException("The signature does not match")
            }
        } catch (ex: CertPathBuilderException) {
            throw SignatureValidationException("The cert path cannot be validated")
        } catch (ex: SignatureValidationException) {
            throw ex
        } catch (ex: Exception) {
            throw SignatureValidationException("Error validating signature", ex)
        }
    }

    private fun checkCertPath(
        trustAnchors: Set<TrustAnchor>,
        signerId: SignerId,
        certs: CertStore
    ): PKIXCertPathBuilderResult {
        val pathBuilder: CertPathBuilder =
            CertPathBuilder.getInstance("PKIX", provider)
        val targetConstraints = X509CertSelector()

        // criteria to target the certificate to build the path to:
        // must match the signing certificate that we pass in, and the
        // signing certificate must have the correct authority key identifier, if one is configured
        targetConstraints.setIssuer(signerId.issuer.encoded)
        targetConstraints.serialNumber = signerId.serialNumber

        val params = PKIXBuilderParameters(
            trustAnchors,
            targetConstraints
        )
        params.addCertStore(certs)
        params.isRevocationEnabled = false
        return pathBuilder.build(params) as PKIXCertPathBuilderResult
    }

    companion object {
        fun getSubjectAlternativeNames(certificate: X509Certificate): List<String> {
            val identities: MutableList<String> = ArrayList()
            try {
                val altNames = certificate.subjectAlternativeNames ?: return emptyList()
                for (item in altNames) {
                    val type = item[0] as Int
                    if (type == 0 || type == 2) {
                        try {
                            var decoder: ASN1InputStream? = null
                            if (item.toTypedArray()[1] is ByteArray) decoder =
                                ASN1InputStream(item.toTypedArray()[1] as ByteArray?) else if (item.toTypedArray()[1] is String) identities.add(
                                item.toTypedArray()[1] as String
                            )
                            if (decoder == null) continue
                            var encoded = decoder.readObject()
                            encoded = (encoded as DERTaggedObject).getObject()
                            val identity = (encoded as DERUTF8String).string
                            identities.add(identity)
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                        }
                    }
                }
            } catch (exception: CertificateParsingException) {
                exception.printStackTrace()
            }
            return identities
        }
    }
}

class SignatureValidationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
