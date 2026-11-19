package com.aether.executor.ui.editor

import android.os.Bundle
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lang.styling.MappedSpans
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lightweight, dependency-free JavaScript language support.
 *
 * The analyzer tokenizes the whole document off the UI thread. It covers the
 * constructs that matter for a script editor: keywords, identifiers, function
 * calls, numbers, strings, template literals, line comments and block comments.
 */
class JsLanguage : Language {

    private val analyzeManager = JsAnalyzeManager()
    private val newlineHandler = JsNewlineHandler()
    private val symbolPairs = createSymbolPairs()

    override fun getAnalyzeManager(): AnalyzeManager = analyzeManager

    override fun getInterruptionLevel(): Int = Language.INTERRUPTION_LEVEL_SLIGHT

    override fun requireAutoComplete(
        source: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        // Intentionally no completion: scripts must stay predictable and offline.
    }

    override fun getIndentAdvance(source: ContentReference, line: Int, column: Int): Int {
        val text = source.getLine(line)
        val limit = if (line == source.getLineCount() - 1 && column > text.length) {
            text.length
        } else {
            minOf(column, text.length)
        }
        val before = text.substring(0, limit).trimEnd()
        return when {
            before.endsWith("{") || before.endsWith("[") || before.endsWith("(") -> 4
            before.endsWith(":") && !before.endsWith("::") -> 4
            else -> 0
        }
    }

    override fun useTab(): Boolean = false

    override fun getFormatter(): Formatter = JsFormatter

    override fun getSymbolPairs(): SymbolPairMatch = symbolPairs

    override fun getNewlineHandlers(): Array<NewlineHandler> = arrayOf(newlineHandler)

    override fun destroy() {
        analyzeManager.destroy()
    }

    private fun createSymbolPairs(): SymbolPairMatch {
        val pairs = SymbolPairMatch()
        pairs.putPair('{', SymbolPairMatch.SymbolPair("{", "}"))
        pairs.putPair('[', SymbolPairMatch.SymbolPair("[", "]"))
        pairs.putPair('(', SymbolPairMatch.SymbolPair("(", ")"))
        pairs.putPair('\'', SymbolPairMatch.SymbolPair("'", "'"))
        pairs.putPair('"', SymbolPairMatch.SymbolPair("\"", "\""))
        pairs.putPair('`', SymbolPairMatch.SymbolPair("`", "`"))
        return pairs
    }
}

