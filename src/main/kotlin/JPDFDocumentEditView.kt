import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.*
import javax.imageio.ImageIO
import javax.swing.*

class JPDFDocumentEditView(owner: Frame, private val pdf: PDFDocument) : JDialog(owner, pdf.fileName) {
    companion object {
        private val loadingImage = ImageIO.read(PDFDocument::class.java.getResource("loading.gif"))
    }

    open class JSelectablePanel : JPanel() {
        val blueBorder = BorderFactory.createLineBorder(Color.BLUE)
        var isSelected = false
            set(value) {
                field = value
                edt {
                    border = if (value) blueBorder else BorderFactory.createEmptyBorder()
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

    class SelectionsManager {
        lateinit var pagesOrder: List<JSelectablePanel>
        var latestSelectedPageIndex = 0
        val selectedPages = LinkedHashSet<JSelectablePanel>()

        fun toggleSelection(item: JSelectablePanel) {
            if (item.toggleSelect()) {
                selectedPages.add(item)
            } else {
                selectedPages.remove(item)
            }
            latestSelectedPageIndex = pagesOrder.indexOf(selectedPages.last())
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
            selectRange(latestSelectedPageIndex, itemIndex)
            latestSelectedPageIndex = itemIndex
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

    class JPagePreview(pageIndex: Int, img: Image, selectionsManager: SelectionsManager) : JSelectablePanel() {
        private val thumbnail = JImage(img)

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

        internal fun repaintWith(image: Image) = this.thumbnail.repaintWith(image)
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var selectionsManager = SelectionsManager()
    private val pagesPreviews =
        pdf.getCurrentState().pages.keys.associateWith { JPagePreview(it, loadingImage, selectionsManager) }

    init {
        selectionsManager.pagesOrder = pagesPreviews.values.toList()

        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = scope.coroutineContext.cancelChildren()
        })

        add(JImage(pdf.currentTitleImage.fit(600)))
        add(Box.createRigidArea(Dimension(0, 5)))
        add(JScrollPane(JPanel(FlowLayout()).also { panel -> pagesPreviews.values.forEach { panel.add(it) } }))
        loadPagesThumbnails(scope)
    }

    private fun loadPagesThumbnails(scope: CoroutineScope) {
        for ((pageIndex, rotation) in pdf.getCurrentState().pages) {
            scope.launch {
                val image = pdf.getPageThumbnail(pageIndex).rotate(rotation.angle.toDouble())
                pagesPreviews[pageIndex]?.repaintWith(image)
            }
        }
    }
}