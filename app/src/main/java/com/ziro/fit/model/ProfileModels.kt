package com.ziro.fit.model

import com.google.gson.annotations.SerializedName

data class ProfileCoreInfoResponse(
    val coreInfo: ProfileCoreInfo
)


data class ProfileCoreInfo(
    @SerializedName("name") val fullName: String?,
    val username: String?,
    val weightUnit: String?,
    val certifications: String?,
    val phone: String?,
    val specialties: List<String>,
    val trainingTypes: List<String>,
    val businessCurrency: String?,
    val aboutMe: String?,
    val locations: List<ProfileCoreLocation>
)

data class ProfileCoreLocation(
    val id: String?,
    val address: String?,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class UpdateCoreInfoRequest(
    @SerializedName("name") val fullName: String?,
    val username: String?,
    val weightUnit: String?,
    val certifications: String?,
    val phone: String?,
    val specialties: List<String>,
    val trainingTypes: List<String>,
    val businessCurrency: String?,
    val aboutMe: String?,
    val locations: List<ProfileCoreLocation>
)



data class ProfileBrandingResponse(
    val branding: ProfileBranding
)

data class ProfileBranding(
    val primaryColor: String?,
    val bannerImageUrl: String?,
    val profileImageUrl: String?
)

data class ProfileService(
    val id: String,
    val title: String,
    val description: String?,
    val price: Double?,
    val currency: String?
)

data class ProfileServicesResponse(
    val services: List<ProfileService>
)

data class ProfilePackage(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double?,
    val sessionCount: Int?
)

data class ProfilePackagesResponse(
    val packages: List<ProfilePackage>
)

data class ProfileAvailabilityResponse(
    val availability: ProfileAvailability
)

data class ProfileAvailability(
    val timeZone: String?,
    val schedule: Map<String, List<String>>?
)

data class ProfileTransformationPhoto(
    val id: String,
    val beforeUrl: String?,
    val afterUrl: String?,
    val date: String?,
    val description: String?
)

data class ProfileTransformationPhotosResponse(
    val photos: List<ProfileTransformationPhoto>
)

data class Testimonial(
    val id: String,
    val clientName: String,
    val content: String,
    val rating: Int?
)

data class ProfileTestimonialsResponse(
    val testimonials: List<Testimonial>
)

data class SocialLink(
    val id: String,
    val platform: String,
    val url: String
)

data class ProfileSocialLinksResponse(
    val links: List<SocialLink>
)

data class ExternalLink(
    val id: String,
    val title: String,
    val url: String
)

data class ProfileExternalLinksResponse(
    val links: List<ExternalLink>
)

data class ProfileBillingResponse(
    val billing: ProfileBilling
)

data class ProfileBilling(
    val stripeConnected: Boolean?,
    val subscriptionStatus: String?,
    @SerializedName("details_submitted") val detailsSubmitted: Boolean? = false,
    @SerializedName("charges_enabled") val chargesEnabled: Boolean? = false,
    @SerializedName("payouts_enabled") val payoutsEnabled: Boolean? = false,
    val accountId: String? = null
)

data class Benefit(
    val id: String,
    val text: String
)

data class ProfileBenefitsResponse(
    val benefits: List<Benefit>
)

data class Notification(
    val id: String,
    val userId: String,
    val message: String,
    val type: String,
    val readStatus: Boolean,
    val createdAt: String
)

data class GetNotificationsResponse(
    val notifications: List<Notification>
)

data class RegisterPushTokenRequest(
    val token: String
)

data class StripeOnboardingResponse(
    val url: String
)

data class SharingPreferences(
    val shareWorkoutHistory: Boolean = true,
    val shareBodyMeasurements: Boolean = false,
    val shareCheckinNotes: Boolean = false,
    val dataRetentionDays: Int = 365
)

data class SharingPreferencesResponse(
    val preferences: SharingPreferences
)
