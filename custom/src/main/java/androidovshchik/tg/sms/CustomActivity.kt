package androidovshchik.tg.sms

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidovshchik.tg.sms.ext.isMarshmallowPlus
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import org.jetbrains.anko.powerManager

class CustomActivity : AppCompatActivity() {

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transact {
            replace(android.R.id.content, SettingsFragment())
            commit()
        }
        if (isMarshmallowPlus()) {
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

    private inline fun transact(action: FragmentTransaction.() -> Unit) {
        supportFragmentManager.beginTransaction().apply(action)
    }
}
