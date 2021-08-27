package nl.rijksoverheid.ctr.holder.ui.create_qr.usecases

import nl.rijksoverheid.ctr.holder.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.*
import nl.rijksoverheid.ctr.holder.ui.create_qr.repositories.CoronaCheckRepository
import nl.rijksoverheid.ctr.shared.models.ErrorResult
import nl.rijksoverheid.ctr.shared.models.NetworkRequestResult

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

/**
 * Get events for a specific [OriginType]
 * This is the entry point class for getting Events and will take care of:
 * - getting all event providers
 * - getting tokens based on JWT
 * - getting events at event providers
 * - map result to success or error states
 */
interface GetEventsUseCase {
    suspend fun getEvents(jwt: String,
                          originType: OriginType,
                          targetProviderIds: List<String>? = null): EventsResult
}

class GetEventsUseCaseImpl(
    private val configProvidersUseCase: ConfigProvidersUseCase,
    private val coronaCheckRepository: CoronaCheckRepository,
    private val getEventProvidersWithTokensUseCase: GetEventProvidersWithTokensUseCase,
    private val getRemoteEventsUseCase: GetRemoteEventsUseCase
) : GetEventsUseCase {

    override suspend fun getEvents(
        jwt: String,
        originType: OriginType,
        targetProviderIds: List<String>?
    ): EventsResult {
        // Fetch event providers
        return when (val eventProvidersResult = configProvidersUseCase.eventProviders()) {
            is EventProvidersResult.Error -> {
                EventsResult.Error(eventProvidersResult.errorResult)
            }
            is EventProvidersResult.Success -> {
                // Fetch access tokens
                when (val tokensResult = coronaCheckRepository.accessTokens(jwt)) {
                    is NetworkRequestResult.Failed -> EventsResult.Error(tokensResult)
                    is NetworkRequestResult.Success -> {
                        val tokens = tokensResult.response

                        // Fetch event providers that have events for us
                        val eventProviderWithTokensResults = getEventProvidersWithTokensUseCase.get(
                            eventProviders = eventProvidersResult.eventProviders,
                            tokens = tokens.tokens,
                            originType = originType,
                            targetProviderIds = targetProviderIds
                        )

                        val eventProvidersWithTokensSuccessResults =
                            eventProviderWithTokensResults.filterIsInstance<EventProviderWithTokenResult.Success>()
                        val eventProvidersWithTokensErrorResults =
                            eventProviderWithTokensResults.filterIsInstance<EventProviderWithTokenResult.Error>()

                        if (eventProvidersWithTokensSuccessResults.isNotEmpty()) {

                            // We have received providers that claim to have events for us so we get those events for each provider
                            val eventResults = eventProvidersWithTokensSuccessResults.map {
                                getRemoteEventsUseCase.getRemoteEvents(
                                    eventProvider = it.eventProvider,
                                    token = it.token,
                                    originType = originType
                                )
                            }

                            // All successful responses
                            val eventSuccessResults =
                                eventResults.filterIsInstance<RemoteEventsResult.Success>()

                            // All failed responses
                            val eventFailureResults =
                                eventResults.filterIsInstance<RemoteEventsResult.Error>()

                            if (eventSuccessResults.isNotEmpty()) {
                                // If we have success responses
                                val signedModels = eventSuccessResults.map { it.signedModel }
                                val hasEvents = signedModels.map { it.model }
                                    .any { it.events?.isNotEmpty() ?: false }

                                if (!hasEvents) {
                                    // But we do not have any events
                                    EventsResult.HasNoEvents(
                                        missingEvents = eventProvidersWithTokensErrorResults.isNotEmpty() || eventFailureResults.isNotEmpty()
                                    )
                                } else {
                                    // We do have events
                                    EventsResult.Success(
                                        signedModels = signedModels,
                                        missingEvents = eventProvidersWithTokensErrorResults.isNotEmpty() || eventFailureResults.isNotEmpty()
                                    )
                                }
                            } else {
                                // We don't have any successful responses from retrieving events for providers
                                EventsResult.Error(eventFailureResults.map { it.errorResult })

//                                val isNetworkError =
//                                    eventFailureResults.any { it is RemoteEventsResult.Error.NetworkError }
//                                if (isNetworkError) {
//                                    EventsResult.Error.NetworkError
//                                } else {
//                                    EventsResult.Error.EventProviderError.ServerError
//                                }
                            }
                        } else {
                            if (eventProvidersWithTokensErrorResults.isEmpty()) {
                                // There are no successful responses and no error responses so no events
                                EventsResult.HasNoEvents(missingEvents = false)
                            } else {
                                // We don't have any successful responses but do have error responses
                                    EventsResult.Error(eventProvidersWithTokensErrorResults.map { it.errorResult })
//                                val isNetworkError =
//                                    eventProvidersWithTokensErrorResults.any { it is EventProviderWithTokenResult.Error.NetworkError }
//                                if (isNetworkError) {
//                                    EventsResult.Error.NetworkError
//                                } else {
//                                    EventsResult.Error.EventProviderError.ServerError
//                                }
                            }
                        }
                    }
                }
            }
        }


    }
}

sealed class EventsResult {
    data class Success (
        val signedModels: List<SignedResponseWithModel<RemoteProtocol3>>,
        val missingEvents: Boolean
    ) :
        EventsResult()
    data class HasNoEvents(val missingEvents: Boolean) : EventsResult()

    data class Error constructor(val errorResults: List<ErrorResult>): EventsResult() {
        constructor(errorResult: ErrorResult): this(listOf(errorResult))
    }

//    sealed class Error(): EventsResult() {
//        object NetworkError : Error()
//
//        sealed class CoronaCheckError: Error() {
//            data class ServerError(val httpCode: Int) : CoronaCheckError()
//        }
//
//        sealed class EventProviderError: Error() {
//            object ServerError : EventProviderError()
//        }
//    }
}
