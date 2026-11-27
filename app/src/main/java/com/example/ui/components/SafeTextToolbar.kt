package com.example.ui.components

import android.graphics.Rect as AndroidRect
import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

private enum class TextToolbarMenuOption(
    val itemId: Int,
    val order: Int,
    val titleRes: Int
) {
    Copy(0, 0, android.R.string.copy),
    Paste(1, 1, android.R.string.paste),
    Cut(2, 2, android.R.string.cut),
    SelectAll(3, 3, android.R.string.selectAll)
}

/**
 * Guarded Android TextToolbar implementation that prevents double-finish or
 * finishing an already-torn-down FloatingActionMode on DecorView during dialog
 * dismissals, screen transitions, or view detachment.
 */
class SafeAndroidTextToolbar(
    private val view: View
) : TextToolbar {
    private var actionMode: ActionMode? = null
    private var isActionModeDestroyed: Boolean = false

    private var menuRect: Rect = Rect.Zero
    private var onCopyRequested: (() -> Unit)? = null
    private var onPasteRequested: (() -> Unit)? = null
    private var onCutRequested: (() -> Unit)? = null
    private var onSelectAllRequested: (() -> Unit)? = null

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    private val floatingCallback = object : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) return false
            onCopyRequested?.let { addItem(menu, TextToolbarMenuOption.Copy) }
            onPasteRequested?.let { addItem(menu, TextToolbarMenuOption.Paste) }
            onCutRequested?.let { addItem(menu, TextToolbarMenuOption.Cut) }
            onSelectAllRequested?.let { addItem(menu, TextToolbarMenuOption.SelectAll) }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) return false
            syncItem(menu, TextToolbarMenuOption.Copy, onCopyRequested != null)
            syncItem(menu, TextToolbarMenuOption.Paste, onPasteRequested != null)
            syncItem(menu, TextToolbarMenuOption.Cut, onCutRequested != null)
            syncItem(menu, TextToolbarMenuOption.SelectAll, onSelectAllRequested != null)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                TextToolbarMenuOption.Copy.itemId -> onCopyRequested?.invoke()
                TextToolbarMenuOption.Paste.itemId -> onPasteRequested?.invoke()
                TextToolbarMenuOption.Cut.itemId -> onCutRequested?.invoke()
                TextToolbarMenuOption.SelectAll.itemId -> onSelectAllRequested?.invoke()
                else -> return false
            }
            hide()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            if (mode == null || mode === actionMode) {
                isActionModeDestroyed = true
                actionMode = null
                status = TextToolbarStatus.Hidden
            }
        }

        override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: AndroidRect?) {
            outRect?.set(
                menuRect.left.toInt(),
                menuRect.top.toInt(),
                menuRect.right.toInt(),
                menuRect.bottom.toInt()
            )
        }
    }

    private fun addItem(menu: Menu, option: TextToolbarMenuOption) {
        menu.add(Menu.NONE, option.itemId, option.order, option.titleRes)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    private fun syncItem(menu: Menu, option: TextToolbarMenuOption, visible: Boolean) {
        val existing = menu.findItem(option.itemId)
        if (visible && existing == null) {
            addItem(menu, option)
        } else if (!visible && existing != null) {
            menu.removeItem(option.itemId)
        }
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        this.menuRect = rect
        this.onCopyRequested = onCopyRequested
        this.onPasteRequested = onPasteRequested
        this.onCutRequested = onCutRequested
        this.onSelectAllRequested = onSelectAllRequested

        if (!view.isAttachedToWindow) {
            status = TextToolbarStatus.Hidden
            return
        }

        val current = actionMode
        if (current == null || isActionModeDestroyed) {
            isActionModeDestroyed = false
            val started = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    view.startActionMode(floatingCallback, ActionMode.TYPE_FLOATING)
                } else {
                    @Suppress("DEPRECATION")
                    view.startActionMode(floatingCallback)
                }
            }.getOrNull()
            actionMode = started
            status = if (started != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden
        } else {
            status = TextToolbarStatus.Shown
            runCatching {
                current.invalidate()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    current.invalidateContentRect()
                }
            }
        }
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        val current = actionMode
        actionMode = null
        val canFinish = current != null && !isActionModeDestroyed && view.isAttachedToWindow
        isActionModeDestroyed = true
        if (canFinish) {
            runCatching { current?.finish() }
        }
    }
}

@Composable
fun ProvideSafeTextToolbar(content: @Composable () -> Unit) {
    val view = LocalView.current
    val toolbar = remember(view) { SafeAndroidTextToolbar(view) }
    DisposableEffect(view, toolbar) {
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                toolbar.hide()
            }
        }
        view.addOnAttachStateChangeListener(attachListener)
        onDispose {
            runCatching { view.removeOnAttachStateChangeListener(attachListener) }
            toolbar.hide()
        }
    }
    CompositionLocalProvider(LocalTextToolbar provides toolbar, content = content)
}
