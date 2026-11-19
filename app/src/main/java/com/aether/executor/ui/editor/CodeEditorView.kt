package com.aether.executor.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aether.executor.data.model.SettingsData
import com.aether.executor.ui.theme.AetherPalette
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Sora-based editor with a JS tokenizer. The editor is only pushed new text
 * when it really differs, so typing never causes a text reset loop.
 */
@Composable
fun AetherCodeEditor(
    tabId: String,
    content: String,
    settings: SettingsData,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val language = remember { JsLanguage() }
    val colorScheme = remember { createAetherColorScheme() }
    val currentContent = remember(tabId) { content }
    val latestOnChange by rememberUpdatedState(onContentChange)
    val lastKnownContent = remember(tabId) { arrayOf(content) }
    val changeHandler = remember(tabId) {
        { newContent: String ->
            lastKnownContent[0] = newContent
            latestOnChange(newContent)
        }
    }

    val editorHolder = remember(tabId) { arrayOfNulls<CodeEditor>(1) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CodeEditor(ctx).apply {
                setEditorLanguage(language)
                setColorScheme(colorScheme)
                setTextSizePx(with(density) { settings.fontSizeSp.sp.toPx() })
                setLineNumberEnabled(settings.lineNumbers)
                setWordwrap(settings.wordWrap)
                setTabWidth(settings.tabSize)
                setHighlightCurrentLine(true)
                setHighlightBracketPair(true)
                setLineSpacing(2f, 1.1f)
                setCursorWidth(with(density) { 2.dp.toPx() })
                setTypefaceText(android.graphics.Typeface.MONOSPACE)
                setTypefaceLineNumber(android.graphics.Typeface.MONOSPACE)
                setText(currentContent)
                subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
                    if (event.action != ContentChangeEvent.ACTION_SET_NEW_TEXT) {
                        changeHandler(text.toString())
                    }
                }
                editorHolder[0] = this
            }
        },
        update = { editor ->
            if (content != lastKnownContent[0]) {
                lastKnownContent[0] = content
                val hadFocus = editor.isFocused
                editor.setText(content)
                if (hadFocus) editor.requestFocus()
            }
            val textSizePx = with(density) { settings.fontSizeSp.sp.toPx() }
            if (editor.textSizePx != textSizePx) editor.setTextSizePx(textSizePx)
            editor.setWordwrap(settings.wordWrap)
            editor.setLineNumberEnabled(settings.lineNumbers)
            if (editor.tabWidth != settings.tabSize) editor.setTabWidth(settings.tabSize)
        }
    )

    DisposableEffect(tabId) {
        onDispose {
            editorHolder[0]?.release()
            editorHolder[0] = null
        }
    }
}

fun createAetherColorScheme(): EditorColorScheme = EditorColorScheme().apply {
    applyDefault()
    setColor(EditorColorScheme.WHOLE_BACKGROUND, AetherPalette.Navy.toArgb())
    setColor(EditorColorScheme.TEXT_NORMAL, AetherPalette.Text.toArgb())
    setColor(EditorColorScheme.KEYWORD, AetherPalette.Cyan.toArgb())
    setColor(EditorColorScheme.COMMENT, AetherPalette.Muted.toArgb())
    setColor(EditorColorScheme.LITERAL, AetherPalette.Amber.toArgb())
    setColor(EditorColorScheme.OPERATOR, AetherPalette.Blue.toArgb())
    setColor(EditorColorScheme.FUNCTION_NAME, AetherPalette.Blue.toArgb())
    setColor(EditorColorScheme.IDENTIFIER_NAME, AetherPalette.Text.toArgb())
    setColor(EditorColorScheme.IDENTIFIER_VAR, AetherPalette.Text.toArgb())
    setColor(EditorColorScheme.CURRENT_LINE, 0x141F3244)
    setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, AetherPalette.Panel.toArgb())
    setColor(EditorColorScheme.LINE_NUMBER, 0xFF4E6A80.toInt())
    setColor(EditorColorScheme.LINE_NUMBER_CURRENT, AetherPalette.Cyan.toArgb())
    setColor(EditorColorScheme.LINE_NUMBER_PANEL, AetherPalette.PanelAlt.toArgb())
    setColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT, AetherPalette.Text.toArgb())
    setColor(EditorColorScheme.LINE_DIVIDER, AetherPalette.Border.toArgb())
    setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, 0x5538A7C4)
    setColor(EditorColorScheme.SELECTION_INSERT, AetherPalette.Cyan.toArgb())
    setColor(EditorColorScheme.SELECTION_HANDLE, AetherPalette.Cyan.toArgb())
    setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, 0x3324A6C4)
    setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, AetherPalette.Cyan.toArgb())
    setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND, 0x2242D9F5)
    setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE, AetherPalette.Cyan.toArgb())
    setColor(EditorColorScheme.SCROLL_BAR_THUMB, 0xFF2C4A63.toInt())
    setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, AetherPalette.CyanDim.toArgb())
    setColor(EditorColorScheme.SCROLL_BAR_TRACK, AetherPalette.Navy.toArgb())
    setColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND, AetherPalette.PanelAlt.toArgb())
    setColor(EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR, AetherPalette.Text.toArgb())
}
