package nl.rijksoverheid.ctr.holder.ui.create_qr.usecases

import nl.rijksoverheid.ctr.holder.ui.create_qr.models.*
import nl.rijksoverheid.ctr.holder.ui.create_qr.repositories.CoronaCheckRepository
import nl.rijksoverheid.ctr.holder.ui.create_qr.repositories.EventProviderRepository
import retrofit2.HttpException
import java.io.IOException

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
interface GetEventsUseCase {
    suspend fun getVaccinationEvents(digidToken: String): EventsResult<RemoteEvents>
    suspend fun getNegativeTestEvents(digidToken: String): EventsResult<RemoteEventsNegativeTests>
}

class GetEventsUseCaseImpl(
    private val configProvidersUseCase: ConfigProvidersUseCase,
    private val coronaCheckRepository: CoronaCheckRepository,
    private val eventProviderRepository: EventProviderRepository
) : GetEventsUseCase {

    private suspend fun getEventProviderWithEvent(digidToken: String):  Map<RemoteConfigProviders.EventProvider, RemoteAccessTokens.Token> {
        // Fetch event providers
        val eventProviders = configProvidersUseCase.eventProviders()

        // Fetch access tokens
        val accessTokens = coronaCheckRepository.accessTokens(digidToken)

        // Map event providers to access tokens
        val eventProvidersWithAccessTokenMap =
            eventProviders.associateWith { eventProvider -> accessTokens.tokens.first { eventProvider.providerIdentifier == it.providerIdentifier } }

        // A list of event providers that have events
        return eventProvidersWithAccessTokenMap.filter {
            val eventProvider = it.key
            val accessToken = it.value

            try {
                val unomi = eventProviderRepository.unomi(
                    url = eventProvider.unomiUrl,
                    token = accessToken.unomi
                )
                unomi.informationAvailable
            } catch (e: HttpException) {
                false
            } catch (e: IOException) {
                false
            }
        }
    }

    override suspend fun getVaccinationEvents(digidToken: String): EventsResult<RemoteEvents>{
        return try {

            val eventProviderWithEvents = getEventProviderWithEvent(digidToken)

            // Get vaccination events from event providers
            val remoteEvents = eventProviderWithEvents.map {
                val eventProvider = it.key
                val accessToken = it.value

                eventProviderRepository
                    .event(
                        url = eventProvider.eventUrl,
                        token = accessToken.event,
                        signingCertificateBytes = eventProvider.cms
                    )
            }

            // For now we only support vaccination events
            val vaccinationEvents =
                remoteEvents.filter { remoteEvent -> remoteEvent.model.events.any { event -> event.type == "vaccination" } }

            EventsResult.Success(
                signedModels = vaccinationEvents
            )
        } catch (ex: HttpException) {
            return EventsResult.ServerError(ex.code())
        } catch (ex: IOException) {
            return EventsResult.NetworkError
        }
    }

    override suspend fun getNegativeTestEvents(digidToken: String): EventsResult<RemoteEventsNegativeTests> {
        return try {

            val eventProviderWithEvents = getEventProviderWithEvent(digidToken)

            // Get vaccination events from event providers
            val negativeTestEvents = eventProviderWithEvents.map {
                val eventProvider = it.key
                val accessToken = it.value

                eventProviderRepository
                    .negativeTestEvent(
                        url = eventProvider.eventUrl,
                        token = accessToken.event,
                        signingCertificateBytes = eventProvider.cms
                    )
            }

            EventsResult.Success(
                signedModels = negativeTestEvents
            )
        } catch (ex: HttpException) {
            return EventsResult.ServerError(ex.code())
        } catch (ex: IOException) {
            return EventsResult.NetworkError
        }
    }
}

sealed class EventsResult<out T> {
    data class Success<T> (
        val signedModels: List<SignedResponseWithModel<T>>
    ) :
        EventsResult<T>()

    data class ServerError(val httpCode: Int) : EventsResult<Nothing>()
    object NetworkError : EventsResult<Nothing>()
}
