package com.litebrowser.adapter.bookmark

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.litebrowser.R
import com.litebrowser.activity.BrowserActivity
import com.litebrowser.application.ESearchApplication.Companion.database
import com.litebrowser.custom.popup.simple.SimplePopupBuilder
import com.litebrowser.custom.popup.simple.SimplePopupBuilder.Companion.COPY
import com.litebrowser.custom.popup.simple.SimplePopupBuilder.Companion.DELETE
import com.litebrowser.custom.popup.simple.SimplePopupBuilder.Companion.EDIT
import com.litebrowser.custom.popup.simple.SimplePopupBuilder.Companion.SHARE
import com.litebrowser.custom.popup.simple.SimplePopupClickListener
import com.litebrowser.custom.popup.simple.SimplePopupItem
import com.litebrowser.data.BrowserTabs.createNewTab
import com.litebrowser.database.bookmarks.Bookmark
import com.litebrowser.databinding.BookmarkItemBinding
import com.litebrowser.extensions.Extensions.getAttrColor
import com.litebrowser.extensions.Extensions.makeClip
import com.litebrowser.extensions.Extensions.shareWith
import com.litebrowser.fragment.bookmarks.BookmarksFragment
import com.litebrowser.fragment.dialog.BookmarkCreationDialog
import com.litebrowser.functions.Functions.byteArrayToBitmap
import com.litebrowser.functions.Functions.doInBackground


class BookmarksAdapter(
    private val fragment: BookmarksFragment,
    private var bookmarkList: List<Bookmark>,
    private val browser: WebView?
) :
    RecyclerView.Adapter<BookmarksAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            BookmarkItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarkList[position]
        Glide.with(fragment.requireContext().applicationContext)
            .load(byteArrayToBitmap(bookmark.icon!!))
            .into(holder.icon)
        holder.description.text = bookmark.description
        holder.url.text = bookmark.url
        holder.itemView.setOnClickListener {
            if (browser != null) (fragment.requireActivity() as BrowserActivity).createNewTab(
                bookmark.url
            )
            else {
                val intent = Intent(fragment.requireContext(), BrowserActivity::class.java)
                intent.putExtra("url", bookmark.url)
                fragment.requireContext().startActivity(intent)
            }
            fragment.dismiss()
        }
        holder.itemView.setOnLongClickListener {
            SimplePopupBuilder(fragment.requireContext(), it)
                .setMenuClickListener(SimplePopupClickListener { id ->
                    when (id) {
                        EDIT -> {
                            val bookmarkEdit =
                                BookmarkCreationDialog(
                                    bookmark.url,
                                    bookmark.description,
                                    true,
                                    bookmark.id
                                )
                            if (!bookmarkEdit.isAdded) bookmarkEdit.show(
                                fragment.requireActivity().supportFragmentManager,
                                "bookmark_edition"
                            )
                        }
                        SHARE -> fragment.requireContext().shareWith(bookmark.url)
                        COPY -> fragment.requireContext().makeClip(bookmark.url)
                        DELETE -> {
                            Snackbar.make(
                                fragment.requireView(),
                                bookmark.url,
                                Snackbar.LENGTH_LONG
                            )
                                .setBackgroundTint(
                                    ContextCompat.getColor(
                                        fragment.requireContext(),
                                        R.color.materialGray
                                    )
                                )
                                .setAction(R.string.undo) {
                                    doInBackground { database.bookmarkDao().insert(bookmark) }
                                }
                                .setTextColor(
                                    ContextCompat.getColor(
                                        fragment.requireContext(),
                                        R.color.white
                                    )
                                )
                                .setActionTextColor(
                                    fragment.requireContext().getAttrColor(R.attr.colorSecondary)
                                )
                                .show()
                            doInBackground {
                                database.bookmarkDao().delete(bookmark)
                            }
                        }
                    }
                })
                .addItems(
                    SimplePopupItem(EDIT, R.string.edit, R.drawable.ic_baseline_edit_24),
                    SimplePopupItem(SHARE, R.string.share, R.drawable.ic_baseline_share_24),
                    SimplePopupItem(COPY, R.string.copy, R.drawable.ic_baseline_content_copy_24),
                    SimplePopupItem(
                        DELETE,
                        R.string.delete,
                        R.drawable.ic_baseline_delete_sweep_24
                    ),
                )
                .show()
            true
        }
    }

    override fun getItemCount(): Int {
        return bookmarkList.size
    }

    inner class ViewHolder(binding: BookmarkItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val icon: ImageView = binding.icon
        val description: TextView = binding.description
        val url: TextView = binding.url
    }

}
