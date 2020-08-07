import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JPanel
import kotlin.collections.ArrayList
import kotlin.reflect.KClass

class JPDFsList : JPanel(), MultiObservable, Observer {
    override val subscribers: MutableMap<KClass<out ObservableEvent>, MutableList<Observer>> = hashMapOf()
    override val allEventsSubscribers: MutableList<Observer> = ArrayList()
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

            val pdf = JPDFDocumentListItem(PDFDocumentEditModel(pdfDocument)).apply {
                addMouseListener(drag)
            }
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
        edt {
            remove(doc)
            validate()
            repaint()
            if (getPDFsListSize() == 0) notifySubscribers(AllPDFsWereRemoved)
        }
    }

    private fun getPDFsListSize() = this.components.size

    override fun update(event: ObservableEvent) = when (event) {
        is PDFWasRemoved -> removePDFDocument(event.pdf)
        else -> doNothing()
    }
}