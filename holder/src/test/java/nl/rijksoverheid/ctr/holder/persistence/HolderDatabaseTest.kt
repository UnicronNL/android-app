package nl.rijksoverheid.ctr.holder.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.ctr.holder.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.holder.persistence.database.entities.*
import nl.rijksoverheid.ctr.holder.persistence.database.models.GreenCard
import nl.rijksoverheid.ctr.holder.persistence.database.models.Wallet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
@RunWith(RobolectricTestRunner::class)
class HolderDatabaseTest : AutoCloseKoinTest() {

    private lateinit var db: HolderDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HolderDatabase::class.java).build()
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    @Test
    fun `Creating entities returns correct Wallet`() = runBlocking {
        val wallet = getDummyWallet()

        // Insert entities into database
        insertWalletInDatabase(
            wallet = wallet
        )

        // First wallet in database should match our dummy wallet object
        assertEquals(listOf(wallet), db.walletDao().get().first())
    }

    @Test
    fun `Remove wallet clears correct database entries`() = runBlocking {
        val wallet = getDummyWallet()

        // Insert entities into database
        insertWalletInDatabase(
            wallet = wallet
        )

        // Delete the wallet entity
        db.walletDao().delete(1)

        // Database entries should no longer exist
        assertEquals(db.walletDao().get().first(), listOf<Wallet>())
        assertEquals(db.eventDao().getAll(), listOf<EventEntity>())
        assertEquals(db.greenCardDao().getAll(), listOf<GreenCardEntity>())
        assertEquals(db.credentialDao().getAll(), listOf<CredentialEntity>())
    }

    private fun getDummyWallet() = Wallet(
        walletEntity = WalletEntity(
            id = 1,
            label = "main"
        ),
        eventEntities = listOf(
            EventEntity(
                id = 1,
                walletId = 1,
                type = EventType.Vaccination,
                issuedAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(1), ZoneOffset.UTC),
                jsonData = ""
            ),
            EventEntity(
                id = 2,
                walletId = 1,
                type = EventType.Vaccination,
                issuedAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(2), ZoneOffset.UTC),
                jsonData = ""
            )
        ),
        greenCards = listOf(
            GreenCard(
                greenCardEntity = GreenCardEntity(
                    id = 1,
                    walletId = 1,
                    type = GreenCardType.EuTest,
                    issuedAt = OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(1),
                        ZoneOffset.UTC
                    )
                ),
                credentialEntities = listOf(
                    CredentialEntity(
                        id = 1,
                        greenCardId = 1,
                        qrData = "",
                        validFrom = OffsetDateTime.ofInstant(
                            Instant.ofEpochSecond(1),
                            ZoneOffset.UTC
                        )
                    )
                )
            ),
            GreenCard(
                greenCardEntity = GreenCardEntity(
                    id = 2,
                    walletId = 1,
                    type = GreenCardType.EuVaccination,
                    issuedAt = OffsetDateTime.ofInstant(
                        Instant.ofEpochSecond(2),
                        ZoneOffset.UTC
                    )
                ),
                credentialEntities = listOf(
                    CredentialEntity(
                        id = 2,
                        greenCardId = 2,
                        qrData = "",
                        validFrom = OffsetDateTime.ofInstant(
                            Instant.ofEpochSecond(2),
                            ZoneOffset.UTC
                        )
                    )
                )
            )
        )
    )

    private suspend fun insertWalletInDatabase(wallet: Wallet) {
        db.walletDao().insert(wallet.walletEntity)
        wallet.eventEntities.forEach {
            db.eventDao().insert(it)
        }
        wallet.greenCards.forEach { greenCard ->
            db.greenCardDao().insert(greenCard.greenCardEntity)
            greenCard.credentialEntities.forEach {
                db.credentialDao().insert(it)
            }
        }
    }
}
