
import java.awt.Frame
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JPanel

class JPDFsList(private val owner: Frame) : JPanel() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    fun addPDFDocuments(files: Iterable<File>) {
        files.map { PDFDocument(it) }.forEach { edt { add(JPDFDocumentListItem(it, this, owner)) }}
        edt {
            validate()
            repaint()
        }
    }

    fun removePDFDocument(doc: JPDFDocumentListItem) {
        edt {
            doc.dispose()
            remove(doc)
            validate()
            repaint()
        }
    }
}