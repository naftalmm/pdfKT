import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.property.AreaBreakType
import com.itextpdf.layout.property.TextAlignment
import java.io.File
import java.io.File.separatorChar as slash

fun main() {
    createPDF("1")            // 1.pdf
    createPDF("2")            // 2.pdf
    createPDF("1", "2")       // 12.pdf
    createPDF("2", "1")       // 21.pdf
    createPDF("1", "2", "3")  // 123.pdf
}

private fun createPDF(vararg pages: String) {
    val outputFile = File("src${slash}test${slash}resources${slash}${pages.joinToString(separator = "")}.pdf")
    Document(PdfDocument(PdfWriter(outputFile))).use {
        pages.forEachIndexed { index, page ->
            it.add(Paragraph(Text(page).apply { setFontSize(500.0f) }).apply { setTextAlignment(TextAlignment.CENTER) })
            if (index != pages.lastIndex) {
                 it.add(AreaBreak(AreaBreakType.NEXT_PAGE))
            }
        }
    }
}