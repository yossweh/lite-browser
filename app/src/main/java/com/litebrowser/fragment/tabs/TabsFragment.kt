package com.litebrowser.fragment.tabs

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.litebrowser.R
import com.litebrowser.activity.BrowserActivity
import com.litebrowser.activity.MainActivity
import com.litebrowser.adapter.tabs.TabAdapter
import com.litebrowser.data.BrowserTabItem
import com.litebrowser.data.BrowserTabs.createNewTab
import com.litebrowser.data.BrowserTabs.loadTab
import com.litebrowser.data.BrowserTabs.openedTabs
import com.litebrowser.data.BrowserTabs.saveLastTab
import com.litebrowser.data.BrowserTabs.updateTabs
import com.litebrowser.data.SharedPreferencesAccess
import com.litebrowser.databinding.TabsFragmentBinding

class TabsFragment : DialogFragment() {

    private var _binding: TabsFragmentBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TabsFragmentBinding.inflate(inflater, container, false)
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

    private var needToLoad = false
    private var position = 0

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        requireDialog().window?.setWindowAnimations(
            R.style.DialogAnimation
        )
        val activity = requireActivity()
        requireDialog().setOnDismissListener {
            if (openedTabs.isEmpty()) (activity as? BrowserActivity)?.finish()
            else if (needToLoad) (activity as? BrowserActivity)?.loadTab(position, false)
            activity.updateTabs()
            this.dismiss()
        }
        (activity as? BrowserActivity)?.saveLastTab()

        binding.close.setOnClickListener { requireDialog().dismiss() }

        binding.clear.setOnClickListener {
            if (openedTabs.isNotEmpty()) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.clearTabs)
                    .setMessage(R.string.clearTabsMessage)
                    .setPositiveButton(R.string.ok_ok) { _, _ ->
                        (activity as? BrowserActivity)?.finish()
                        requireDialog().dismiss()
                        openedTabs.clear()
                        requireContext().updateTabs()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            } else {
                Toast.makeText(activity.applicationContext, R.string.noTabs, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.addTab.setOnClickListener {
            requireDialog().dismiss()
            if (activity is MainActivity) {
                val intent = Intent(activity, BrowserActivity::class.java)
                intent.putExtra("url", "https://google.com")
                requireContext().startActivity(intent)
            } else (activity as? BrowserActivity)?.createNewTab()
        }

        binding.label.apply {
            text =
                if (openedTabs.isNotEmpty()) "${getString(R.string.tabsOpened)} ${openedTabs.size}"
                else getString(R.string.tabs)
        }

        binding.tabRecycler.apply {
            val list: ArrayList<BrowserTabItem> = ArrayList()
            list.addAll(openedTabs)
            when (list.isNotEmpty()) {
                true -> adapter = TabAdapter(requireContext(), list, this@TabsFragment)
                else -> binding.errorMessage.visibility = VISIBLE
            }

        }
    }

    fun notifyPosition(position: Int) {
        needToLoad = true
        this.position = position
    }

}
