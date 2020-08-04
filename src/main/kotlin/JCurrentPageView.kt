import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.JPanel

class JCurrentPageView(private val pdf: PDFDocumentEditModel) : JPanel() {
    private val currentImageMaxDimension = 600

    private val currentPageImageView = JImage(pdf.getCurrentTitleImage().fit(currentImageMaxDimension)).apply {
        alignmentY = Component.CENTER_ALIGNMENT
    }

    init {
        add(currentPageImageView)
        add(Box.createRigidArea(Dimension(0, currentImageMaxDimension)))
    }

    fun setCurrentPage(pageIndex: Int) {
        currentPageImageView.repaintWith(pdf.getCurrentPageImage(pageIndex).fit(currentImageMaxDimension))
        edt {
            validate()
            repaint()
        }
    }
}