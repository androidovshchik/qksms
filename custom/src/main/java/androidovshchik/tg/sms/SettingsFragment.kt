package androidovshchik.tg.sms

import android.os.Bundle
import androidovshchik.tg.sms.local.Preferences
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        val preferences = Preferences(requireContext())
        val authCode = preferenceManager.findPreference<EditTextPreference>("auth_code")
        authCode?.text = preferences.authCode
        authCode?.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString().trim()
            if (value.isNotBlank() && preferences.authCode != value) {
                val context = requireContext()
                preferences.authCode = value
                authCode.text = value
                doAsync {
                    db.chatDao().deleteAll()
                    SendWorker.cancel(context)
                }
            }
            false
        }
        val updInbox = preferenceManager.findPreference<Preference>("update_inbox")
        updInbox?.setOnPreferenceClickListener {
            val context = requireContext()
            context.toast("Подождите...")
            UpdateWorker.launch(requireContext())
            true
        }
    }
}
