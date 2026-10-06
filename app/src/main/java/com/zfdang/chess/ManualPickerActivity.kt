package com.zfdang.chess

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.button.MaterialButton
import com.zfdang.chess.databinding.ActivityManualPickerBinding
import com.zfdang.chess.manuals.ManualEntry
import com.zfdang.chess.utils.PathUtil
import com.zfdang.chess.utils.WindowInsetsUtil
import java.io.File
import java.util.concurrent.Executors

/** A single-tap browser confined to the installed XQF library. */
class ManualPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManualPickerBinding
    private lateinit var library: File
    private lateinit var directory: File
    private val executor = Executors.newSingleThreadExecutor()
    private var generation = 0
    private var entries = emptyList<ManualEntry>()
    private val adapter = ManualAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowInsetsUtil.apply(this, binding.root)
        library = File(PathUtil.getInternalAppFilesDir(this, "XQF")).canonicalFile
        val requested = savedInstanceState?.getString("directory") ?: intent.getStringExtra(EXTRA_DIRECTORY)
        directory = requested?.let { safeDirectory(File(it)) } ?: library
        binding.manualList.layoutManager = LinearLayoutManager(this)
        binding.manualList.adapter = adapter
        binding.closePicker.setOnClickListener { finish() }
        binding.searchManual.doAfterTextChanged { filterEntries() }
        binding.searchManual.setOnEditorActionListener { view, _, _ ->
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
            true
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (directory == library) finish() else openDirectory(directory.parentFile ?: library)
            }
        })
        loadDirectory()
    }

    private fun safeDirectory(file: File): File? = runCatching {
        file.canonicalFile.takeIf { it.isDirectory && (it == library || it.path.startsWith(library.path + File.separator)) }
    }.getOrNull()

    private fun openDirectory(file: File) {
        directory = safeDirectory(file) ?: library
        binding.searchManual.setText("")
        binding.searchManual.clearFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.searchManual.windowToken, 0)
        loadDirectory()
    }

    private fun loadDirectory() {
        val requested = directory
        val requestGeneration = ++generation
        entries = emptyList()
        filterEntries()
        binding.emptyList.visibility = View.GONE
        binding.listSummary.text = getString(R.string.manual_picker_loading)
        binding.breadcrumbs.removeAllViews()
        val chain = generateSequence(directory) { if (it == library) null else it.parentFile }.toList().reversed()
        chain.forEachIndexed { index, folder ->
            val button = MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = if (folder == library) getString(R.string.manual_library) else folder.name
                setTextColor(ContextCompat.getColor(this@ManualPickerActivity, if (folder == directory) R.color.ui_red else R.color.ui_muted))
                textSize = 13f
                minimumHeight = dp(48)
                setOnClickListener { openDirectory(folder) }
            }
            binding.breadcrumbs.addView(button)
            if (index != chain.lastIndex) binding.breadcrumbs.addView(TextView(this).apply { text = "›"; gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(16), dp(48)))
        }
        binding.breadcrumbScroll.post { binding.breadcrumbScroll.fullScroll(View.FOCUS_RIGHT) }
        executor.execute {
            val files = runCatching {
                ManualEntry.readDirectory(requested)
            }.getOrNull()
            runOnUiThread {
                if (isDestroyed || isFinishing || requestGeneration != generation) return@runOnUiThread
                entries = files.orEmpty()
                filterEntries()
                if (files == null) {
                    binding.emptyList.text = getString(R.string.manual_picker_unavailable)
                    binding.emptyList.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun filterEntries() {
        val query = binding.searchManual.text?.toString()?.trim().orEmpty()
        val visible = entries.filter { it.name.contains(query, ignoreCase = true) }
        adapter.submitList(visible) { binding.manualList.scrollToPosition(0) }
        binding.listSummary.text = getString(R.string.manual_picker_summary, visible.count { it.isDir }, visible.count { !it.isDir })
        binding.emptyList.text = if (query.isEmpty()) getString(R.string.manual_empty_folder) else getString(R.string.manual_search_empty)
        binding.emptyList.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("directory", directory.path)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        ++generation
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private inner class ManualAdapter : ListAdapter<ManualEntry, ManualRow>(object : DiffUtil.ItemCallback<ManualEntry>() {
        override fun areItemsTheSame(oldItem: ManualEntry, newItem: ManualEntry) = oldItem.file.path == newItem.file.path
        override fun areContentsTheSame(oldItem: ManualEntry, newItem: ManualEntry) = oldItem == newItem
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManualRow {
            val row = LinearLayout(this@ManualPickerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(80)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setBackgroundResource(R.drawable.ui_card)
                withStyledAttributes(attrs = intArrayOf(android.R.attr.selectableItemBackground)) {
                    foreground = getDrawable(0)
                }
                isClickable = true
                isFocusable = true
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }
            }
            val icon = ImageView(this@ManualPickerActivity)
            row.addView(icon, LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(14) })
            val text = LinearLayout(this@ManualPickerActivity).apply { orientation = LinearLayout.VERTICAL }
            val title = TextView(this@ManualPickerActivity).apply {
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.ui_ink))
                maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val subtitle = TextView(this@ManualPickerActivity).apply {
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.ui_muted))
                setPadding(0, dp(5), 0, 0)
            }
            text.addView(title)
            text.addView(subtitle)
            row.addView(text, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this@ManualPickerActivity).apply { this.text = "›"; textSize = 24f; setTextColor(ContextCompat.getColor(context, R.color.ui_muted)); setPadding(dp(12), 0, 0, 0) })
            return ManualRow(row, icon, title, subtitle)
        }
        override fun onBindViewHolder(holder: ManualRow, position: Int) {
            val entry = getItem(position)
            val file = entry.file
            holder.title.text = entry.title
            holder.subtitle.text = if (entry.isDir) getString(R.string.manual_folder_hint) else getString(R.string.manual_file_hint)
            holder.icon.setImageResource(if (entry.isDir) R.drawable.ic_manual_folder else R.drawable.manual)
            holder.icon.setColorFilter(ContextCompat.getColor(this@ManualPickerActivity, if (entry.isDir) R.color.ui_muted else R.color.ui_red))
            holder.itemView.setOnClickListener {
                if (entry.isDir) openDirectory(file) else {
                    setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_MANUAL, file.path))
                    finish()
                }
            }
        }
    }

    private class ManualRow(view: View, val icon: ImageView, val title: TextView, val subtitle: TextView) : RecyclerView.ViewHolder(view)

    companion object {
        const val EXTRA_DIRECTORY = "manual_directory"
        const val EXTRA_MANUAL = "manual_path"
    }
}
