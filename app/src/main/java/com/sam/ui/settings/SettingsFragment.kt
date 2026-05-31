package com.sam.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.sam.R
import com.sam.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.themeToggleGroup.check(R.id.btnThemeLight)
            AppCompatDelegate.MODE_NIGHT_YES -> binding.themeToggleGroup.check(R.id.btnThemeDark)
            else -> binding.themeToggleGroup.check(R.id.btnThemeAuto)
        }

        binding.themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btnThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.btnThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                
                prefs.edit().putInt("theme_mode", mode).apply()
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
