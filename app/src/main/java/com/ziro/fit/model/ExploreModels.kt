package com.ziro.fit.model

import com.google.gson.annotations.SerializedName

data class ExploreCity(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isCurrentLocation: Boolean = false
)

data class ExploreCategory(
    val id: String,
    val name: String,
    val iconUrl: String? = null
)

data class ExploreMetadataResponse(
    val cities: List<ExploreCity>,
    val categories: List<ExploreCategory>
)

data class ExploreFeaturedResponse(
    val featuredEvents: List<ExploreEvent>,
    val featuredTrainers: List<FeaturedTrainerSummary>
)

data class ExploreEvent(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: String,
    val endTime: String?,
    val price: Double?,
    val currency: String?,
    val locationName: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val categoryId: String?,
    val cityId: String?,
    val priceDisplay: String?,
    val hostName: String?,
    val hostId: String?,
    val trainerName: String?,
    val trainerId: String?,
    val enrolledCount: Int?,
    val capacity: Int?,
    val isBooked: Boolean?,
    val isNearCapacity: Boolean?,
    val trainer: EventTrainer?,
    val status: String? = null,
    val rejectionReason: String? = null,
    val organizerType: String? = null,
    val highlights: List<String>? = null
) {
    val resolvedHostName: String?
        get() = hostName ?: trainerName ?: trainer?.name

    val isFull: Boolean
        get() = (enrolledCount ?: 0) >= (capacity ?: 1)

    val spotsLeft: Int
        get() = maxOf(0, (capacity ?: 0) - (enrolledCount ?: 0))

    val isPending: Boolean
        get() = status == "PENDING"

    val isRejected: Boolean
        get() = status == "REJECTED"

    val isApproved: Boolean
        get() = status == "APPROVED"
}

data class EventTrainer(
    val name: String?,
    val username: String?,
    val profile: EventTrainerProfile?
)

data class EventTrainerProfile(
    val profilePhotoPath: String?,
    val aboutMe: String?
)

data class ExploreEventsResponse(
    val events: List<ExploreEvent>,
    val pagination: PaginationData?
)

data class PaginationData(
    val total: Int?,
    val page: Int,
    val hasMore: Boolean
)

data class EventDetailResponse(
    val event: ExploreEvent
)

data class TrainerEventsListResponse(
    val events: List<ExploreEvent>
)

data class EventCreateResponse(
    val event: ExploreEvent
)

data class PromotedTrainersResponse(
    val trainers: List<PromotedTrainerSummary>
)

data class PromotedTrainerSummary(
    val id: String,
    val name: String,
    val username: String?,
    val avatarUrl: String?,
    val rating: Double?,
    val isVerified: Boolean?,
    val specialties: List<String>?,
    val promotionCategory: String? = null,
    val promotedAt: String? = null
) {
    fun toTrainerSummary(): TrainerSummary {
        return TrainerSummary(
            id = id,
            name = name,
            username = username,
            profile = TrainerProfileSummary(
                profilePhotoPath = avatarUrl,
                certifications = specialties?.joinToString(", "),
                averageRating = rating,
                locations = null,
                services = null
            )
        )
    }
}

data class SubscribeNotificationsRequest(
    val topic: String
)
