package me.rocka.fcitx5test.ui.main.settings

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cn.berberman.girls.utils.maybe.Maybe
import cn.berberman.girls.utils.maybe.fx
import kotlinx.coroutines.*
import me.rocka.fcitx5test.R
import me.rocka.fcitx5test.data.PinyinDictManager
import me.rocka.fcitx5test.data.pinyin.Dictionary
import me.rocka.fcitx5test.data.pinyin.LibIMEDictionary
import me.rocka.fcitx5test.ui.common.BaseDynamicListUi
import me.rocka.fcitx5test.ui.common.OnItemChangedListener
import me.rocka.fcitx5test.ui.main.MainViewModel
import me.rocka.fcitx5test.utils.queryFileName
import splitties.systemservices.notificationManager
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

class PinyinDictionaryFragment : Fragment(), OnItemChangedListener<LibIMEDictionary>,
    CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    private val viewModel: MainViewModel by activityViewModels()

    private val entries: List<LibIMEDictionary>
        get() = ui.entries

    private val contentResolver: ContentResolver
        get() = requireContext().contentResolver

    private lateinit var launcher: ActivityResultLauncher<String>

    private val dirty: AtomicBoolean = AtomicBoolean(false)
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
                        idx,
                        entry.also {
                            if (isChecked)
                                it.enable()
                            else it.disable()
                        })
                }
            }
        ) {
            init {
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
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.get("uri")?.let { it as? Uri }?.let { importFromUri(it) }
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
        launch {
            val id = IMPORT_ID++
            Maybe.fx {
                val file = uri.queryFileName(contentResolver).bindNullable().let { File(it) }
                when {
                    file.nameWithoutExtension in entries.map { it.name } -> {
                        errorDialog(getString(R.string.dict_already_exists))
                        return@fx pure(Unit)
                    }
                    Dictionary.Type.fromFileName(file.name) == null -> {
                        errorDialog(getString(R.string.invalid_dict))
                        return@fx pure(Unit)
                    }
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
                @Suppress("BlockingMethodInNonBlockingContext")
                val inputStream = contentResolver.openInputStream(uri).bindNullable()
                runCatching {
                    val result: LibIMEDictionary
                    measureTimeMillis {
                        result = PinyinDictManager.importFromInputStream(
                            inputStream,
                            file.name
                        )
                    }.also {
                        Timber.tag(this@PinyinDictionaryFragment.javaClass.name)
                            .d("Took $it to import $result")
                    }
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
                pure(Unit)
            }
            notificationManager.cancel(id)
        }

    private fun errorDialog(message: String) {
        launch(Dispatchers.Main) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.import_error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setIcon(R.drawable.ic_baseline_error_24)
                .show()
        }
    }

    private fun reloadDict() {
        if (!dirty.get())
            return
        launch(Dispatchers.IO) {
            if (busy.compareAndSet(false, true)) {
                val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_library_books_24)
                    .setContentTitle(getString(R.string.pinyin_dict))
                    .setContentText(getString(R.string.refreshing))
                    .setOngoing(true)
                    .setProgress(100, 0, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                val id = RELOAD_ID++
                builder.build().let { notificationManager.notify(id, it) }
                measureTimeMillis {
                    viewModel.fcitx.reloadPinyinDict()
                }.let {
                    Timber.tag(this@PinyinDictionaryFragment.javaClass.name)
                        .d("Took $it to reload dict")
                }
                notificationManager.cancel(id)
                busy.set(false)
                clean()
            }
        }
    }

    private fun dirty() {
        if (dirty.compareAndSet(false, true)) {
            launch(Dispatchers.Main) {
                viewModel.enableToolbarSaveButton { reloadDict() }
            }
        }
    }

    private fun clean() {
        if (dirty.compareAndSet(true, false)) {
            launch(Dispatchers.Main) {
                viewModel.disableToolbarSaveButton()
            }
        }
    }

    override fun onItemAdded(idx: Int, item: LibIMEDictionary) {
        dirty()
    }

    override fun onItemRemoved(idx: Int, item: LibIMEDictionary) {
        item.file.delete()
        dirty()
    }

    override fun onItemUpdated(idx: Int, old: LibIMEDictionary, new: LibIMEDictionary) {
        dirty()
    }

    override fun onPause() {
        reloadDict()
        super.onPause()
    }

    override fun onDestroy() {
        coroutineContext.cancel()
        super.onDestroy()
    }

    companion object {
        private var RELOAD_ID = 0
        private var IMPORT_ID = 0
        const val CHANNEL_ID = "pinyin_dict"
    }
}