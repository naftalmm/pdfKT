import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.lang.ref.WeakReference
import javax.swing.BoxLayout
import javax.swing.JPanel

class JPDFsList : JPanel(), Observer, MultiObservable by MultiObservableImpl() {
    private val pdfDocumentsCache = HashMap<File, WeakReference<PDFDocument>>()
    private val drag = object : MouseAdapter() {
        private lateinit var pressed: MouseEvent
        private lateinit var originalCursor: Cursor

        override fun mousePressed(e: MouseEvent) {
            pressed = e
            originalCursor = cursor
            cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
            e.component.addMouseMotionListener(this)
        }

        override fun mouseDragged(e: MouseEvent) = with(e) {
            val snapSize = component.height
            val maxY = (componentCount - 1) * snapSize
            translatePoint(0, component.location.y - pressed.y)
            component.setLocation(0, y.coerceIn(0, maxY) / snapSize * snapSize)

            edt {
                val currentComponentIndex = components.indexOf(component)
                val currentComponentIndexByCoordinate = component.y / snapSize
                if (currentComponentIndex != currentComponentIndexByCoordinate) {
                    remove(component)
                    add(component, currentComponentIndexByCoordinate)
                    validate()
                    repaint()
                }
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            cursor = originalCursor
            e.component.removeMouseMotionListener(this)
        }
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    fun addPDFDocuments(files: Iterable<File>) {
        val wasEmpty = getPDFsListSize() == 0
        for (file in files) {
            val pdfDocument = pdfDocumentsCache[file]?.get() ?: PDFDocument(file)
            pdfDocumentsCache.putIfAbsent(file, WeakReference(pdfDocument))

            val pdf = JPDFDocumentListItem(PDFDocumentEditModel(pdfDocument)).apply { addMouseListener(drag) }
            CloseableObjectsUsage.register(pdf, pdfDocument)
            subscribeTo(pdf)
            edt {
                add(pdf)
            }
        }
        edt {
            validate()
            repaint()
            if (wasEmpty && getPDFsListSize() > 0) notifySubscribers(FirstPDFWasAdded)
        }
    }

    private fun removePDFDocument(doc: JPDFDocumentListItem) {
        CloseableObjectsUsage.deregister(doc)
        doc.decompose()

        edt {
            remove(doc)
            validate()
            repaint()
            if (getPDFsListSize() == 0) notifySubscribers(AllPDFsWereRemoved)
        }
    }

    private fun getPDFsListSize() = components.size

    fun getCurrentPDFsState(): List<Pair<File, DocumentState>> =
        components.map { it as JPDFDocumentListItem }.map { it.pdf }.map { it.pdf.file to it.getCurrentState() }

    override fun update(event: ObservableEvent) {
        if (event is PDFWasRemoved) removePDFDocument(event.pdf)
    }
}