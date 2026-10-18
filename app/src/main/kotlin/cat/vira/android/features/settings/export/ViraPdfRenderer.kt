package cat.vira.android.features.settings.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import cat.vira.android.R
import java.io.File
import kotlin.math.ceil

/// Dibuixa un `ViraPdfDocument` en A4 amb `PdfDocument`, l'API del sistema (`ViraPDFRenderer`).
///
/// **Fonts fixes, no la mida de lletra del telèfon.** És l'única superfície de Vira on això és
/// correcte: un PDF no s'adapta a qui el llegeix, i 10,5 pt de cos fan una A4 llegible impresa.
/// L'accessibilitat d'aquest camí és el JSON i la pantalla d'Ajustos, que sí escalen.
///
/// Els colors són tinta sobre paper blanc, no tokens de pantalla: el PDF no té mode fosc.
object ViraPdfRenderer {
    // A4 en punts (72 ppp).
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val FOOTER_HEIGHT = 28f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
    private const val BOTTOM = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT

    private const val INK = 0xFF1C1B1F.toInt()
    private const val INK_SECONDARY = 0xFF5F5C66.toInt()
    private const val INK_TERTIARY = 0xFF8E8B94.toInt()
    private const val RULE = 0xFFD5D3DA.toInt()

    fun write(context: Context, document: ViraPdfDocument, file: File): File {
        val fonts = Fonts(context)
        val pdf = PdfDocument()
        try {
            val cursor = Cursor(pdf, fonts)
            cursor.beginPage()
            cursor.draw(document.title, fonts.paint(fonts.bold, 24f, INK), spacingAfter = 4f)
            cursor.draw(document.subtitle, fonts.paint(fonts.regular, 9f, INK_SECONDARY), spacingAfter = 18f)
            document.sections.forEach(cursor::drawSection)
            cursor.finishPage()
            file.outputStream().use(pdf::writeTo)
        } finally {
            pdf.close()
        }
        return file
    }

    private class Fonts(private val context: Context) {
        private fun load(id: Int, fallback: Typeface) = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback
        val bold = load(R.font.satoshi_bold, Typeface.DEFAULT_BOLD)
        val medium = load(R.font.satoshi_medium, Typeface.DEFAULT)
        val regular = load(R.font.satoshi_regular, Typeface.DEFAULT)

        fun paint(face: Typeface, size: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            textSize = size
            this.color = color
        }
    }

    /// On som a la pàgina i quantes en portem.
    private class Cursor(private val pdf: PdfDocument, private val fonts: Fonts) {
        private var page: PdfDocument.Page? = null
        private val canvas: Canvas get() = requireNotNull(page).canvas
        private var y = MARGIN
        private var pageNumber = 0

        private val body = fonts.paint(fonts.regular, 10.5f, INK)
        private val value = fonts.paint(fonts.medium, 10.5f, INK)
        private val small = fonts.paint(fonts.regular, 9f, INK_SECONDARY)
        private val section = fonts.paint(fonts.bold, 14f, INK)
        private val empty = fonts.paint(fonts.regular, 10.5f, INK_SECONDARY)

        fun beginPage() {
            pageNumber += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            y = MARGIN
        }

        /// Peu amb el número de pàgina, en tancar-la: numerar-les a l'inici obligaria a saber-ne el total.
        fun finishPage() {
            val footer = fonts.paint(fonts.regular, 9f, INK_TERTIARY)
            canvas.drawText("$pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN + footer.textSize, footer)
            pdf.finishPage(page)
            page = null
        }

        private fun ensureRoom(height: Float) {
            if (y + height <= BOTTOM) return
            finishPage()
            beginPage()
        }

        private fun layout(
            text: String,
            paint: TextPaint,
            width: Float,
            alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        ): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
                .setAlignment(alignment)
                .setIncludePad(false)
                .build()

        fun draw(text: String, paint: TextPaint, indent: Float = 0f, spacingAfter: Float = 2f) {
            val layout = layout(text, paint, CONTENT_WIDTH - indent)
            ensureRoom(layout.height.toFloat())
            canvas.save()
            canvas.translate(MARGIN + indent, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + spacingAfter
        }

        fun drawSection(item: ViraPdfDocument.Section) {
            // Prou espai per al títol **i** la primera fila: un títol sol al peu d'una pàgina es
            // llegeix com una secció buida.
            ensureRoom(46f)
            y += 10f
            draw(item.title, section, spacingAfter = 3f)
            drawRule()
            item.note?.let { draw(it, small, spacingAfter = 4f) }
            if (item.rows.isEmpty()) {
                draw(item.emptyText, empty, spacingAfter = 4f)
                return
            }
            item.rows.forEach(::drawRow)
        }

        private fun drawRule() {
            ensureRoom(6f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = RULE
                strokeWidth = 0.5f
            }
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, paint)
            y += 6f
        }

        /// Etiqueta a l'esquerra, valor a la dreta. Si l'etiqueta és llarga, s'embolica a la seva
        /// columna en comptes de trepitjar el valor; un valor llarg (una nota) també s'embolica, a la
        /// seva, que mai passa de la meitat de l'amplada.
        private fun drawRow(row: ViraPdfDocument.Row) {
            val text = row.value?.takeIf { it.isNotEmpty() }
            val valueWidth = text?.let { ceil(value.measureText(it)).coerceAtMost(CONTENT_WIDTH / 2) } ?: 0f
            val labelWidth = CONTENT_WIDTH - if (valueWidth > 0) valueWidth + 12f else 0f
            val label = layout(row.label, body, labelWidth)
            val valueLayout = text?.let { layout(it, value, valueWidth, Layout.Alignment.ALIGN_OPPOSITE) }
            val height = maxOf(label.height, valueLayout?.height ?: 0).toFloat()
            ensureRoom(height + 4f)
            canvas.save()
            canvas.translate(MARGIN, y)
            label.draw(canvas)
            canvas.restore()
            if (valueLayout != null) {
                canvas.save()
                canvas.translate(MARGIN + CONTENT_WIDTH - valueWidth, y)
                valueLayout.draw(canvas)
                canvas.restore()
            }
            y += height + 2f
            val detail = row.detail?.takeIf { it.isNotEmpty() }
            if (detail != null) draw(detail, small, spacingAfter = 3f) else y += 1f
        }
    }
}
