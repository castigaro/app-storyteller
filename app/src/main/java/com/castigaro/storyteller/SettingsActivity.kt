package com.castigaro.storyteller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.castigaro.common.llm.ProviderSettingsController
import com.castigaro.storyteller.databinding.ActivitySettingsBinding

/**
 * Einstellungen rund um die KI-Anbindung. Die eigentliche Oberfläche und
 * Logik kommen aus der gemeinsamen Bibliothek (provider_settings.xml +
 * ProviderSettingsController); gespeichert wird app-privat.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ProviderSettingsController(this, binding.providerSettings)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
