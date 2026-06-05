package com.litebrowser.adapter.tabs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.luminance
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.litebrowser.R
import com.litebrowser.activity.BrowserActivity
import com.litebrowser.custom.view.BrowserView
import com.litebrowser.data.BrowserTabItem
import com.litebrowser.data.BrowserTabs.getCutSnap
import com.litebrowser.data.BrowserTabs.loadTab
import com.litebrowser.data.BrowserTabs.openedTabs
import com.litebrowser.databinding.TabItemBinding
import com.litebrowser.extensions.Extensions.darkenColor
import com.litebrowser.extensions.Extensions.dipToPixels
import com.litebrowser.extensions.Extensions.getAttrColor
import com.litebrowser.extensions.Extensions.lightenColor
import com.litebrowser.extensions.Extensions.setTint
import com.litebrowser.fragment.tabs.TabsFragment
import com.litebrowser.helper.utils.diff.TabDiffUtil
import java.net.URL


class TabAdapter(
    private val context: Context,
    private var adapterTabs: ArrayList<BrowserTabItem>,
    private val fragment: TabsFragment
) :
    RecyclerView.Adapter<TabAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            TabItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, wronPosition: Int) {
        val black = ContextCompat.getColor(
            context,
            R.color.black
        )
        val white = ContextCompat.getColor(
            context,
            R.color.white
        )
        var position = holder.layoutPosition

        (holder.itemView.layoutParams as RecyclerView.LayoutParams).apply {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                topMargin = if (position == 0) context.dipToPixels(6f).toInt()
                else context.dipToPixels(-14f).toInt()
            } else {
                marginStart = if (position == 0) context.dipToPixels(6f).toInt()
                else context.dipToPixels(-14f).toInt()
            }

            if (position == adapterTabs.lastIndex) {
                holder.shadow.visibility = GONE
            } else {
                holder.shadow.visibility = VISIBLE
            }
        }
        Glide.with(context.applicationContext)
            .load(adapterTabs[position].fullSnap?.getCutSnap(context))
            .into(holder.snap)

        holder.title.text = adapterTabs[position].title
        holder.url.text = URL(adapterTabs[position].url).host

        adapterTabs[position].fullSnap?.let { bitmap ->
            Palette.from(bitmap).generate { palette ->
                val vibrant = palette!!.getDominantColor(white)
                if (vibrant == white || vibrant == black) {
                    Glide.with(context.applicationContext).load(R.drawable.skeleton)
                        .into(holder.snap)
                }

                val titleColor: Int
                val urlColor: Int
                if (vibrant.luminance > 0.5) {
                    titleColor = black
                    urlColor = vibrant.darkenColor(0.4f)
                } else {
                    titleColor = white
                    urlColor = vibrant.lightenColor(0.4f)
                }

                holder.backgroundColor.setBackgroundColor(vibrant)

                holder.title.setTextColor(titleColor)
                holder.close.setTint(titleColor)
                holder.url.setTextColor(urlColor)
            }
        }

        holder.itemView.setOnClickListener {
            position = holder.layoutPosition
            if (context is BrowserActivity) {
                context.webViewContainer?.removeView(context.findViewById(R.id.webBrowser))
                context.webViewContainer?.removeAllViews()
                context.loadTab(position)
            } else {
                val intent = Intent(context, BrowserActivity::class.java)
                intent.putExtra("position", position)
                intent.putExtra("loadTab", true)
                context.startActivity(intent)
            }
            fragment.dismiss()
        }

        holder.close.setOnClickListener {
            val lastTab = adapterTabs.last().tab
            val removed = adapterTabs[holder.layoutPosition]

            openedTabs.remove(removed)
            Snackbar.make(fragment.requireView(), removed.url, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    openedTabs.add(removed)
                    updateState()
                }
                .setBackgroundTint(
                    ContextCompat.getColor(
                        fragment.requireContext(),
                        R.color.materialGray
                    )
                )
                .setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.white))
                .setActionTextColor(context.getAttrColor(R.attr.colorSecondary))
                .setAnchorView(fragment.binding.addTab)
                .show()

            updateState()

            if (context is BrowserActivity) {
                context.apply {
                    if (findViewById<BrowserView>(R.id.webBrowser) === lastTab) {
                        fragment.notifyPosition(adapterTabs.lastIndex)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateState() {
        val dfc = TabDiffUtil(adapterTabs, openedTabs)
        val difResult = DiffUtil.calculateDiff(dfc)
        adapterTabs.clear()
        adapterTabs.addAll(openedTabs)
        difResult.dispatchUpdatesTo(this)
        if (openedTabs.isNotEmpty()) {
            fragment.binding.label.text =
                "${context.getString(R.string.tabsOpened)} ${openedTabs.size}"
            fragment.binding.errorMessage.visibility = GONE
        } else {
            fragment.binding.label.text = context.getString(R.string.tabs)
            fragment.binding.errorMessage.visibility = VISIBLE
        }

    }

    override fun getItemCount(): Int {
        return adapterTabs.size
    }

    inner class ViewHolder(binding: TabItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val snap: ShapeableImageView = binding.snap
        val title: TextView = binding.title
        val url: TextView = binding.url
        val shadow: FrameLayout = binding.shadow
        val close: ImageButton = binding.close
        val backgroundColor: ShapeableImageView = binding.backgroundColor
    }

}
