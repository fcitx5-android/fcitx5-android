package org.fcitx.fcitx5.android.ui.main.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import arrow.core.None
import arrow.core.Option
import arrow.core.continuations.option
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.reloadQuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.BuiltinQuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.CustomQuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhrase
import org.fcitx.fcitx5.android.data.quickphrase.QuickPhraseManager
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.DynamicListTouchCallback
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.queryFileName
import splitties.dimensions.dp
import splitties.systemservices.notificationManager
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.add
import splitties.views.dsl.core.editText
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.imageResource
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

class QuickPhraseListFragment : Fragment(), OnItemChangedListener<QuickPhrase> {

    private val viewModel: MainViewModel by activityViewModels()

    private val entries: List<QuickPhrase>
        get() = ui.entries

    private val contentResolver: ContentResolver
        get() = requireContext().contentResolver

    private lateinit var launcher: ActivityResultLauncher<String>

    private val busy: AtomicBoolean = AtomicBoolean(false)
    private val dustman = NaiveDustman<Boolean>().apply {
        onDirty = {
            viewModel.enableToolbarSaveButton { reloadQuickPhrase() }
        }
        onClean = {
            viewModel.disableToolbarSaveButton()
        }
    }

    private val ui: BaseDynamicListUi<QuickPhrase> by lazy {
        object : BaseDynamicListUi<QuickPhrase>(
            requireContext(),
            Mode.Custom(),
            QuickPhraseManager.listQuickPhrase(),
            initCheckBox = { idx ->
                val entry = entries[idx]
                isEnabled = true
                isChecked = entry.isEnabled
                setOnClickListener {
                    ui.updateItem(ui.indexItem(entry), entry.also {
                        if (isChecked)
                            it.enable()
                        else
                            it.disable()
                    })
                }
            },
            initSettingsButton = { idx ->
                val entry = entries[idx]
                visibility = if (!entry.isEnabled)
                    View.GONE
                else
                    View.VISIBLE
                fun edit() {
                    findNavController().navigate(
                        R.id.action_quickPhraseListFragment_to_quickPhraseEditFragment,
                        bundleOf(QuickPhraseEditFragment.ARG to entry)
                    )
                    parentFragmentManager.setFragmentResultListener(
                        QuickPhraseEditFragment.RESULT,
                        this@QuickPhraseListFragment
                    ) { _, _ ->
                        ui.updateItem(idx, entry)
                        // editor changed file content
                        dustman.forceDirty()
                    }
                }
                when (entry) {
                    is BuiltinQuickPhrase -> {
                        if (entry.override != null) {
                            imageResource = R.drawable.ic_baseline_expand_more_24
                            setOnClickListener {
                                val actions =
                                    arrayOf(getString(R.string.edit), getString(R.string.reset))
                                AlertDialog.Builder(requireContext())
                                    .setItems(actions) { _, i ->
                                        when (i) {
                                            0 -> edit()
                                            1 -> {
                                                entry.deleteOverride()
                                                ui.updateItem(idx, entry)
                                                // not sure if the content changes
                                                dustman.forceDirty()
                                            }
                                        }
                                    }
                                    .show()
                            }
                        } else {
                            imageResource = R.drawable.ic_baseline_edit_24
                            setOnClickListener {
                                edit()
                            }
                        }

                    }
                    is CustomQuickPhrase -> {
                        imageResource = R.drawable.ic_baseline_edit_24
                        setOnClickListener {
                            edit()
                        }
                    }
                }

            }
        ) {
            init {
                enableUndo = false
                fab.setOnClickListener {
                    // TODO use expandable fab instead
                    val actions = arrayOf(
                        getString(R.string.import_from_file),
                        getString(R.string.create_new)
                    )
                    AlertDialog.Builder(requireContext())
                        .setItems(actions) { _, i ->
                            when (i) {
                                0 -> {
                                    launcher.launch("*/*")
                                }
                                1 -> {
                                    val editText = editText {
                                        setHint(R.string.name)
                                    }
                                    val layout = constraintLayout {
                                        add(editText, lParams {
                                            height = wrapContent
                                            width = matchParent
                                            topOfParent()
                                            bottomOfParent()
                                            leftOfParent(dp(20))
                                            rightOfParent(dp(20))
                                        })
                                    }
                                    AlertDialog.Builder(requireContext())
                                        .setTitle(R.string.create_new)
                                        .setView(layout)
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            ui.addItem(item = QuickPhraseManager.newEmpty(editText.text.toString()))
                                        }
                                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                        .show()
                                }
                                else -> {}
                            }
                        }
                        .show()


                }
            }

            override fun updateFAB() {
                // do nothing
            }

            override fun showEntry(x: QuickPhrase): String = x.name

        }.also {
            it.addTouchCallback(object : DynamicListTouchCallback<QuickPhrase>(it) {
                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int {
                    // Builtin quick phrase shouldn't be removed
                    // But it can be disabled
                    if (entries[viewHolder.bindingAdapterPosition] is BuiltinQuickPhrase)
                        return if (it.enableOrder) ItemTouchHelper.UP or ItemTouchHelper.DOWN
                        else ItemTouchHelper.ACTION_STATE_IDLE
                    return super.getSwipeDirs(recyclerView, viewHolder)
                }
            })
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getText(R.string.quickphrase_editor),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = CHANNEL_ID }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerLauncher() {
        launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null)
                importFromUri(uri)
        }
    }

    private fun importFromUri(uri: Uri) =
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            val id = IMPORT_ID++
            option {
                val file = uri.queryFileName(contentResolver).bind().let { File(it) }
                when {
                    file.nameWithoutExtension in entries.map { it.name } -> {
                        errorDialog(getString(R.string.quickphrase_already_exists))
                        shift(None)
                    }
                    file.extension != QuickPhrase.EXT -> {
                        errorDialog(getString(R.string.invalid_quickphrase))
                        shift(None)
                    }
                    else -> Unit
                }

                val builder =
                    NotificationCompat.Builder(
                        requireContext(),
                        CHANNEL_ID
                    )
                        .setSmallIcon(R.drawable.ic_baseline_format_quote_24)
                        .setContentTitle(getString(R.string.quickphrase_editor))
                        .setContentText("${getString(R.string.importing)} ${file.nameWithoutExtension}")
                        .setOngoing(true)
                        .setProgress(100, 0, true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.build().let { notificationManager.notify(id, it) }
                val inputStream = Option.catch { contentResolver.openInputStream(uri) }
                    .mapNotNull { it }
                    .bind()
                runCatching {
                    val result: CustomQuickPhrase
                    measureTimeMillis {
                        inputStream.use { i ->
                            result = QuickPhraseManager.importFromInputStream(
                                i,
                                file.name
                            ).getOrThrow()
                        }
                    }.also { Timber.d("Took $it to import $result") }
                    result
                }
                    .onFailure {
                        errorDialog(it.localizedMessage ?: it.stackTraceToString())
                    }
                    .onSuccess {
                        launch(Dispatchers.Main) {
                            ui.addItem(item = it)
                        }
                    }
            }
            notificationManager.cancel(id)
        }

    private fun errorDialog(message: String) {
        lifecycleScope.launch {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.import_error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIcon(R.drawable.ic_baseline_error_24)
                .show()
        }
    }

    private fun reloadQuickPhrase() {
        if (!dustman.dirty)
            return
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            if (busy.compareAndSet(false, true)) {
                val builder = NotificationCompat.Builder(
                    requireContext(),
                    CHANNEL_ID
                )
                    .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                    .setContentTitle(getString(R.string.quickphrase_editor))
                    .setContentText(getString(R.string.reloading))
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                val id = RELOAD_ID++
                builder.build().let { notificationManager.notify(id, it) }
                measureTimeMillis {
                    viewModel.fcitx.reloadQuickPhrase()
                }.let { Timber.d("Took $it to reload quickphrase") }
                notificationManager.cancel(id)
                busy.set(false)
                launch(Dispatchers.Main) {
                    resetDustman()
                }
            }
        }
    }


    private fun resetDustman() {
        dustman.reset(entries.associate {
            it.name to it.isEnabled
        })
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        createNotificationChannel()
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(getString(R.string.quickphrase_editor))
        registerLauncher()
        ui.addOnItemChangedListener(this)
        resetDustman()
        return ui.root
    }

    override fun onItemAdded(idx: Int, item: QuickPhrase) {
        dustman.addOrUpdate(item.name, item.isEnabled)
    }

    override fun onItemRemoved(idx: Int, item: QuickPhrase) {
        item.file.delete()
        dustman.remove(item.name)
    }

    override fun onItemUpdated(idx: Int, old: QuickPhrase, new: QuickPhrase) {
        dustman.addOrUpdate(new.name, new.isEnabled)
    }

    override fun onPause() {
        reloadQuickPhrase()
        super.onPause()
    }

    companion object {
        private var RELOAD_ID = 0
        private var IMPORT_ID = 0
        const val CHANNEL_ID = "quickphrase"
    }
}