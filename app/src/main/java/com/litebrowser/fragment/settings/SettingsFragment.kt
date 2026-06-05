package com.litebrowser.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.litebrowser.R
import com.litebrowser.adapter.settings.BrowserSettingsAdapter
import com.litebrowser.data.SharedPreferencesAccess
import com.litebrowser.databinding.SettingsFragmentBinding
import com.litebrowser.extensions.Extensions.createSettingsList

class SettingsFragment : DialogFragment() {

    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!

    var adapter: BrowserSettingsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        requireDialog().window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, SharedPreferencesAccess.loadTheme(requireContext()))
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        requireDialog().window?.setWindowAnimations(
            R.style.DialogAnimation
        )
        binding.close.setOnClickListener { dismiss() }
        adapter = BrowserSettingsAdapter(requireContext(), requireContext().createSettingsList())
        binding.settingsRecycler.adapter = adapter

    }

}