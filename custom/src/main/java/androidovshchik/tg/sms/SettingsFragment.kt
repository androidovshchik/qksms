package androidovshchik.tg.sms

import android.os.Bundle
import androidovshchik.tg.sms.local.Preferences
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import org.jetbrains.anko.doAsync

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        val preferences = Preferences(requireContext())
        val authCode = preferenceManager.findPreference<EditTextPreference>("auth_code")
        authCode?.text = preferences.authCode
        authCode?.setOnPreferenceChangeListener { _, newValue ->
            if (preferences.authCode != newValue) {
                preferences.authCode = newValue.toString().trim()
                doAsync {
                    db.chatDao().deleteAll()
                }
            }
            false
        }
    }
}
