package com.ziro.fit.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.ziro.fit.BuildConfig

data class GoogleAuthResult(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val role: String
)

sealed class GoogleAuthOutcome {
    data class Success(val result: GoogleAuthResult) : GoogleAuthOutcome()
    object Cancelled : GoogleAuthOutcome()
    data class Error(val message: String) : GoogleAuthOutcome()
}

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private val OAUTH_ENDPOINT = "${BuildConfig.BASE_URL.trimEnd('/')}/api/auth/mobile-signin?provider=google"
        private const val CALLBACK_SCHEME = "zirofitapp"
        private const val CALLBACK_HOST = "auth-callback"
    }

    private val _authOutcome = MutableSharedFlow<GoogleAuthOutcome>(replay = 1)
    val authOutcome: SharedFlow<GoogleAuthOutcome> = _authOutcome.asSharedFlow()

    private var customTabsSession: CustomTabsSession? = null
    private var callbackInvoked = false

    fun launchGoogleSignIn(context: Context) {
        callbackInvoked = false

        val packageName = CustomTabsClient.getPackageName(context, null)
        if (packageName == null) {
            launchInBrowser(context)
            return
        }

        val bound = CustomTabsClient.bindCustomTabsService(
            context,
            packageName,
            object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(
                    name: android.content.ComponentName,
                    client: CustomTabsClient
                ) {
                    client.warmup(0L)

                    val session = client.newSession(object : CustomTabsCallback() {
                        override fun onNavigationEvent(
                            navigationEvent: Int,
                            extras: android.os.Bundle?
                        ) {
                            if (navigationEvent == NAVIGATION_FINISHED && !callbackInvoked) {
                                val pendingUrl = extras?.getString(
                                    "androidx.browser.customtabs.extra.TITLE_URL"
                                ) ?: return
                                if (pendingUrl.startsWith("$CALLBACK_SCHEME://$CALLBACK_HOST")) {
                                    handleCallbackUrl(pendingUrl)
                                }
                            }
                        }
                    })

                    customTabsSession = session

                    val customTabsIntent = CustomTabsIntent.Builder(session)
                        .setShowTitle(true)
                        .setDefaultColorSchemeParams(
                            CustomTabColorSchemeParams.Builder()
                                .setToolbarColor(
                                    ContextCompat.getColor(context, android.R.color.white)
                                )
                                .build()
                        )
                        .build()

                    try {
                        customTabsIntent.launchUrl(context, Uri.parse(OAUTH_ENDPOINT))
                    } catch (_: Exception) {
                        launchInBrowser(context)
                    }
                }

                override fun onServiceDisconnected(name: android.content.ComponentName?) {
                    if (!callbackInvoked) {
                        _authOutcome.tryEmit(GoogleAuthOutcome.Cancelled)
                        customTabsSession = null
                    }
                }
            }
        )

        if (!bound) {
            launchInBrowser(context)
        }
    }

    private fun launchInBrowser(context: Context) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(OAUTH_ENDPOINT))
            context.startActivity(browserIntent)
        } catch (_: Exception) {
            _authOutcome.tryEmit(
                GoogleAuthOutcome.Error("No browser available to complete sign-in")
            )
        }
    }

    internal fun handleCallbackUrl(url: String) {
        callbackInvoked = true
        val params = parseAuthParamsFromUrl(url)
        if (params.isEmpty()) {
            customTabsSession = null
            return
        }
        val accessToken = params["access_token"]
        val userId = params["user_id"]
        val role = params["role"]
        if (accessToken != null && userId != null && role != null) {
            val result = GoogleAuthResult(
                accessToken = accessToken,
                refreshToken = params["refresh_token"] ?: "",
                userId = userId,
                role = role
            )
            _authOutcome.tryEmit(GoogleAuthOutcome.Success(result))
        } else {
            _authOutcome.tryEmit(GoogleAuthOutcome.Error("Incomplete auth data in callback: missing fields"))
        }
        customTabsSession = null
    }

    internal fun parseAuthParamsFromUrl(url: String): Map<String, String?> {
        if (!url.startsWith("$CALLBACK_SCHEME://$CALLBACK_HOST")) return emptyMap()
        val query = url.substringAfter("?", "")
        val params = mutableMapOf<String, String?>()
        for (pair in query.split("&")) {
            val kv = pair.split("=", limit = 2)
            if (kv.size == 2) {
                params[kv[0]] = kv[1].takeIf { it.isNotEmpty() }
            }
        }
        return params
    }

    fun processIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == CALLBACK_SCHEME && uri.host == CALLBACK_HOST) {
                handleCallbackUrl(uri.toString())
            }
        }
    }
}