private class JsAnalyzeManager : AnalyzeManager {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "aether-js-analyzer").apply { isDaemon = true }
    }
    private val generation = AtomicInteger(0)

    @Volatile
    private var receiver: StyleReceiver? = null

    @Volatile
    private var reference: ContentReference? = null

    @Volatile
    private var destroyed = false

    override fun setReceiver(receiver: StyleReceiver?) {
        this.receiver = receiver
    }

    override fun reset(ref: ContentReference, extraArguments: Bundle) {
        reference = ref
        schedule()
    }

    override fun insert(start: CharPosition, end: CharPosition, inserted: CharSequence) {
        schedule()
    }

    override fun delete(start: CharPosition, end: CharPosition, deleted: CharSequence) {
        schedule()
    }

    override fun rerun() {
        schedule()
    }

    private fun schedule() {
        if (destroyed) return
        val target = receiver ?: return
        val ref = reference ?: return
        val requestId = generation.incrementAndGet()
        executor.execute {
            if (destroyed || requestId != generation.get()) return@execute
            val styles = buildStyles(ref)
            if (destroyed || requestId != generation.get()) return@execute
            target.setStyles(this, styles)
        }
    }

    override fun destroy() {
        destroyed = true
        generation.incrementAndGet()
        receiver = null
        reference = null
        executor.shutdownNow()
    }

    private fun buildStyles(ref: ContentReference): Styles {
        val builder = MappedSpans.Builder(ref.getLineCount())
        var inBlockComment = false
        var inTemplate = false

        for (lineIndex in 0 until ref.getLineCount()) {
            val line = ref.getLine(lineIndex)
            val spans = tokenizeLine(line, inBlockComment, inTemplate)
            inBlockComment = spans.inBlockComment
            inTemplate = spans.inTemplate

            if (spans.tokens.isEmpty()) {
                builder.add(lineIndex, SpanFactory.obtain(0, NORMAL))
            } else {
                var cursor = 0
                for (token in spans.tokens) {
                    if (token.start > cursor) {
                        builder.add(lineIndex, SpanFactory.obtain(cursor, NORMAL))
                    }
                    builder.add(lineIndex, SpanFactory.obtain(token.start, token.style))
                    cursor = token.end
                }
                if (cursor == 0) {
                    builder.add(lineIndex, SpanFactory.obtain(0, NORMAL))
                }
            }
        }
        if (ref.getLineCount() == 0) {
            builder.addNormalIfNull()
        }
        return Styles(builder.build())
    }

    private data class Token(val start: Int, val end: Int, val style: Long)

    private data class LineTokens(
        val tokens: List<Token>,
        val inBlockComment: Boolean,
        val inTemplate: Boolean
    )

    private fun tokenizeLine(
        line: String,
        blockCommentAtStart: Boolean,
        templateAtStart: Boolean
    ): LineTokens {
        val tokens = ArrayList<Token>()
        val length = line.length
        var i = 0
        var inBlockComment = blockCommentAtStart
        var inTemplate = templateAtStart

        if (inBlockComment) {
            val end = line.indexOf("*/")
            if (end < 0) {
                if (length > 0) tokens.add(Token(0, length, COMMENT))
                return LineTokens(tokens, true, inTemplate)
            }
            val stop = end + 2
            tokens.add(Token(0, stop, COMMENT))
            inBlockComment = false
            i = stop
        }

        if (inTemplate) {
            val end = findStringEnd(line, -1, '`')
            if (end < 0) {
                if (length > i) tokens.add(Token(i, length, STRING))
                return LineTokens(tokens, inBlockComment, true)
            }
            tokens.add(Token(i, end, STRING))
            inTemplate = false
            i = end
        }

        while (i < length) {
            val c = line[i]
            when {
                c == '/' && i + 1 < length && line[i + 1] == '/' -> {
                    tokens.add(Token(i, length, COMMENT))
                    i = length
                }

                c == '/' && i + 1 < length && line[i + 1] == '*' -> {
                    val end = line.indexOf("*/", i + 2)
                    if (end < 0) {
                        tokens.add(Token(i, length, COMMENT))
                        inBlockComment = true
                        i = length
                    } else {
                        tokens.add(Token(i, end + 2, COMMENT))
                        i = end + 2
                    }
                }

                c == '"' || c == '\'' -> {
                    val end = findStringEnd(line, i, c)
                    if (end < 0) {
                        tokens.add(Token(i, length, STRING))
                        i = length
                    } else {
                        tokens.add(Token(i, end, STRING))
                        i = end
                    }
                }

                c == '`' -> {
                    val end = findStringEnd(line, i, '`')
                    if (end < 0) {
                        tokens.add(Token(i, length, STRING))
                        inTemplate = true
                        i = length
                    } else {
                        tokens.add(Token(i, end, STRING))
                        i = end
                    }
                }

                c.isDigit() || (c == '.' && i + 1 < length && line[i + 1].isDigit()) -> {
                    val start = i
                    i++
                    while (i < length && (line[i].isLetterOrDigit() || line[i] == '.' ||
                                line[i] == '_')
                    ) {
                        i++
                    }
                    tokens.add(Token(start, i, NUMBER))
                }

                c.isLetter() || c == '_' || c == '$' -> {
                    val start = i
                    i++
                    while (i < length && (line[i].isLetterOrDigit() || line[i] == '_' ||
                                line[i] == '$')
                    ) {
                        i++
                    }
                    val word = line.substring(start, i)
                    val style = when {
                        word in KEYWORDS -> KEYWORD
                        word == "true" || word == "false" || word == "null" ||
                            word == "undefined" || word == "NaN" -> LITERAL_STYLE
                        isCallStart(line, i) -> FUNCTION
                        else -> NORMAL
                    }
                    if (style != NORMAL) tokens.add(Token(start, i, style))
                }

                c in OPERATORS -> {
                    tokens.add(Token(i, i + 1, OPERATOR))
                    i++
                }

                else -> i++
            }
        }
        return LineTokens(tokens, inBlockComment, inTemplate)
    }

    private fun isCallStart(line: String, index: Int): Boolean {
        var i = index
        while (i < line.length && line[i] == ' ') i++
        return i < line.length && line[i] == '('
    }

    private fun findStringEnd(line: String, start: Int, quote: Char): Int {
        var i = start + 1
        while (i < line.length) {
            val c = line[i]
            if (c == '\\') {
                i += 2
                continue
            }
            if (c == quote) return i + 1
            i++
        }
        return -1
    }

    companion object {
        // Sora spans carry EditorColorScheme ids, not ARGB values. The actual
        // colors are mapped in AetherEditorColorScheme.
        private val NORMAL = TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)
        private val KEYWORD = TextStyle.makeStyle(EditorColorScheme.KEYWORD, true)
        private val STRING = TextStyle.makeStyle(EditorColorScheme.LITERAL)
        private val NUMBER = TextStyle.makeStyle(EditorColorScheme.LITERAL)
        private val COMMENT = TextStyle.makeStyle(
            EditorColorScheme.COMMENT,
            0,
            false,
            true,
            false
        )
        private val FUNCTION = TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME)
        private val OPERATOR = TextStyle.makeStyle(EditorColorScheme.OPERATOR)
        private val LITERAL_STYLE = TextStyle.makeStyle(EditorColorScheme.LITERAL)

        private val KEYWORDS = setOf(
            "async", "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "export", "extends", "finally",
            "for", "function", "get", "if", "import", "in", "instanceof", "let", "new",
            "of", "return", "set", "static", "super", "switch", "this", "throw", "try",
            "typeof", "var", "void", "while", "with", "yield"
        )

        private val OPERATORS = charArrayOf(
            '+', '-', '*', '/', '%', '=', '<', '>', '!', '&', '|', '^', '~', '?', ':'
        )
    }
}

