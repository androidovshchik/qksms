package androidovshchik.tg.sms

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceActivity
import android.provider.Settings
import androidovshchik.tg.sms.ext.getComponent
import androidovshchik.tg.sms.ext.isRPlus
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentTransaction
import org.jetbrains.anko.powerManager

class CustomActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transact {
            replace(android.R.id.content, SettingsFragment())
            commit()
        }
    }

    @SuppressLint("BatteryLife")
    override fun onStart() {
        super.onStart()
        if (!hasNotificationPermission()) {
            val name = getComponent<NotificationService>().flattenToString()
            if (isRPlus()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, name)
                })
            } else {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    putExtra(EXTRA_FRAGMENT_ARG_KEY, name)
                    putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, Bundle().also {
                        it.putString(EXTRA_FRAGMENT_ARG_KEY, name)
                    })
                })
            }
        } else {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        val listeners = NotificationManagerCompat.getEnabledListenerPackages(applicationContext)
        return listeners.contains(packageName)
    }

    private inline fun transact(action: FragmentTransaction.() -> Unit) {
        supportFragmentManager.beginTransaction().apply(action)
    }

    companion object {

        // see https://android.googlesource.com/platform/packages/apps/Settings/+/master/src/com/android/settings/SettingsActivity.java
        private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    }
}
