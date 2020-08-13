import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.lang.ref.WeakReference
import javax.swing.BoxLayout
import javax.swing.JPanel
import kotlin.reflect.KClass

class JPDFsList : JPanel(), MultiObservable, Observer {
    override val subscribers: HashMap<KClass<out ObservableEvent>, MutableList<WeakReference<Observer>>> = hashMapOf()
    override val allEventsSubscribers = ArrayList<WeakReference<Observer>>()
    private val openedDocumentsUsages = CloseableObjectsUsage<PDFDocument>()
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

    internal class CloseableObjectsUsage<O : AutoCloseable> {
        private val objectToUsers: HashMap<O, MutableSet<Any>> = hashMapOf()
        private val userToObject: HashMap<Any, MutableSet<O>> = hashMapOf()

        fun register(user: Any, obj: O) {
            objectToUsers.getOrPut(obj) { HashSet() }.add(user)
            userToObject.getOrPut(user) { HashSet() }.add(obj)
        }

        fun deregister(user: Any) {
            val objects = userToObject.remove(user) ?: return
            for (obj in objects) {
                val users = objectToUsers[obj] ?: continue
                if (users.size == 1) {
                    obj.close()
                    objectToUsers.remove(obj)
                } else {
                    users.remove(user)
                }
            }
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
            openedDocumentsUsages.register(pdf, pdfDocument)
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
        openedDocumentsUsages.deregister(doc)
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

    override fun update(event: ObservableEvent) = when (event) {
        is PDFWasRemoved -> removePDFDocument(event.pdf)
        else -> doNothing()
    }
}