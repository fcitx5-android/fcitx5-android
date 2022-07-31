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
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import arrow.core.None
import arrow.core.Option
import arrow.core.continuations.option
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.reloadPinyinDict
import org.fcitx.fcitx5.android.data.pinyin.PinyinDictManager
import org.fcitx.fcitx5.android.data.pinyin.dict.Dictionary
import org.fcitx.fcitx5.android.data.pinyin.dict.LibIMEDictionary
import org.fcitx.fcitx5.android.ui.common.BaseDynamicListUi
import org.fcitx.fcitx5.android.ui.common.OnItemChangedListener
import org.fcitx.fcitx5.android.ui.main.MainViewModel
import org.fcitx.fcitx5.android.utils.NaiveDustman
import org.fcitx.fcitx5.android.utils.errorDialog
import org.fcitx.fcitx5.android.utils.queryFileName
import splitties.systemservices.notificationManager
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

class PinyinDictionaryFragment : Fragment(), OnItemChangedListener<LibIMEDictionary> {

    private val viewModel: MainViewModel by activityViewModels()

    private val entries: List<LibIMEDictionary>
        get() = ui.entries

    private val contentResolver: ContentResolver
        get() = requireContext().contentResolver

    private lateinit var launcher: ActivityResultLauncher<String>

    private val dustman = NaiveDustman<Boolean>().apply {
        onDirty = {
            viewModel.enableToolbarSaveButton { reloadDict() }

        }
        onClean = {
            viewModel.disableToolbarSaveButton()
        }
    }
    private val busy: AtomicBoolean = AtomicBoolean(false)

    private val ui: BaseDynamicListUi<LibIMEDictionary> by lazy {
        object : BaseDynamicListUi<LibIMEDictionary>(
            requireContext(),
            Mode.Custom(),
            PinyinDictManager.libIMEDictionaries(),
            initCheckBox = { idx ->
                val entry = entries[idx]
                isChecked = entry.isEnabled
                setOnClickListener {
                    ui.updateItem(
                        ui.indexItem(entry),
                        entry.also {
                            if (isChecked)
                                it.enable()
                            else it.disable()
                        })
                }
            }
        ) {
            init {
                addTouchCallback()
                fab.setOnClickListener {
                    launcher.launch("*/*")
                }
            }

            override fun updateFAB() {
                // do nothing
            }

            override fun showEntry(x: LibIMEDictionary): String = x.name
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        createNotificationChannel()
        viewModel.disableToolbarSaveButton()
        viewModel.setToolbarTitle(getString(R.string.pinyin_dict))
        registerLauncher()
        ui.addOnItemChangedListener(this)
        resetDustman()
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.get(INTENT_DATA_URI)
            ?.let { it as? Uri }
            ?.let { importFromUri(it) }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getText(R.string.pinyin_dict),
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
                        importErrorDialog(getString(R.string.dict_already_exists))
                        shift(None)
                    }
                    Dictionary.Type.fromFileName(file.name) == null -> {
                        importErrorDialog(getString(R.string.invalid_dict))
                        shift(None)
                    }
                    else -> Unit
                }
                val builder =
                    NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                        .setContentTitle(getString(R.string.pinyin_dict))
                        .setContentText("${getString(R.string.importing)} ${file.nameWithoutExtension}")
                        .setOngoing(true)
                        .setProgress(100, 0, true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.build().let { notificationManager.notify(id, it) }
                val inputStream = Option
                    .catch { contentResolver.openInputStream(uri) }
                    .mapNotNull { it }
                    .bind()
                runCatching {
                    val result: LibIMEDictionary
                    measureTimeMillis {
                        inputStream.use { i ->
                            result = PinyinDictManager.importFromInputStream(
                                i,
                                file.name
                            ).getOrThrow()
                        }
                    }.also { Timber.d("Took $it to import $result") }
                    result
                }
                    .onFailure {
                        importErrorDialog(it.localizedMessage ?: it.stackTraceToString())
                    }
                    .onSuccess {
                        launch(Dispatchers.Main) {
                            ui.addItem(item = it)
                        }
                    }
            }
            notificationManager.cancel(id)
        }

    private suspend fun importErrorDialog(message: String) {
        errorDialog(requireContext(), getString(R.string.import_error), message)
    }

    private fun reloadDict() {
        if (!dustman.dirty)
            return
        lifecycleScope.launch(NonCancellable + Dispatchers.IO) {
            if (busy.compareAndSet(false, true)) {
                val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                    .setContentTitle(getString(R.string.pinyin_dict))
                    .setContentText(getString(R.string.reloading))
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                val id = RELOAD_ID++
                builder.build().let { notificationManager.notify(id, it) }
                measureTimeMillis {
                    viewModel.fcitx.reloadPinyinDict()
                }.let { Timber.d("Took $it to reload dict") }
                notificationManager.cancel(id)
                busy.set(false)
                launch(Dispatchers.Main) {
                    resetDustman()
                }
            }
        }
    }

    private fun resetDustman() {
        dustman.reset((entries.associate { it.name to it.isEnabled }))
    }

    override fun onItemAdded(idx: Int, item: LibIMEDictionary) {
        dustman.addOrUpdate(item.name, item.isEnabled)
    }

    override fun onItemRemoved(idx: Int, item: LibIMEDictionary) {
        item.file.delete()
        dustman.remove(item.name)
    }

    override fun onItemUpdated(idx: Int, old: LibIMEDictionary, new: LibIMEDictionary) {
        dustman.addOrUpdate(new.name, new.isEnabled)
    }

    override fun onPause() {
        reloadDict()
        super.onPause()
    }

    companion object {
        private var RELOAD_ID = 0
        private var IMPORT_ID = 0
        const val CHANNEL_ID = "pinyin_dict"
        const val INTENT_DATA_URI = "uri"
    }
}