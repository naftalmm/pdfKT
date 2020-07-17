
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Frame
import javax.swing.JDialog
import javax.swing.JPanel

class JPDFDocumentEditView(owner: Frame, pdf: PDFDocument) : JDialog(owner, pdf.fileName) {
    init {
        layout = FlowLayout()

        add(JImage(pdf.currentTitleImage).fit(800), BorderLayout.CENTER)
        val p = JPanel(FlowLayout())
        for ((pageIndex, rotation) in pdf.getCurrentState().pages) {
            val image = JImage(pdf.images[pageIndex]).fit(200)
            p.add(if (rotation != Rotation.NORTH) image.rotate(rotation.angle.toDouble()) else image)
        }
        add(p, BorderLayout.SOUTH)
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        defaultCloseOperation = DISPOSE_ON_CLOSE
    }
}