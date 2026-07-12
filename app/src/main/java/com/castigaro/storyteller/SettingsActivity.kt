package com.castigaro.storyteller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.castigaro.storyteller.data.AppSettings
import com.castigaro.storyteller.databinding.ActivitySettingsBinding
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val provider = AppSettings.getProvider(this)
        if (provider == AppSettings.PROVIDER_OPENAI) {
            binding.radioOpenai.isChecked = true
        } else {
            binding.radioAnthropic.isChecked = true
        }
        binding.inputApiKey.setText(AppSettings.getApiKey(this))
        binding.inputModel.setText(AppSettings.getModel(this))

        // Beim Providerwechsel das jeweilige Standardmodell vorschlagen,
        // sofern der Nutzer kein eigenes eingetragen hat.
        binding.providerGroup.setOnCheckedChangeListener { _, checkedId ->
            val newProvider = if (checkedId == binding.radioOpenai.id) {
                AppSettings.PROVIDER_OPENAI
            } else {
                AppSettings.PROVIDER_ANTHROPIC
            }
            val current = binding.inputModel.text.toString().trim()
            if (current.isEmpty() ||
                current == AppSettings.DEFAULT_MODEL_ANTHROPIC ||
                current == AppSettings.DEFAULT_MODEL_OPENAI
            ) {
                binding.inputModel.setText(AppSettings.defaultModel(newProvider))
            }
        }

        binding.buttonSaveSettings.setOnClickListener {
            val newProvider = if (binding.radioOpenai.isChecked) {
                AppSettings.PROVIDER_OPENAI
            } else {
                AppSettings.PROVIDER_ANTHROPIC
            }
            AppSettings.save(
                this,
                provider = newProvider,
                apiKey = binding.inputApiKey.text.toString(),
                model = binding.inputModel.text.toString(),
            )
            Snackbar.make(binding.root, R.string.settings_saved, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
