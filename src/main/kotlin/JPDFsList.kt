import java.awt.Cursor
import java.awt.event.MouseEvent
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JLayeredPane
import javax.swing.event.MouseInputAdapter
import kotlin.collections.ArrayList
import kotlin.reflect.KClass

class JPDFsList : JLayeredPane(), MultiObservable, Observer {
    override val subscribers: MutableMap<KClass<out ObservableEvent>, MutableList<Observer>> = hashMapOf()
    override val allEventsSubscribers: MutableList<Observer> = ArrayList()
    private val pdfDocumentsCache = HashMap<File, WeakReference<PDFDocument>>()
    private val drag = object : SimpleVerticalDragListener() {
        override fun mousePressed(e: MouseEvent) {
            super.mousePressed(e)
            moveToFront(e.component)
        }

        override fun mouseReleased(e: MouseEvent) {
            super.mouseReleased(e)
            edt {
                remove(e.component)
                add(e.component, getIndexOfChildComponentByYCoordinate(e.component.y))
                validate()
                repaint()
            }
        }
    }

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    fun getIndexOfChildComponentByYCoordinate(y: Int) =
        components.withIndex().find { it.value.location.y >= y }?.index ?: components.lastIndex + 1

    fun addPDFDocuments(files: Iterable<File>) {
        val wasEmpty = getPDFsListSize() == 0
        for (file in files) {
            val pdfDocument = pdfDocumentsCache[file]?.get() ?: PDFDocument(file)
            pdfDocumentsCache.putIfAbsent(file, WeakReference(pdfDocument))

            val pdf = JPDFDocumentListItem(PDFDocumentEditModel(pdfDocument)).apply {
                addMouseListener(drag)
                addMouseMotionListener(drag)
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

open class SimpleVerticalDragListener : MouseInputAdapter() {
    private lateinit var pressed: MouseEvent
    private lateinit var originalCursor : Cursor
    override fun mousePressed(e: MouseEvent)  = with(e) {
        pressed = this
        originalCursor = component.cursor
        component.cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }

    override fun mouseDragged(e: MouseEvent) = with(e) {
        translatePoint(-x, component.location.y - pressed.y)
        component.setLocation(x, y)
    }

    override fun mouseReleased(e: MouseEvent) = with(e)  {
        component.cursor = originalCursor
    }
}