/**
 * Formatting stays disabled: scripts are executed verbatim, so the editor must
 * never silently rewrite them.
 */
private object JsFormatter : Formatter {
    override fun format(text: Content, cursorRange: TextRange) = Unit
    override fun formatRegion(
        text: Content,
        rangeToFormat: TextRange,
        cursorRange: TextRange
    ) = Unit

    override fun setReceiver(receiver: Formatter.FormatResultReceiver?) = Unit
    override fun isRunning(): Boolean = false
    override fun destroy() = Unit
}

private class JsNewlineHandler : NewlineHandler {

    override fun matchesRequirement(
        text: Content,
        cursor: CharPosition,
        styles: Styles?
    ): Boolean {
        if (cursor.column == 0) return false
        val line = text.getLine(cursor.line).toString()
        val before = line.substring(0, minOf(cursor.column, line.length)).trimEnd()
        return before.endsWith("{") || before.endsWith("[") || before.endsWith("(")
    }

    override fun handleNewline(
        text: Content,
        cursor: CharPosition,
        styles: Styles?,
        tabSize: Int
    ): NewlineHandleResult {
        val line = text.getLine(cursor.line).toString()
        val leading = line.takeWhile { it == ' ' || it == '\t' }
        val unit = " ".repeat(tabSize.coerceAtLeast(2))
        val inner = leading + unit
        val afterCursor = line.substring(minOf(cursor.column, line.length))
        val closing = afterCursor.trimStart().firstOrNull()

        return if (closing == '}' || closing == ']' || closing == ')') {
            NewlineHandleResult("\n$inner\n$leading", leading.length + 1)
        } else {
            NewlineHandleResult("\n$inner", 0)
        }
    }
}
