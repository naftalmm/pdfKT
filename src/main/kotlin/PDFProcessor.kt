import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import java.io.File
import java.nio.file.Path

object PDFProcessor {
    fun cat(input: List<Pair<File, DocumentState>>, output: Path) {
        PdfDocument(PdfWriter(output.toFile())).use { out ->
            input.forEach { (file, state) ->
                PdfDocument(PdfReader(file)).use { `in` ->
                    state.pages.forEach { (pageIndex, rotation) ->
                        val page = `in`.getPage(pageIndex + 1)
                        page.rotation = (page.rotation + rotation.angle) % 360
                    }
                    `in`.copyPagesTo(state.pages.keys.map { it + 1  }, out)
                }
            }
        }
    }
}
