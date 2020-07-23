import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.Border

class JPDFDocumentEditView(owner: Frame, private val pdf: PDFDocumentEditModel) : JDialog(owner, pdf.fileName) {
    open class JSelectablePanel : JPanel() {
        companion object {
            private val blueBorder: Border = BorderFactory.createLineBorder(Color.BLUE)
            private val emptyBorder: Border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        }

        init {
            border = emptyBorder
        }

        private var isSelected = false
            set(value) {
                field = value
                edt {
                    border = if (value) blueBorder else emptyBorder
                }
            }

        fun toggleSelect(): Boolean {
            isSelected = !isSelected
            return isSelected
        }

        fun select() {
            isSelected = true
        }

        fun unselect() {
            isSelected = false
        }
    }

    class SelectionsManager(private val jpdfDocumentEditView: JPDFDocumentEditView) {
        lateinit var pagesOrder: List<JPagePreview>
        private var latestSelectedPageIndexInPagesOrderList = 0
            set(value) {
                jpdfDocumentEditView.repaintCurrentPageImageViewWith(pagesOrder[value].pageIndex)
                field = value
            }
        val selectedPages = LinkedHashSet<JSelectablePanel>()

        fun toggleSelection(item: JSelectablePanel) {
            if (item.toggleSelect()) {
                selectedPages.add(item)
            } else {
                selectedPages.remove(item)
            }
            latestSelectedPageIndexInPagesOrderList =
                if (selectedPages.isNotEmpty()) pagesOrder.indexOf(selectedPages.last()) else 0
        }

        fun setSelection(item: JSelectablePanel) {
            clearSelection()
            toggleSelection(item)
        }

        private fun clearSelection() {
            selectedPages.forEach { it.unselect() }
            selectedPages.clear()
        }

        fun rangeSelectFromLatestSelectedTo(item: JSelectablePanel) {
            if (selectedPages.isEmpty()) {
                toggleSelection(item)
                return
            }

            val itemIndex = pagesOrder.indexOf(item)
            selectRange(latestSelectedPageIndexInPagesOrderList, itemIndex)
            latestSelectedPageIndexInPagesOrderList = itemIndex
        }

        private fun selectRange(fromIndexInclusive: Int, toIndexInclusive: Int) {
            val range =
                if (fromIndexInclusive < toIndexInclusive) fromIndexInclusive..toIndexInclusive
                else (fromIndexInclusive downTo toIndexInclusive)
            range.map { pagesOrder[it] }.forEach {
                it.select()
                selectedPages.add(it)
            }
        }
    }

    class JPagePreview(val pageIndex: Int, thumbnail: JImage, selectionsManager: SelectionsManager) :
        JSelectablePanel() {
        init {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(thumbnail)
            add(JLabel((pageIndex + 1).toString()))
            addMouseListener(object : MouseListener {
                fun MouseEvent.isCtrlClick(): Boolean {
                    val ctrlClick = InputEvent.BUTTON1_MASK or InputEvent.CTRL_MASK
                    return modifiers and ctrlClick == ctrlClick
                }

                fun MouseEvent.isShiftClick(): Boolean {
                    val shiftClick = InputEvent.BUTTON1_MASK or InputEvent.SHIFT_MASK
                    return modifiers and shiftClick == shiftClick
                }

                override fun mouseEntered(e: MouseEvent) {}

                override fun mousePressed(e: MouseEvent) {}

                override fun mouseReleased(e: MouseEvent) {}

                override fun mouseClicked(e: MouseEvent) {
                    when {
                        e.isShiftClick() -> selectionsManager.rangeSelectFromLatestSelectedTo(this@JPagePreview)
                        e.isCtrlClick() -> selectionsManager.toggleSelection(this@JPagePreview)
                        else -> selectionsManager.setSelection(this@JPagePreview)
                    }
                }

                override fun mouseExited(e: MouseEvent) {}
            })
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val currentImageMaxDimension = 800
    private val currentPageImageView = JImage(pdf.getCurrentTitleImage().fit(currentImageMaxDimension))
    private var selectionsManager = SelectionsManager(this)
    private val pagesPreviews = pdf.getCurrentPagesThumbnails(scope)
        .map { (pageIndex, thumbnail) -> JPagePreview(pageIndex, thumbnail, selectionsManager) }

    init {
        selectionsManager.pagesOrder = pagesPreviews.toList()

        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = scope.coroutineContext.cancelChildren()
        })

        add(currentPageImageView)
        add(Box.createRigidArea(Dimension(0, 5)))
        add(JScrollPane(JPanel(FlowLayout()).apply { addAll(pagesPreviews) }))
    }

    fun repaintCurrentPageImageViewWith(pageIndex: Int) =
        currentPageImageView.repaintWith(pdf.getCurrentPageImage(pageIndex).fit(currentImageMaxDimension))
}