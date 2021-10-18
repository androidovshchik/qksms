package androidovshchik.tg.sms

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        val preferences = Preferences(requireContext())
        val authCode = preferenceManager.findPreference<EditTextPreference>("auth_code")
        authCode?.text = preferences.authCode
    }
}
