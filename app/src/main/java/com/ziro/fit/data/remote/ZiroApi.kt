package com.ziro.fit.data.remote

import com.ziro.fit.model.ApiResponse
import com.ziro.fit.model.CalendarResponse
import com.ziro.fit.model.GetExercisesResponse
import com.ziro.fit.model.GetClientsResponse
import com.ziro.fit.model.CalendarSummaryResponse
import com.ziro.fit.model.GetClientDetailsResponse
import com.ziro.fit.model.GetMeasurementsResponse
import com.ziro.fit.model.GetAssessmentsResponse
import com.ziro.fit.model.GetPhotosResponse
import com.ziro.fit.model.GetClientSessionsResponse
import com.ziro.fit.model.LogSetRequest
import com.ziro.fit.model.LoginRequest
import com.ziro.fit.model.LoginResponse
import com.ziro.fit.model.RefreshTokenRequest
import com.ziro.fit.model.RefreshTokenResponse
import com.ziro.fit.model.ServerLiveSessionResponse
import com.ziro.fit.model.StartWorkoutRequest
import com.ziro.fit.model.StartWorkoutResponse
import com.ziro.fit.model.GetActiveSessionResponse
import com.ziro.fit.model.User
import com.ziro.fit.model.CreateClientRequest
import com.ziro.fit.model.CreateClientResponse
import com.ziro.fit.model.UpdateClientRequest
import com.ziro.fit.model.CreateMeasurementRequest
import com.ziro.fit.model.CreateMeasurementResponse
import com.ziro.fit.model.CreateAssessmentRequest
import com.ziro.fit.model.CreateAssessmentResponse
import com.ziro.fit.model.UploadPhotoResponse
import com.ziro.fit.model.UpdateSessionRequest
import com.ziro.fit.model.ClientSession
import com.ziro.fit.model.CreateSessionRequest
import com.ziro.fit.model.CreateSessionResponse
import com.ziro.fit.model.CreateExerciseRequest
import com.ziro.fit.model.CreateExerciseResponse
import com.ziro.fit.model.*
import com.ziro.fit.data.model.CheckInContext
import com.ziro.fit.data.model.CheckInDetailWrapper
import com.ziro.fit.data.model.CheckInPendingItem
import com.ziro.fit.data.model.ReviewCheckInRequest
import com.ziro.fit.data.model.CheckInConfig
import com.ziro.fit.data.model.CheckInSubmissionRequest
import com.ziro.fit.data.model.CheckInHistoryItem
import com.ziro.fit.model.BlogListResponse
import com.ziro.fit.model.BlogPost
import com.ziro.fit.model.AdminBlogListResponse
import com.ziro.fit.model.AdminBlogPost
import com.ziro.fit.model.CreateBlogPostRequest
import com.ziro.fit.model.UpdateBlogPostRequest
import com.ziro.fit.model.ForgotPasswordRequest
import com.ziro.fit.model.ForgotPasswordResponse
import com.ziro.fit.model.UpdatePasswordRequest
import com.ziro.fit.model.UpdatePasswordResponse
import com.ziro.fit.model.BillingPortalResponse
import com.ziro.fit.model.AdminEventsResponse
import com.ziro.fit.model.EventModerationDetailResponse
import com.ziro.fit.model.EventModerationActionRequest
import com.ziro.fit.model.EventModerationUpdateResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.*

