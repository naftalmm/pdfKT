import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class JPDFDocumentListItem(private val pdf: PDFDocumentEditModel) : JPanel(), Observer, Observable<PDFWasRemoved> {
    override val subscribers: MutableList<Observer> = ArrayList()
    private val titleImageMaxSize = 50
    private val currentTitleImage = JImage(getCurrentTitleImagePreview())

    private fun getCurrentTitleImagePreview() = pdf.getCurrentTitleImage().fit(titleImageMaxSize)

    init {
        layout = FlowLayout(FlowLayout.LEFT)
        add(currentTitleImage)
        add(JLabel(pdf.fileName))
        add(JButton("Edit").apply { addActionListener { createAndShowEditDialog() } })
        add(JButton("Delete").apply { addActionListener { notifySubscribers() } })
        preferredSize = Dimension(size.width, titleImageMaxSize)
        maximumSize = Dimension(Int.MAX_VALUE, titleImageMaxSize)
        alignmentX = LEFT_ALIGNMENT
        isVisible = true

        subscribeTo(pdf to listOf(TitleImageChanged::class))
    }

    private fun createAndShowEditDialog() {
        val owner = SwingUtilities.getRoot(this) as Frame
        edt {
            JPDFDocumentEditView(owner, pdf).isVisible = true
        }
    }

    override fun update(event: ObservableEvent) = when (event) {
        TitleImageChanged -> {
            currentTitleImage.repaintWith(getCurrentTitleImagePreview())
            edt {
                validate()
                repaint()
            }
        }
        else -> doNothing()
    }

    override fun getEvent() = PDFWasRemoved(this)
}