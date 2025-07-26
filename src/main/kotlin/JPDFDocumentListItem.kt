import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class JPDFDocumentListItem(val pdf: PDFDocumentEditModel) :
    JPanel(), Observer, Observable<PDFWasRemoved> by ObservableImpl() {
    companion object {
        private const val titleImageMaxSize = 50
    }

    private val currentTitleImage = JImage(getCurrentTitleImagePreview())
    private val editButton = JButton("Edit").apply {
        addActionListener {
            isEnabled = false
            createAndShowEditDialog()
        }
    }

    private fun getCurrentTitleImagePreview() = pdf.getCurrentTitleImage().fit(titleImageMaxSize)

    init {
        layout = FlowLayout(FlowLayout.LEFT)
        add(currentTitleImage)
        add(JLabel(pdf.fileName))
        add(editButton)
        val event = PDFWasRemoved(this)
        add(JButton("Delete").apply { addActionListener { notifySubscribers(event) } })
        preferredSize = Dimension(size.width, titleImageMaxSize)
        maximumSize = Dimension(Int.MAX_VALUE, titleImageMaxSize)
        alignmentX = LEFT_ALIGNMENT
        isVisible = true

        subscribeTo(pdf to listOf(TitleImageChanged::class))
    }

    private fun createAndShowEditDialog() {
        val owner = SwingUtilities.getRoot(this) as Frame
        edt {
            val editView = JPDFDocumentEditView(owner, pdf).apply {
                isVisible = true
            }
            subscribeTo(editView)
        }
    }

    override fun update(event: ObservableEvent) {
        when (event) {
            TitleImageChanged -> {
                currentTitleImage.repaintWith(getCurrentTitleImagePreview())
                edt {
                    validate()
                    repaint()
                }
            }
            is WindowWasClosed if event.window is JPDFDocumentEditView -> {
                editButton.isEnabled = true
            }
        }
    }
}