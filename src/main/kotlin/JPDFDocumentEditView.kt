import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.imageio.ImageIO
import javax.swing.*

class JPDFDocumentEditView(owner: Frame, private val pdf: PDFDocument) : JDialog(owner, pdf.fileName) {
    companion object {
        private val loadingImage = ImageIO.read(PDFDocument::class.java.getResource("loading.gif"))
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val thumbnailsStubs = HashMap<Int, JImage>()
    private val p = JPanel(FlowLayout())

    init {
        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = scope.coroutineContext.cancelChildren()
        })

        add(JImage(pdf.currentTitleImage.fit(600)))
        add(Box.createRigidArea(Dimension(0, 5)))

        for ((pageIndex, _) in pdf.getCurrentState().pages) {
            val stubImage = JImage(loadingImage)
            thumbnailsStubs[pageIndex] = stubImage
            p.add(stubImage)
        }
        add(JScrollPane(p))
        loadPagesThumbnails(scope)
    }

    private fun loadPagesThumbnails(scope: CoroutineScope) {
        for ((pageIndex, rotation) in pdf.getCurrentState().pages) {
            scope.launch {
                val image = pdf.getPageThumbnail(pageIndex).rotate(rotation.angle.toDouble())
                thumbnailsStubs[pageIndex]?.repaintWith(image)
            }
        }
    }
}