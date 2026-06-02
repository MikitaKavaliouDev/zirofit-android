package com.ziro.fit.data.repository

import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.model.ExploreEvent
import com.ziro.fit.model.ExploreEventsResponse
import com.ziro.fit.model.ExploreFeaturedResponse
import com.ziro.fit.model.ExploreMetadataResponse
import com.ziro.fit.model.PromotedTrainerSummary
import com.ziro.fit.model.PromotedTrainersResponse
import com.ziro.fit.model.SubscribeNotificationsRequest
import com.ziro.fit.util.ApiErrorParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExploreRepository @Inject constructor(
    private val api: ZiroApi
) {
    suspend fun getEvents(
        page: Int = 1,
        limit: Int = 20,
        categoryId: String? = null,
        search: String? = null,
        isFree: Boolean? = null
    ): Result<ExploreEventsResponse> {
        return try {
            val response = api.getExploreEvents(
                page = page,
                limit = limit,
                categoryId = categoryId,
                search = search,
                isFree = isFree
            )
            if (response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load events"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }

    suspend fun getEventDetails(eventId: String): Result<ExploreEvent> {
        return try {
            val response = api.getEventDetails(eventId)
            if (response.data != null) {
                Result.success(response.data.event)
            } else {
                Result.failure(Exception(response.message ?: "Event not found"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }

    suspend fun joinFreeEvent(eventId: String): Result<Unit> {
        return try {
            val response = api.joinFreeEvent(eventId)
            if (response.success != false) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to join event"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }

    suspend fun getMetadata(): Result<ExploreMetadataResponse> {
        return try {
            val response = api.getExploreMetadata()
            if (response.data != null) Result.success(response.data)
            else Result.failure(Exception("Metadata empty"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFeatured(lat: Double? = null, long: Double? = null, cityId: String? = null): Result<ExploreFeaturedResponse> {
        return try {
            val response = api.getExploreFeatured(lat, long, cityId)
            if (response.data != null) Result.success(response.data)
            else Result.failure(Exception("Featured content empty"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrainerEvents(): Result<List<ExploreEvent>> {
        return try {
            val response = api.getTrainerEvents()
            if (response.data != null) {
                Result.success(response.data.events)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load trainer events"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }

    suspend fun createEvent(event: ExploreEvent): Result<ExploreEvent> {
        return try {
            val response = api.createEvent(event)
            if (response.data != null) {
                Result.success(response.data.event)
            } else {
                Result.failure(Exception(response.message ?: "Failed to create event"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }

    suspend fun updateEvent(id: String, event: ExploreEvent): Result<ExploreEvent> {
        return try {
            val response = api.updateEvent(id, event)
            if (response.data != null) {
                Result.success(response.data.event)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update event"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }

    suspend fun deleteEvent(id: String): Result<Unit> {
        return try {
            val response = api.deleteEvent(id)
            if (response.success != false) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to delete event"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }

    suspend fun getPromotedTrainers(category: String? = null): Result<List<PromotedTrainerSummary>> {
        return try {
            val response = api.getPromotedTrainers(category = category)
            if (response.data != null) {
                Result.success(response.data.trainers)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load promoted trainers"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }

    suspend fun subscribeToNotifications(topic: String): Result<Unit> {
        return try {
            val response = api.subscribeToNotifications(SubscribeNotificationsRequest(topic))
            if (response.success != false) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to subscribe to notifications"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.getErrorMessage(ApiErrorParser.parse(e))))
        }
    }
}
