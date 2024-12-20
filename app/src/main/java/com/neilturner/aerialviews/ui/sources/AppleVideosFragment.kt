package com.neilturner.aerialviews.ui.sources

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.neilturner.aerialviews.R
import com.neilturner.aerialviews.utils.MenuStateFragment

class AppleVideosFragment : MenuStateFragment() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.sources_apple_videos, rootKey)
        updateSummary()
    }

    private fun updateSummary() {
        val quality = findPreference<ListPreference>("apple_videos_quality")
        quality?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                updateDataUsageSummary(quality.findIndexOfValue(newValue as String))
                true
            }
        quality?.findIndexOfValue(quality.value)?.let { updateDataUsageSummary(it) }
    }

    private fun updateDataUsageSummary(index: Int) {
        val res = context?.resources ?: return
        val dataUsage = findPreference<Preference>("apple_videos_data_usage") ?: return
        val bitrateList = res.getStringArray(R.array.apple_videos_data_usage_values)
        val bitrate = bitrateList[index]
        dataUsage.summary = String.format(res.getString(R.string.apple_videos_data_estimate_summary), bitrate)
    }
}
