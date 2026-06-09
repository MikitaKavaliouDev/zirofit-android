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

data class AppleAuthResult(
    val idToken: String,
    val authCode: String,
    val email: String?
)

sealed class AppleAuthOutcome {
    data class Success(val result: AppleAuthResult) : AppleAuthOutcome()
    object Cancelled : AppleAuthOutcome()
    data class Error(val message: String) : AppleAuthOutcome()
}

@Singleton
class AppleSignInManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        private val OAUTH_ENDPOINT = "${BuildConfig.BASE_URL.trimEnd('/')}/api/auth/mobile-signin?provider=apple"
        private const val CALLBACK_SCHEME = "zirofit"
        private const val CALLBACK_HOST = "apple-callback"
    }

    private val _authOutcome = MutableSharedFlow<AppleAuthOutcome>(replay = 1)
    val authOutcome: SharedFlow<AppleAuthOutcome> = _authOutcome.asSharedFlow()

    private var customTabsSession: CustomTabsSession? = null
    private var callbackInvoked = false

    fun signIn() {
        callbackInvoked = false

        val packageName = CustomTabsClient.getPackageName(appContext, null)
        if (packageName == null) {
            launchInBrowser()
            return
        }

        val bound = CustomTabsClient.bindCustomTabsService(
            appContext,
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
                                    ContextCompat.getColor(appContext, android.R.color.white)
                                )
                                .build()
                        )
                        .build()

                    try {
                        customTabsIntent.launchUrl(appContext, Uri.parse(OAUTH_ENDPOINT))
                    } catch (_: Exception) {
                        launchInBrowser()
                    }
                }

                override fun onServiceDisconnected(name: android.content.ComponentName?) {
                    if (!callbackInvoked) {
                        _authOutcome.tryEmit(AppleAuthOutcome.Cancelled)
                        customTabsSession = null
                    }
                }
            }
        )

        if (!bound) {
            launchInBrowser()
        }
    }

    private fun launchInBrowser() {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(OAUTH_ENDPOINT))
            appContext.startActivity(browserIntent)
        } catch (_: Exception) {
            _authOutcome.tryEmit(
                AppleAuthOutcome.Error("No browser available to complete sign-in")
            )
        }
    }

    fun handleCallbackUrl(url: String) {
        callbackInvoked = true
        val params = parseAuthParamsFromUrl(url)
        if (params.isEmpty()) {
            customTabsSession = null
            return
        }
        
        val idToken = params["id_token"]
        val authCode = params["code"]
        
        if (idToken != null && authCode != null) {
            val result = AppleAuthResult(
                idToken = idToken,
                authCode = authCode,
                email = params["email"]
            )
            _authOutcome.tryEmit(AppleAuthOutcome.Success(result))
        } else {
            _authOutcome.tryEmit(AppleAuthOutcome.Error("Incomplete auth data in callback: missing id_token or code"))
        }
        customTabsSession = null
    }

    fun parseAuthParamsFromUrl(url: String): Map<String, String?> {
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
