import java.awt.FlowLayout
import java.awt.Frame
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class JPDFDocumentListItem(private val pdf: PDFDocumentEditModel, private val pdfsList: JPDFsList) : JPanel() {
    init {
        layout = FlowLayout(FlowLayout.LEFT)

        add(pdf.currentTitleImageThumbnail)
        add(JLabel(pdf.fileName))
        add(JButton("Edit").apply { addActionListener { createAndShowEditDialog() } })
        add(JButton("Delete").also { it.addActionListener { pdfsList.removePDFDocument(this) } })

        maximumSize = preferredSize
        alignmentX = LEFT_ALIGNMENT
        isVisible = true
    }

    private fun createAndShowEditDialog() {
        val owner = SwingUtilities.getRoot(this) as Frame
        edt {
            JPDFDocumentEditView(owner, pdf).isVisible = true
        }
    }
}