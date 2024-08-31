package com.neilturner.aerialviews.ui.settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.neilturner.aerialviews.R
import com.neilturner.aerialviews.utils.LoggingHelper

class AppearanceAnimationsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.settings_appearance_animations, rootKey)
        updateAllSummaries()
    }

    override fun onResume() {
        super.onResume()
        LoggingHelper.logScreenView("Animations", TAG)
    }

    private fun updateAllSummaries() {
        val editPref = findPreference<ListPreference>("overlay_auto_hide")
        editPref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                toggleRevealTimeout(newValue as String)
                true
            }
        toggleRevealTimeout(editPref?.value as String)
    }

    private fun toggleRevealTimeout(value: String) {
        val revealTimeoutPref = findPreference<ListPreference>("overlay_reveal_timeout")
        revealTimeoutPref?.isEnabled = value != "-1"
    }

    companion object {
        private const val TAG = "AnimationsFragment"
    }
}
