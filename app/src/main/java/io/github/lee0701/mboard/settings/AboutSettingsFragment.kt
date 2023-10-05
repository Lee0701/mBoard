package io.github.lee0701.mboard.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.github.lee0701.mboard.R

class AboutSettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_about, rootKey)
    }
}