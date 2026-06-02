package com.ziro.fit.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.ziro.fit.model.ProfileCoreInfo
import com.ziro.fit.util.Logger

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    
    // Core Info fields
    var savedName: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()
    
  var savedLocation: String?
    get() {
        val value = prefs.getString(KEY_LOCATION, null)
        Logger.d("state", "getSavedLocation: $value")
        return value
    }
    set(value) {
        Logger.d("state", "setSavedLocation: $value")
        prefs.edit().putString(KEY_LOCATION, value).apply()
    }

    var savedBio: String?
        get() = prefs.getString(KEY_BIO, null)
        set(value) = prefs.edit().putString(KEY_BIO, value).apply()
    
    var savedRole: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()
    
        var savedAvatarUri: String?
    get() = prefs.getString(KEY_AVATAR_URI, null)
    set(value) = prefs.edit().putString(KEY_AVATAR_URI, value).apply()

    fun saveCoreInfo(coreInfo: ProfileCoreInfo) {
        Logger.d("state", "saveCoreInfo called - coreInfo: $coreInfo")
        val locationAddress = coreInfo.locations.firstOrNull()?.address
        prefs.edit().apply {
            putString(KEY_NAME, coreInfo.fullName)
            putString(KEY_LOCATION, locationAddress)
            apply()
        }
        Logger.d("state", "saveCoreInfo - saved name='${coreInfo.fullName}', location='$locationAddress'")
    }
    
    fun clearSession() {
        Logger.d("state", "clearSession called - clearing ALL prefs")
        prefs.edit().clear().apply()
    }
    
    companion object {
        private const val KEY_NAME = "onboarding_name"
        private const val KEY_LOCATION = "onboarding_location"
        private const val KEY_BIO = "onboarding_bio"
        private const val KEY_ROLE = "onboarding_role"
        private const val KEY_AVATAR_URI = "onboarding_avatar_uri"
    }
}
