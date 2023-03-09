package com.avidco.studentintellect.activities.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.avidco.studentintellect.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}