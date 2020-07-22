import java.awt.FlowLayout
import java.awt.Frame
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class JPDFDocumentListItem(private val pdf: PDFDocumentEditModel, private val pdfsList: JPDFsList, private val owner: Frame) : JPanel() {

    init {
        layout = FlowLayout(FlowLayout.LEFT)

        add(pdf.currentTitleImageThumbnail)
        add(JLabel(pdf.fileName))
        add(JButton("Edit").also { it.addActionListener { createAndShowEditDialog() } })
        add(JButton("Delete").also { it.addActionListener { pdfsList.removePDFDocument(this)} })

        maximumSize = preferredSize
        alignmentX = LEFT_ALIGNMENT
        isVisible = true
    }

    private fun createAndShowEditDialog() {
        edt {
            JPDFDocumentEditView(owner, pdf).isVisible = true
        }
    }
}