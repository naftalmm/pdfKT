
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import javax.swing.*

class JPDFDocumentEditView(owner: Frame, pdf: PDFDocument) : JDialog(owner, pdf.fileName) {
    init {
        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        defaultCloseOperation = DISPOSE_ON_CLOSE

        add(JImage(pdf.currentTitleImage.fit(800)))
        add(Box.createRigidArea(Dimension(0, 5)))
        val p = JPanel(FlowLayout())
        for ((pageIndex, rotation) in pdf.getCurrentState().pages) {
            val image = JImage(pdf.imagesThumbnails[pageIndex])
            p.add(if (rotation != Rotation.NORTH) image.rotate(rotation.angle.toDouble()) else image)
        }
        add(JScrollPane(p))
    }
}