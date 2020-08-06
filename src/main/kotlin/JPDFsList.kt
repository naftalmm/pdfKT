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

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    //TODO rearrange PDFs in list
    fun addPDFDocuments(files: Iterable<File>) {
        val thereWereNoPDFsAdded = getPDFsListSize() == 0
        for (file in files) {
            val pdfDocument = pdfDocumentsCache[file]?.get() ?: PDFDocument(file)
            pdfDocumentsCache.putIfAbsent(file, WeakReference(pdfDocument))
            edt {
                val pdf = JPDFDocumentListItem(PDFDocumentEditModel(pdfDocument))
                add(pdf)
                subscribeTo(pdf)
            }
        }
        edt {
            validate()
            repaint()
            if (thereWereNoPDFsAdded && getPDFsListSize() > 0) {
                notifySubscribers(FirstPDFWasAdded)
            }
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