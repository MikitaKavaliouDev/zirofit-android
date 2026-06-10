package com.ziro.fit.data.repository

import com.ziro.fit.data.remote.ZiroApi
import com.ziro.fit.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: ZiroApi
) {
    suspend fun getCoreInfo(): Result<ProfileCoreInfo> {
        return try {
            val response = api.getCoreInfo()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!.coreInfo)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch core info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBranding(): Result<ProfileBranding> {
        return try {
            val response = api.getBranding()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!.branding)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch branding"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBranding(bannerFile: java.io.File?, profileFile: java.io.File?): Result<Unit> {
        return try {
            val bannerPart = bannerFile?.let {
                val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.extension) ?: "image/jpeg"
                val requestBody = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), it)
                okhttp3.MultipartBody.Part.createFormData("bannerImage", it.name, requestBody)
            }
            val profilePart = profileFile?.let {
                val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.extension) ?: "image/jpeg"
                val requestBody = okhttp3.RequestBody.create(mimeType.toMediaTypeOrNull(), it)
                okhttp3.MultipartBody.Part.createFormData("profileImage", it.name, requestBody)
            }

            val response = api.updateBranding(bannerPart, profilePart)
            if (response.success != false) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update branding"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServices(): Result<ProfileServicesResponse> {
        return try {
            val response = api.getServices()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch services"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPackages(): Result<ProfilePackagesResponse> {
        return try {
            val response = api.getPackages()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch packages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailability(): Result<ProfileAvailability> {
        return try {
            val response = api.getAvailability()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!.availability)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch availability"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransformationPhotos(): Result<ProfileTransformationPhotosResponse> {
        return try {
            val response = api.getTransformationPhotos()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch transformation photos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTestimonials(): Result<ProfileTestimonialsResponse> {
        return try {
            val response = api.getTestimonials()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch testimonials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSocialLinks(): Result<ProfileSocialLinksResponse> {
        return try {
            val response = api.getSocialLinks()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch social links"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExternalLinks(): Result<ProfileExternalLinksResponse> {
        return try {
            val response = api.getExternalLinks()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch external links"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBilling(): Result<ProfileBilling> {
        return try {
            val response = api.getBilling()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!.billing)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch billing info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingWindowSettings(): Result<BookingWindowSettings> {
        return try {
            val response = api.getBookingWindowSettings()
            if ((response.success ?: true) && response.data != null) {
                val settings = response.data!!.bookingWindowSettings
                if (settings != null) {
                    Result.success(settings)
                } else {
                    Result.failure(Exception("Booking window settings not found"))
                }
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch booking window settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBookingWindowSettings(settings: BookingWindowSettings): Result<Unit> {
        return try {
            val response = api.updateBookingWindowSettings(settings)
            if (response.success != false) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update booking window settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBenefits(): Result<ProfileBenefitsResponse> {
        return try {
            val response = api.getBenefits()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch benefits"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(): Result<GetNotificationsResponse> {
        return try {
            val response = api.getNotifications()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch notifications"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerPushToken(token: String): Result<Unit> {
        return try {
            val response = api.registerPushToken(RegisterPushTokenRequest(token))
            if (response.success != false) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to register push token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchStripeOnboardingUrl(): Result<String> {
        return try {
            val response = api.fetchStripeOnboardingUrl()
            if ((response.success ?: true) && response.data != null) {
                Result.success(response.data!!.url)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch Stripe URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkingHours(workingHours: com.ziro.fit.model.WorkingHours): Result<Unit> {
        return try {
            val response = api.updateWorkingHours(workingHours)
            if (response.success != false) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update working hours"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val response = api.deleteAccount()
            if (response.success != false) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to delete account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getRevenue(): Flow<Result<RevenueResponse>> = flow {
        try {
            val response = api.getRevenue()
            if ((response.success ?: true) && response.data != null) {
                emit(Result.success(response.data!!))
            } else {
                emit(Result.failure(Exception(response.message ?: "Failed to fetch revenue")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
