import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JPanel

class JPDFsList : JPanel() {
    private val pdfDocumentsCache = HashMap<File, WeakReference<PDFDocument>>()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    fun addPDFDocuments(files: Iterable<File>) {
        for (file in files) {
            val pdfDocument = pdfDocumentsCache[file]?.get() ?: PDFDocument(file)
            pdfDocumentsCache.putIfAbsent(file, WeakReference(pdfDocument))
            edt {
                add(JPDFDocumentListItem(PDFDocumentEditModel(pdfDocument), this))
            }
        }
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