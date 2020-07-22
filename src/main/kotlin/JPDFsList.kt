import java.awt.Frame
import java.io.File
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JPanel

class JPDFsList(private val owner: Frame) : JPanel() {
    val pdfDocumentsCache = WeakHashMap<File, PDFDocument>()
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    fun addPDFDocuments(files: Iterable<File>) {
        val pdfs = files.associateWith { pdfDocumentsCache.getOrElse(it) { PDFDocument(it) } }
        pdfs.values.map { PDFDocumentEditModel(it) }.forEach { edt { add(JPDFDocumentListItem(it, this, owner)) } }
        pdfs.forEach { (file, pdf) -> pdfDocumentsCache.putIfAbsent(file, pdf) }
        edt {
            validate()
            repaint()
        }
    }

    fun removePDFDocument(doc: JPDFDocumentListItem) {
        edt {
            remove(doc)
            validate()
            repaint()
        }
    }
}