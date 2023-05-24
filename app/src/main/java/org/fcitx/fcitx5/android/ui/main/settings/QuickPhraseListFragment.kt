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
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.materialTextInput
import org.fcitx.fcitx5.android.utils.notificationManager
import org.fcitx.fcitx5.android.utils.queryFileName
import org.fcitx.fcitx5.android.utils.str
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.verticalLayout
import splitties.views.imageResource
import splitties.views.setPaddingDp
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class QuickPhraseListFragment : Fragment(), OnItemChangedListener<QuickPhrase> {

    private val viewModel: MainViewModel by activityViewModels()

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
            initCheckBox = { entry ->
                setOnCheckedChangeListener(null)
                isEnabled = true
                isChecked = entry.isEnabled
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) entry.enable() else entry.disable()
                    ui.updateItem(ui.indexItem(entry), entry)
                }
            },
            initSettingsButton = { entry ->
                visibility = if (!entry.isEnabled) View.GONE else View.VISIBLE
                fun edit() {
                    findNavController().navigate(
                        R.id.action_quickPhraseListFragment_to_quickPhraseEditFragment,
                        bundleOf(QuickPhraseEditFragment.ARG to entry)
                    )
                    parentFragmentManager.setFragmentResultListener(
                        QuickPhraseEditFragment.RESULT,
                        this@QuickPhraseListFragment
                    ) { _, _ ->
                        ui.updateItem(ui.indexItem(entry), entry)
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
                                                ui.updateItem(ui.indexItem(entry), entry)
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
                        .setTitle(R.string.quickphrase_editor)
                        .setItems(actions) { _, i ->
                            when (i) {
                                0 -> {
                                    launcher.launch("*/*")
                                }
                                1 -> {
                                    val (inputLayout, editText) = materialTextInput {
                                        setHint(R.string.name)
                                    }
                                    val layout = verticalLayout {
                                        setPaddingDp(20, 10, 20, 0)
                                        add(inputLayout, lParams(matchParent))
                                    }
                                    AlertDialog.Builder(requireContext())
                                        .setTitle(R.string.create_new)
                                        .setView(layout)
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            ui.addItem(item = QuickPhraseManager.newEmpty(editText.str))
                                        }
                                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                        .show()
                                }
                                else -> {}
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            }

            override fun updateFAB() {
                // do nothing
            }

            override fun showEntry(x: QuickPhrase): String = x.name

        }.also {
            // Builtin quick phrase shouldn't be removed
            // But it can be disabled
            it.removable = { e -> e !is BuiltinQuickPhrase }
            it.addTouchCallback()
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

    private fun importFromUri(uri: Uri) {
        val nm = notificationManager
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            val id = IMPORT_ID++
            option {
                val file = uri.queryFileName(contentResolver).bind().let { File(it) }
                when {
                    file.nameWithoutExtension in ui.entries.map { it.name } -> {
                        errorDialog(getString(R.string.quickphrase_already_exists))
                        shift(None)
                    }
                    file.extension != QuickPhrase.EXT -> {
                        errorDialog(getString(R.string.invalid_quickphrase))
                        shift(None)
                    }
                    else -> Unit
                }

                NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_format_quote_24)
                    .setContentTitle(getString(R.string.quickphrase_editor))
                    .setContentText("${getString(R.string.importing)} ${file.nameWithoutExtension}")
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build().let { nm.notify(id, it) }
                val inputStream = Option.catch { contentResolver.openInputStream(uri) }
                    .mapNotNull { it }
                    .bind()
                runCatching {
                    inputStream.use { i ->
                        QuickPhraseManager.importFromInputStream(i, file.name).getOrThrow()
                    }
                }.onFailure {
                    errorDialog(it.localizedMessage ?: it.stackTraceToString())
                }.onSuccess {
                    launch(Dispatchers.Main) {
                        ui.addItem(item = it)
                    }
                }
            }
            nm.cancel(id)
        }
    }

    private fun errorDialog(message: String) {
        lifecycleScope.launch {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.import_error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show()
        }
    }

    private fun reloadQuickPhrase() {
        if (!dustman.dirty) return
        resetDustman()
        // save the reference to NotificationManager, in case we need to cancel notification
        // after Fragment detached
        val nm = notificationManager
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            if (busy.compareAndSet(false, true)) {
                val id = RELOAD_ID++
                NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                    .setContentTitle(getString(R.string.quickphrase_editor))
                    .setContentText(getString(R.string.reloading))
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build().let { nm.notify(id, it) }
                viewModel.fcitx.runOnReady {
                    reloadQuickPhrase()
                }
                nm.cancel(id)
                busy.set(false)
            }
        }
    }

    private fun resetDustman() {
        dustman.reset(ui.entries.associate { it.name to it.isEnabled })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        createNotificationChannel()
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

    override fun onItemRemovedBatch(indexed: List<Pair<Int, QuickPhrase>>) {
        batchRemove(indexed)
    }

    override fun onItemUpdated(idx: Int, old: QuickPhrase, new: QuickPhrase) {
        dustman.addOrUpdate(new.name, new.isEnabled)
    }

    override fun onResume() {
        super.onResume()
        viewModel.enableToolbarEditButton {
            ui.enterMultiSelect(
                requireActivity().onBackPressedDispatcher,
                viewModel
            )
        }
    }

    override fun onPause() {
        reloadQuickPhrase()
        ui.exitMultiSelect(viewModel)
        viewModel.disableToolbarEditButton()
        super.onPause()
    }

    override fun onDestroy() {
        ui.removeItemChangedListener()
        super.onDestroy()
    }

    companion object {
        private var RELOAD_ID = 0
        private var IMPORT_ID = 0
        const val CHANNEL_ID = "quickphrase"
    }
}