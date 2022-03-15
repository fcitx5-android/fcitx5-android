package org.fcitx.fcitx5.android.ui.common

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.dsl.core.styles.AndroidStyles

@Suppress("FunctionName")
fun <T> Context.DynamicListUi(
    mode: BaseDynamicListUi.Mode<T>,
    initialEntries: List<T>,
    enableOrder: Boolean = false,
    initCheckBox: (CheckBox.(Int) -> Unit) = { visibility = View.GONE },
    initSettingsButton: (ImageButton.(Int) -> Unit) = { visibility = View.GONE },
    useCustomTouchCallback: Boolean = false,
    show: (T) -> String
): BaseDynamicListUi<T> = object :
    BaseDynamicListUi<T>(
        this,
        mode,
        initialEntries,
        enableOrder,
        initCheckBox,
        initSettingsButton
    ) {
    init {
        if (!useCustomTouchCallback)
            addTouchCallback()
    }

    override fun showEntry(x: T): String = show(x)
}

@Suppress("FunctionName")
fun <T> Context.CheckBoxListUi(
    initialEntries: List<T>,
    initCheckBox: (CheckBox.(Int) -> Unit),
    initSettingsButton: (ImageButton.(Int) -> Unit),
    show: (T) -> String
) = DynamicListUi(
    BaseDynamicListUi.Mode.Immutable(),
    initialEntries,
    false,
    initCheckBox,
    initSettingsButton,
    false,
    show
)

@Suppress("FunctionName")
fun Context.ProgressBarDialogIndeterminate(): AlertDialog.Builder {
    val androidStyles = AndroidStyles(this)
    return AlertDialog.Builder(this)
        .setTitle(R.string.loading)
        .setView(verticalLayout {
            add(androidStyles.progressBar.horizontal {
                isIndeterminate = true
            }, lParams {
                width = matchParent
                verticalMargin = dp(20)
                horizontalMargin = dp(26)
            })
        })
        .setCancelable(false)
}

fun CoroutineScope.withLoadingDialog(
    context: Context,
    threshold: Long = 200L,
    action: suspend () -> Unit
) {
    val loading = context.ProgressBarDialogIndeterminate().create()
    val job = launch {
        delay(threshold)
        loading.show()
    }
    launch {
        action()
        job.cancelAndJoin()
        if (loading.isShowing)
            loading.dismiss()
    }
}
