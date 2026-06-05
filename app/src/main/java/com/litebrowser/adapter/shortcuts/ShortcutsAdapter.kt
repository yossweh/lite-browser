package com.litebrowser.adapter.shortcuts

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.litebrowser.R
import com.litebrowser.activity.BrowserActivity
import com.litebrowser.activity.MainActivity
import com.litebrowser.application.ESearchApplication.Companion.coeff
import com.litebrowser.application.ESearchApplication.Companion.database
import com.litebrowser.custom.popup.simple.SimplePopupBuilder
import com.litebrowser.custom.popup.simple.SimplePopupBuilder.Companion.COPY
import com.litebrowser.custom.popup.simple.SimplePopupBuilder.Companion.DELETE
import com.litebrowser.custom.popup.simple.SimplePopupBuilder.Companion.EDIT
import com.litebrowser.custom.popup.simple.SimplePopupBuilder.Companion.SHARE
import com.litebrowser.custom.popup.simple.SimplePopupClickListener
import com.litebrowser.custom.popup.simple.SimplePopupItem
import com.litebrowser.database.shortcuts.Shortcut
import com.litebrowser.databinding.MainGridItemBinding
import com.litebrowser.extensions.Extensions.getAttrColor
import com.litebrowser.extensions.Extensions.makeClip
import com.litebrowser.extensions.Extensions.shareWith
import com.litebrowser.fragment.dialog.ShortcutCreationDialog
import com.litebrowser.functions.Functions.byteArrayToBitmap
import com.litebrowser.functions.Functions.doInBackground

class ShortcutsAdapter(
    private var context: Context,
    private var newList: List<Shortcut>,
    private val isLast: Boolean
) :
    RecyclerView.Adapter<ShortcutsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(MainGridItemBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val shortcut = newList[position]
        Glide.with(context.applicationContext).load(byteArrayToBitmap(shortcut.icon!!))
            .into(holder.icon)

        holder.description.text = shortcut.description
        if (position != newList.size - 1 || !isLast) {
            holder.itemView.setOnClickListener {
                val intent = Intent(context, BrowserActivity::class.java)
                intent.putExtra("url", shortcut.url)
                context.startActivity(intent)
            }

            holder.itemView.setOnLongClickListener {
                SimplePopupBuilder(context, it)
                    .setMenuClickListener(SimplePopupClickListener { id ->
                        when (id) {
                            EDIT -> {
                                val shortcutEdit =
                                    ShortcutCreationDialog(
                                        shortcut.url,
                                        shortcut.description,
                                        true,
                                        shortcut.id
                                    )
                                if (!shortcutEdit.isAdded) shortcutEdit.show(
                                    (context as AppCompatActivity).supportFragmentManager,
                                    "shortcut_edition"
                                )
                            }
                            SHARE -> context.shareWith(shortcut.url)
                            COPY -> context.makeClip(shortcut.url)
                            DELETE -> {
                                Snackbar.make(
                                    (context as MainActivity).binding.root,
                                    shortcut.url,
                                    Snackbar.LENGTH_LONG
                                )
                                    .setBackgroundTint(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.materialGray
                                        )
                                    )
                                    .setAction(R.string.undo) {
                                        doInBackground { database.shortcutDao().insert(shortcut) }
                                    }
                                    .setActionTextColor(
                                        context.getAttrColor(R.attr.colorSecondary)
                                    )
                                    .setAnchorView((context as MainActivity).binding.fab)
                                    .setTextColor(ContextCompat.getColor(context, R.color.white))
                                    .show()
                                doInBackground { database.shortcutDao().delete(shortcut) }
                            }
                        }
                    })
                    .addItems(
                        SimplePopupItem(
                            EDIT,
                            R.string.edit,
                            R.drawable.ic_baseline_edit_24
                        ),
                        SimplePopupItem(
                            SHARE,
                            R.string.share,
                            R.drawable.ic_baseline_share_24
                        ),
                        SimplePopupItem(
                            COPY,
                            R.string.copy,
                            R.drawable.ic_baseline_content_copy_24
                        ),
                        SimplePopupItem(
                            DELETE,
                            R.string.delete,
                            R.drawable.ic_baseline_delete_sweep_24
                        ),
                    )
                    .show()
                true
            }

        } else if (isLast) {
            holder.itemView.setOnClickListener {
                val shortcutDialog = ShortcutCreationDialog()
                if (!shortcutDialog.isAdded) shortcutDialog.show(
                    (context as AppCompatActivity).supportFragmentManager,
                    "shortcutDialog"
                )
            }
            holder.itemView.setOnLongClickListener { true }
        }

        holder.itemView.layoutParams.height =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                coeff.toFloat(),
                context.resources.displayMetrics
            )
                .toInt()
    }

    override fun getItemCount(): Int {
        return newList.size
    }

    inner class ViewHolder(binding: MainGridItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val icon = binding.icon
        val description = binding.description
    }

}