interface ZiroApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<RegisterResponse>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ApiResponse<ForgotPasswordResponse>

    @POST("api/auth/update-password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): ApiResponse<UpdatePasswordResponse>

    @POST("api/auth/apple")
    suspend fun appleAuth(@Body request: com.ziro.fit.model.AppleAuthRequest): ApiResponse<com.ziro.fit.model.AppleAuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshAccessToken(@Body request: RefreshTokenRequest): ApiResponse<RefreshTokenResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): ApiResponse<RefreshTokenResponse>

    @Multipart
    @POST("api/onboarding/complete")
    suspend fun completeOnboarding(
        @Part("role") role: okhttp3.RequestBody,
        @Part("name") name: okhttp3.RequestBody,
        @Part("location") location: okhttp3.RequestBody?,
        @Part("bio") bio: okhttp3.RequestBody?,
        @Part avatar: okhttp3.MultipartBody.Part?
    ): ApiResponse<User>

    @GET("api/auth/me")
    suspend fun getMe(): ApiResponse<User>

    @POST("api/profile/me/push-token")
    suspend fun registerPushToken(@Body request: RegisterPushTokenRequest): ApiResponse<Any>

    @GET("api/trainer/calendar")
    suspend fun getCalendarEvents(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): ApiResponse<CalendarResponse>

    @GET("api/trainer/calendar/clients-summary")
    suspend fun getCalendarClientsSummary(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): ApiResponse<CalendarSummaryResponse>

    @POST("api/trainer/calendar")
    suspend fun createCalendarSession(@Body request: CreateSessionRequest): ApiResponse<CreateSessionResponse>

    @GET("api/workout-sessions/live")
    suspend fun getActiveSession(): ApiResponse<GetActiveSessionResponse>

    @POST("api/workout-sessions/start")
    suspend fun startWorkout(@Body request: StartWorkoutRequest): ApiResponse<StartWorkoutResponse>

    @POST("api/workout-templates")
    suspend fun createWorkoutTemplate(@Body request: CreateWorkoutTemplateRequest): ApiResponse<CreateWorkoutTemplateResponse>

    // New Endpoint to fetch template details
    @GET("api/workout-templates/{id}")
    suspend fun getWorkoutTemplate(@retrofit2.http.Path("id") id: String): ApiResponse<ServerTemplate>

    @retrofit2.http.PUT("api/workout-templates/{id}")
    suspend fun updateWorkoutTemplate(
        @retrofit2.http.Path("id") id: String,
        @Body request: CreateWorkoutTemplateRequest
    ): ApiResponse<CreateWorkoutTemplateResponse>

    @retrofit2.http.DELETE("api/workout-templates/{id}")
    suspend fun deleteWorkoutTemplate(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    @POST("api/workout/log")
    suspend fun logSet(@Body request: LogSetRequest): ApiResponse<Any>

    @POST("api/workout-sessions/finish")
    suspend fun finishWorkout(@Body request: FinishWorkoutRequest): ApiResponse<FinishWorkoutResponse>

    @POST("api/workout-sessions/{id}/cancel")
    suspend fun cancelActiveWorkout(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    @retrofit2.http.PUT("api/workout-sessions/{id}")
    suspend fun updateWorkoutSession(
        @retrofit2.http.Path("id") id: String,
        @Body request: UpdateSessionRequest
    ): ApiResponse<Any>
    
    @retrofit2.http.PUT("api/trainer/availability")
    suspend fun updateWorkingHours(@Body request: com.ziro.fit.model.WorkingHours): ApiResponse<Any>
    @GET("api/exercises")
    suspend fun getExercises(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1
    ): ApiResponse<GetExercisesResponse>

    @POST("api/exercises")
    suspend fun createExercise(@Body request: CreateExerciseRequest): ApiResponse<CreateExerciseResponse>

    @retrofit2.http.PUT("api/exercises/{id}")
    suspend fun updateExercise(
        @retrofit2.http.Path("id") id: String,
        @Body request: CreateExerciseRequest
    ): ApiResponse<Any>

    @retrofit2.http.DELETE("api/exercises/{id}")
    suspend fun deleteExercise(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    @GET("api/exercises/sync")
    suspend fun syncExercises(
        @Query("lastPulledAt") lastPulledAt: Long?
    ): ApiResponse<SyncExercisesResponse>


    @POST("api/clients")
    suspend fun createClient(@Body request: CreateClientRequest): ApiResponse<CreateClientResponse>

    @GET("api/clients")
    suspend fun getClients(): ApiResponse<GetClientsResponse>

    @GET("api/clients/{id}")
    suspend fun getClientDetails(@retrofit2.http.Path("id") id: String): ApiResponse<GetClientDetailsResponse>

    @retrofit2.http.PUT("api/clients/{id}")
    suspend fun updateClient(@retrofit2.http.Path("id") id: String, @Body request: UpdateClientRequest): ApiResponse<Any>

    @GET("api/clients/{id}/measurements")
    suspend fun getClientMeasurements(@retrofit2.http.Path("id") id: String): ApiResponse<GetMeasurementsResponse>

    @POST("api/clients/{id}/measurements")
    suspend fun createMeasurement(@retrofit2.http.Path("id") id: String, @Body request: CreateMeasurementRequest): ApiResponse<CreateMeasurementResponse>

    @retrofit2.http.DELETE("api/clients/{id}/measurements/{measurementId}")
    suspend fun deleteMeasurement(@retrofit2.http.Path("id") id: String, @retrofit2.http.Path("measurementId") measurementId: String): ApiResponse<Any>

    @GET("api/clients/{id}/assessments")
    suspend fun getClientAssessments(@retrofit2.http.Path("id") id: String): ApiResponse<GetClientAssessmentsResponse>

    @POST("api/clients/{id}/assessments")
    suspend fun createAssessment(@retrofit2.http.Path("id") id: String, @Body request: CreateClientAssessmentRequest): ApiResponse<CreateClientAssessmentResponse>

    @retrofit2.http.DELETE("api/clients/{id}/assessments/{resultId}")
    suspend fun deleteAssessment(@retrofit2.http.Path("id") id: String, @retrofit2.http.Path("resultId") resultId: String): ApiResponse<Any>

    @GET("api/clients/{id}/photos")
    suspend fun getClientPhotos(@retrofit2.http.Path("id") id: String): ApiResponse<GetPhotosResponse>

    @retrofit2.http.Multipart
    @POST("api/clients/{id}/photos")
    suspend fun uploadPhoto(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Part photo: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("photoDate") photoDate: okhttp3.RequestBody,
        @retrofit2.http.Part("caption") caption: okhttp3.RequestBody?
    ): ApiResponse<UploadPhotoResponse>

    @retrofit2.http.DELETE("api/clients/{id}/photos/{photoId}")
    suspend fun deletePhoto(@retrofit2.http.Path("id") id: String, @retrofit2.http.Path("photoId") photoId: String): ApiResponse<Any>

    @GET("api/clients/{id}/sessions")
    suspend fun getClientSessions(@retrofit2.http.Path("id") id: String): ApiResponse<GetClientSessionsResponse>

    @retrofit2.http.PUT("api/clients/{id}/sessions/{sessionId}")
    suspend fun updateSession(
        @retrofit2.http.Path("id") id: String, 
        @retrofit2.http.Path("sessionId") sessionId: String,
        @Body request: UpdateSessionRequest
    ): ApiResponse<ClientSession>

    @retrofit2.http.DELETE("api/clients/{id}/sessions/{sessionId}")
    suspend fun deleteSession(@retrofit2.http.Path("id") id: String, @retrofit2.http.Path("sessionId") sessionId: String): ApiResponse<Any>

    @retrofit2.http.DELETE("api/clients/{id}")
    suspend fun deleteClient(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    // Profile Endpoints
    @GET("api/profile/me/core-info")
    suspend fun getCoreInfo(): ApiResponse<ProfileCoreInfoResponse>

    @retrofit2.http.PUT("api/profile/me/core-info")
    suspend fun updateCoreInfo(@Body request: UpdateCoreInfoRequest): ApiResponse<Any>

    @GET("api/client/sharing")
    suspend fun getSharingPreferences(): ApiResponse<SharingPreferencesResponse>

    @PUT("api/client/sharing")
    suspend fun updateSharingPreferences(@Body request: SharingPreferences): ApiResponse<Any>

    @GET("api/profile/me/branding")
    suspend fun getBranding(): ApiResponse<ProfileBrandingResponse>

    @retrofit2.http.Multipart
    @POST("api/profile/me/branding")
    suspend fun updateBranding(
        @retrofit2.http.Part bannerImage: okhttp3.MultipartBody.Part?,
        @retrofit2.http.Part profileImage: okhttp3.MultipartBody.Part?
    ): ApiResponse<Any>

    @GET("api/profile/me/services")
    suspend fun getServices(): ApiResponse<ProfileServicesResponse>

    @GET("api/profile/me/packages")
    suspend fun getPackages(): ApiResponse<ProfilePackagesResponse>

    @GET("api/profile/me/availability")
    suspend fun getAvailability(): ApiResponse<ProfileAvailabilityResponse>

    @GET("api/profile/me/transformation-photos")
    suspend fun getTransformationPhotos(): ApiResponse<ProfileTransformationPhotosResponse>

    @GET("api/profile/me/testimonials")
    suspend fun getTestimonials(): ApiResponse<ProfileTestimonialsResponse>

    @GET("api/profile/me/social-links")
    suspend fun getSocialLinks(): ApiResponse<ProfileSocialLinksResponse>

    @GET("api/profile/me/external-links")
    suspend fun getExternalLinks(): ApiResponse<ProfileExternalLinksResponse>

    @GET("api/profile/me/billing")
    suspend fun getBilling(): ApiResponse<ProfileBillingResponse>

    @POST("api/profile/me/billing")
    suspend fun fetchStripeOnboardingUrl(): ApiResponse<StripeOnboardingResponse>

    @GET("api/profile/me/benefits")
    suspend fun getBenefits(): ApiResponse<ProfileBenefitsResponse>

    @GET("api/profile/me/revenue")
    suspend fun getRevenue(): ApiResponse<RevenueResponse>

    @GET("api/notifications")
    suspend fun getNotifications(): ApiResponse<GetNotificationsResponse>

    @GET("api/trainer/booking-window")
    suspend fun getBookingWindowSettings(): ApiResponse<BookingWindowSettingsResponse>

    @PUT("api/trainer/booking-window")
    suspend fun updateBookingWindowSettings(@Body request: BookingWindowSettings): ApiResponse<Any>

    // Generic Assessments Management - Removed ApiResponse wrapper
    @GET("api/trainer/assessments")
    suspend fun getAssessments(): GetAssessmentsResponse

    @POST("api/trainer/assessments")
    suspend fun createAssessment(@Body request: com.ziro.fit.model.CreateAssessmentRequest): CreateAssessmentResponse

    @retrofit2.http.PUT("api/trainer/assessments/{id}")
    suspend fun updateAssessment(@retrofit2.http.Path("id") id: String, @Body request: com.ziro.fit.model.UpdateAssessmentRequest): ApiResponse<Assessment>

    @retrofit2.http.DELETE("api/trainer/assessments/{id}")
    suspend fun deleteAssessment(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    // Bookings
    @GET("api/bookings")
    suspend fun getBookings(): ApiResponse<List<Booking>>

    @POST("api/bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): ApiResponse<CreateBookingResponse>

    @retrofit2.http.PUT("api/bookings/{id}")
    suspend fun updateBooking(
        @retrofit2.http.Path("id") id: String,
        @Body request: UpdateBookingRequest
    ): ApiResponse<BookingResponse>

    @retrofit2.http.PUT("api/bookings/{id}/confirm")
    suspend fun confirmBooking(
        @retrofit2.http.Path("id") id: String,
        @Body request: ConfirmBookingRequest
    ): ApiResponse<BookingResponse>

    @retrofit2.http.PUT("api/bookings/{id}/decline")
    suspend fun declineBooking(
        @retrofit2.http.Path("id") id: String
    ): ApiResponse<BookingResponse>

    @retrofit2.http.DELETE("api/bookings/{id}")
    suspend fun deleteBooking(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    // Check-Ins
    @GET("api/trainer/check-ins/pending")
    suspend fun getPendingCheckIns(): List<CheckInPendingItem>

    @GET("api/trainer/check-ins/{id}")
    suspend fun getCheckInDetails(@retrofit2.http.Path("id") id: String): ApiResponse<CheckInContext>

    @retrofit2.http.PATCH("api/trainer/check-ins/{id}/review")
    suspend fun reviewCheckIn(
        @retrofit2.http.Path("id") id: String,
        @Body request: ReviewCheckInRequest
    ): ApiResponse<Any>
    @GET("api/client/dashboard")
    suspend fun getClientDashboard(): ApiResponse<ClientDashboardResponse>

    // Trainer Discovery
    @GET("api/trainers")
    suspend fun getTrainers(
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("location") location: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("minRating") minRating: Double? = null,
        @Query("specialties") specialties: String? = null,
        @Query("trainingTypes") trainingTypes: String? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null
    ): ApiResponse<GetTrainersResponse>

    @GET("api/trainers/{id}/public")
    suspend fun getPublicTrainerProfile(@retrofit2.http.Path("id") id: String): ApiResponse<PublicTrainerProfileResponse>

    @GET("api/trainers/{username}/schedule")
    suspend fun getTrainerSchedule(@retrofit2.http.Path("username") username: String): ApiResponse<TrainerScheduleResponse>

    @POST("api/client/trainer/link")
    suspend fun linkTrainer(@Body request: LinkTrainerRequest): ApiResponse<LinkActionResponse>

    @retrofit2.http.DELETE("api/client/trainer/link")
    suspend fun unlinkTrainer(): ApiResponse<LinkActionResponse>

    @GET("api/client/trainer")
    suspend fun getLinkedTrainer(): ApiResponse<LinkedTrainerResponse>

    @GET("api/workout-sessions/history")
    suspend fun getWorkoutHistory(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null,
        @Query("clientId") clientId: String? = null
    ): ApiResponse<WorkoutHistoryResponse>

    @GET("api/client/progress")
    suspend fun getClientProgress(): ApiResponse<ClientProgressResponse>

    // Client Check-ins
    @GET("api/client/check-in/config")
    suspend fun getCheckInConfig(): ApiResponse<CheckInConfig>

    @POST("api/client/check-in")
    suspend fun submitCheckIn(@Body request: CheckInSubmissionRequest): ApiResponse<Any>

    @GET("api/client/check-ins")
    suspend fun getClientCheckInHistory(): ApiResponse<List<CheckInHistoryItem>>

    @GET("api/client/check-ins/{id}")
    suspend fun getClientCheckInDetails(@retrofit2.http.Path("id") id: String): ApiResponse<CheckInDetailWrapper>

    @GET("api/client/programs")
    suspend fun getClientPrograms(): List<ClientProgramAssignmentDto>

    @GET("api/trainer/workout-templates")
    suspend fun getWorkoutTemplates(): ApiResponse<WorkoutTemplatesResponse>

    @GET("api/client/program/active")
    suspend fun getActiveProgramProgress(): ApiResponse<ActiveProgramApiResponse>

    // Chat
    @GET("api/chat")
    suspend fun getChatHistory(
        @Query("clientId") clientId: String,
        @Query("trainerId") trainerId: String
    ): ApiResponse<StartChatResponse>

    @POST("api/chat")
    suspend fun sendMessage(@Body request: SendMessageRequest): ApiResponse<Any>

    @POST("api/client/ai/generate")
    suspend fun generateAiWorkout(@Body request: AiGenerationRequest): ApiResponse<WorkoutGenerationResponse>

    @POST("api/checkout/session")
    suspend fun createCheckoutSession(@Body request: CreateCheckoutSessionRequest): ApiResponse<CreateCheckoutSessionResponse>

    @POST("api/trainer/programs/ai-generate")
    suspend fun generateAiProgram(@Body request: GenerateProgramRequest): ApiResponse<ProgramResponse>

    @GET("api/clients/{id}/programs")
    suspend fun getTrainerClientPrograms(@retrofit2.http.Path("id") id: String): ApiResponse<GetClientProgramsResponse>

    // AI Coach
    @POST("api/mobile/ai-coach/refine")
    suspend fun refineGoal(@Body request: RefineGoalRequest): ApiResponse<RefineGoalResponse>

    @POST("api/mobile/ai-coach/generate")
    suspend fun generateProgramFromGoal(@Body request: GenerateProgramFromGoalRequest): ApiResponse<GenerateProgramFromGoalResponse>
    
    // Explore Events
    @GET("api/events")
    suspend fun getExploreEvents(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("cityId") cityId: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("long") long: Double? = null,
        @Query("category") categoryId: String? = null,
        @Query("search") search: String? = null,
        @Query("isFree") isFree: Boolean? = null,
        @Query("sortBy") sortBy: String? = null
    ): ApiResponse<ExploreEventsResponse>

    @GET("api/events/{id}")
    suspend fun getEventDetails(@retrofit2.http.Path("id") id: String): ApiResponse<EventDetailResponse>

    @POST("api/events/{id}/join")
    suspend fun joinFreeEvent(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    // Trainer Events CRUD
    @GET("api/trainer/events")
    suspend fun getTrainerEvents(): ApiResponse<TrainerEventsListResponse>

    @POST("api/trainer/events")
    suspend fun createEvent(@Body event: ExploreEvent): ApiResponse<EventCreateResponse>

    @PUT("api/trainer/events/{id}")
    suspend fun updateEvent(@retrofit2.http.Path("id") id: String, @Body event: ExploreEvent): ApiResponse<EventCreateResponse>

    @DELETE("api/trainer/events/{id}")
    suspend fun deleteEvent(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    // Explore Metadata and Featured
    @GET("api/explore/metadata")
    suspend fun getExploreMetadata(): ApiResponse<ExploreMetadataResponse>

    @GET("api/explore/featured")
    suspend fun getExploreFeatured(
        @Query("lat") lat: Double? = null,
        @Query("long") long: Double? = null,
        @Query("cityId") cityId: String? = null
    ): ApiResponse<ExploreFeaturedResponse>

    @GET("api/explore/promoted-trainers")
    suspend fun getPromotedTrainers(
        @Query("category") category: String? = null
    ): ApiResponse<PromotedTrainersResponse>

    @POST("api/notifications/subscribe")
    suspend fun subscribeToNotifications(
        @Body request: SubscribeNotificationsRequest
    ): ApiResponse<Any>

    // Analytics
    // Lightweight polling for ElevenLabs voice training session
    @GET("api/client/active-workout")
    suspend fun getActiveWorkoutStatus(): ApiResponse<ActiveWorkoutStatus>

    @GET("api/client/analytics")
    suspend fun getClientAnalytics(
        @Query("days") days: Int = 30
    ): ApiResponse<ClientAnalyticsResponse>

    // Daily Targets
    @GET("api/client/targets")
    suspend fun getDailyTargets(): ApiResponse<List<DailyTarget>>

    @POST("api/client/targets")
    suspend fun createDailyTarget(@Body request: CreateDailyTargetRequest): ApiResponse<DailyTarget>

    @retrofit2.http.PUT("api/client/targets/{id}")
    suspend fun updateDailyTarget(
        @retrofit2.http.Path("id") id: String,
        @Body request: UpdateDailyTargetRequest
    ): ApiResponse<DailyTarget>

    @retrofit2.http.DELETE("api/client/targets/{id}")
    suspend fun deleteDailyTarget(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    // Account deletion
    @HTTP(method = "DELETE", path = "api/user/delete", hasBody = false)
    suspend fun deleteAccount(): ApiResponse<Any>

    // Surgical Logout
    @POST("api/auth/signout")
    suspend fun signOut(@Body request: SignOutRequest): ApiResponse<Any>

    // Public Blog
    @GET("api/blog")
    suspend fun getBlogPosts(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): ApiResponse<BlogListResponse>

    @GET("api/blog/{slug}")
    suspend fun getBlogPost(@retrofit2.http.Path("slug") slug: String): ApiResponse<BlogPost>

    // Admin Blog
    @GET("api/admin/blog")
    suspend fun getAdminBlogPosts(): ApiResponse<AdminBlogListResponse>

    @POST("api/admin/blog")
    suspend fun createBlogPost(@Body request: CreateBlogPostRequest): ApiResponse<AdminBlogPost>

    @GET("api/admin/blog/{id}")
    suspend fun getAdminBlogPost(@retrofit2.http.Path("id") id: String): ApiResponse<AdminBlogPost>

    @PUT("api/admin/blog/{id}")
    suspend fun updateBlogPost(
        @retrofit2.http.Path("id") id: String,
        @Body request: UpdateBlogPostRequest
    ): ApiResponse<AdminBlogPost>

    @DELETE("api/admin/blog/{id}")
    suspend fun deleteBlogPost(@retrofit2.http.Path("id") id: String): ApiResponse<Any>

    // Billing
    @POST("api/billing/portal")
    suspend fun getBillingPortalUrl(): ApiResponse<BillingPortalResponse>

    @GET("api/billing/subscription")
    suspend fun getBillingSubscription(): ApiResponse<SubscriptionInfo>

    // Admin Events
    @GET("api/admin/events")
    suspend fun getPendingEvents(): ApiResponse<AdminEventsResponse>

    @GET("api/admin/events/{id}")
    suspend fun getEventForModeration(@retrofit2.http.Path("id") id: String): ApiResponse<EventModerationDetailResponse>

    @PATCH("api/admin/events/{id}")
    suspend fun moderateEvent(
        @retrofit2.http.Path("id") id: String,
        @Body request: EventModerationActionRequest
    ): ApiResponse<EventModerationUpdateResponse>
}
