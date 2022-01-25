package me.rocka.fcitx5test.ui.common

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.views.dsl.core.*
import splitties.views.dsl.core.styles.AndroidStyles
import splitties.views.gravityCenter

@Suppress("FunctionName")
fun <T> Context.DynamicListUi(
    mode: BaseDynamicListUi.Mode<T>,
    initialEntries: List<T>,
    enableOrder: Boolean = false,
    initCheckBox: (CheckBox.(Int) -> Unit) = { visibility = View.GONE },
    initSettingsButton: (ImageButton.(Int) -> Unit) = { visibility = View.GONE },
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
    show
)

@Suppress("FunctionName")
fun Context.ProgressBarDialogIndeterminate(): AlertDialog.Builder {
    val androidStyles = AndroidStyles(this)
    return AlertDialog.Builder(this)
        .setTitle(R.string.loading)
        .setView(frameLayout {
            add(androidStyles.progressBar.horizontal {
                isIndeterminate = true
            }, lParams {
                width = matchParent
                height = wrapContent
                gravity = gravityCenter
                horizontalMargin = dp(18)
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
