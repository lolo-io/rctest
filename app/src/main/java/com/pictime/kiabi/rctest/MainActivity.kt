package com.pictime.kiabi.rctest

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Minimal isolation test for Firebase Remote Config Realtime against project kiabi-17f3b.
 *
 * No app plumbing (no custom OkHttp, no DataDome, no interceptors, no app-level proxy):
 * just the Firebase SDK. Everything is logged on screen AND to logcat (tag RCTEST).
 *
 * Steps:
 *  1. Launch the app, keep it in the foreground.
 *  2. Wait for "Listener registered".
 *  3. In the Firebase console, change any Remote Config parameter and "Publish".
 *  4. Observe: "onUpdate" (realtime works), "onError" (with the code), or nothing (bug reproduced).
 */
class MainActivity : AppCompatActivity() {

    private val tag = "RCTEST"
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logView = TextView(this).apply {
            setPadding(28, 28, 28, 28)
            textSize = 12f
            setTextIsSelectable(true)
            setTextColor(Color.parseColor("#111111"))
        }
        val scrollView = ScrollView(this).apply { addView(logView) }
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            logView.setPadding(28, 28 + statusBarInsets.top, 28, 28)
            insets
        }
        setContentView(scrollView)

        log("=== RC Realtime test ===")

        val rc = FirebaseRemoteConfig.getInstance()

        // Proves Firebase Installations (same auth path as realtime) works.
        FirebaseInstallations.getInstance().id
            .addOnSuccessListener { log("Firebase Installation ID = $it") }
            .addOnFailureListener { log("FID FAILED: ${it.javaClass.simpleName} ${it.message}") }

        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // debug
            .build()
        rc.setConfigSettingsAsync(settings)
            .addOnCompleteListener { settingsTask ->
                log("setConfigSettingsAsync done (success=${settingsTask.isSuccessful})")

                // Initial fetch (the "classic" path, different host from realtime).
                rc.fetchAndActivate()
                    .addOnCompleteListener { fetchTask ->
                        if (fetchTask.isSuccessful) {
                            log("fetchAndActivate OK (updated=${fetchTask.result}) keys=${rc.all.keys}")
                        } else {
                            log("fetchAndActivate FAILED: ${fetchTask.exception?.javaClass?.simpleName} ${fetchTask.exception?.message}")
                        }

                        log("Registering realtime listener...")
                        val registration = rc.addOnConfigUpdateListener(object : ConfigUpdateListener {
                            override fun onUpdate(configUpdate: ConfigUpdate) {
                                log("onUpdate — updatedKeys=${configUpdate.updatedKeys}")
                                rc.activate().addOnCompleteListener { log("   activate() success=${it.isSuccessful}") }
                            }

                            override fun onError(error: FirebaseRemoteConfigException) {
                                log("onError — code=${error.code} : ${error.message}")
                            }
                        })
                        log("Listener registered: $registration")
                        log("Setup complete. >>> Publish a change in the Firebase console and wait here. <<<")
                    }
            }
    }

    private fun log(msg: String) {
        Log.i(tag, msg)
        val line = "${clock.format(Date())}  $msg\n\n"
        runOnUiThread { logView.append(line) }
    }
}
