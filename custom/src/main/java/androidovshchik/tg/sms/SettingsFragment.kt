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
            val value = newValue.toString().trim()
            if (value.isNotBlank() && preferences.authCode != value) {
                preferences.authCode = value
                authCode.text = value
                doAsync {
                    db.chatDao().deleteAll()
                    MainWorker.cancel(requireContext())
                }
            }
            false
        }
    }
}
