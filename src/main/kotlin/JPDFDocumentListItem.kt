import java.awt.FlowLayout
import java.awt.Frame
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class JPDFDocumentListItem(private val pdf: PDFDocument, private val pdfsList: JPDFsList, private val owner: Frame) : JPanel() {
    private val THUMBNAIL_SIZE = 50

    init {
        layout = FlowLayout(FlowLayout.LEFT)

        add(JImage(pdf.currentTitleImage.fit(THUMBNAIL_SIZE)))
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

    fun dispose() {
        pdf.cancelPageImagesLoadingJob()
    }
}