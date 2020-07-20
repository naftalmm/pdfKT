import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import javax.imageio.ImageIO
import javax.swing.*

class JPDFDocumentEditView(owner: Frame, private val pdf: PDFDocument) : JDialog(owner, pdf.fileName) {
    companion object {
        private val loadingImage = ImageIO.read(PDFDocument::class.java.getResource("loading.gif"))
    }

    private val thumbnailsStubs = HashMap<Int, JImage>()
    private val p = JPanel(FlowLayout())

    init {
        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        defaultCloseOperation = DISPOSE_ON_CLOSE

        add(JImage(pdf.currentTitleImage.fit(600)))
        add(Box.createRigidArea(Dimension(0, 5)))
        if (pdf.thumbnailsAreReady()) {
            for ((pageIndex, rotation) in pdf.getCurrentState().pages) {
                p.add(JImage(pdf.getPageThumbnail(pageIndex).rotate(rotation.angle.toDouble())))
            }
        } else {
            for ((pageIndex, _) in pdf.getCurrentState().pages) {
                val stubImage = JImage(loadingImage)
                thumbnailsStubs[pageIndex] = stubImage
                p.add(stubImage)
            }
        }

        add(JScrollPane(p))
        GlobalScope.launch {
            loadPagesThumbnails()
        }
    }

    private suspend fun loadPagesThumbnails() = withContext(Dispatchers.Default) {
        for ((pageIndex, rotation) in pdf.getCurrentState().pages) {
            launch {
                val image = pdf.getPageThumbnail(pageIndex).rotate(rotation.angle.toDouble())
                thumbnailsStubs[pageIndex]?.repaintWith(image)
                println(pageIndex)
            }
        }
    }
}