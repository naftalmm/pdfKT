import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.Border
import kotlin.reflect.KClass

class JPDFDocumentEditView(owner: Frame, private val pdf: PDFDocumentEditModel) : JDialog(owner, pdf.fileName),
    Observer {
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

    class SelectionsManager : MultiObservable {
        override val allEventsSubscribers: MutableList<Observer> = ArrayList()
        override val subscribers: MutableMap<KClass<out ObservableEvent>, MutableList<Observer>> = hashMapOf()
        lateinit var pagesOrder: List<JPagePreview>
        private var latestSelectedPageIndexInPagesOrderList = 0
            set(value) {
                field = value
                notifySubscribers(PageSelected(getLatestSelectedPageIndex()))
            }

        private fun getLatestSelectedPageIndex(): Int = pagesOrder[latestSelectedPageIndexInPagesOrderList].pageIndex

        val selectedPages = LinkedHashSet<JSelectablePanel>()

        fun toggleSelection(item: JSelectablePanel) {
            if (item.toggleSelect()) {
                selectedPages.add(item)
            } else {
                selectedPages.remove(item)
            }

            when (selectedPages.size) {
                0 -> notifySubscribers(AllPagesWereUnSelected)
                1 -> notifySubscribers(FirstPageWasSelected)
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
    private var selectionsManager = SelectionsManager()
    private val pagesPreviews = pdf.getCurrentPagesThumbnails(scope)
        .map { (pageIndex, thumbnail) -> JPagePreview(pageIndex, thumbnail, selectionsManager) }
    private val pagesPreviewsPanel = JPanel(FlowLayout()).apply { addAll(pagesPreviews) }
    private val selectionDependentButtons = ArrayList<JButton>()

    init {
        selectionsManager.pagesOrder = pagesPreviews.toList()

        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        setSize(DEFAULT_WIDTH, getScreenHeightWithoutTaskBar())
        setLocationRelativeTo(null)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = scope.coroutineContext.cancelChildren()
        })

        add(currentPageImageView)
        add(Box.createRigidArea(Dimension(0, 5)))
        add(JScrollPane(pagesPreviewsPanel).apply {
            preferredSize = preferredSize //to set isPreferredSizeSet=true
            maximumSize = preferredSize
        })
        add(JPanel().apply {
            add(JButton("Rotate all counter-clockwise").apply { addActionListener { } })
            add(JButton("Rotate counter-clockwise").apply {
                isEnabled = false
                selectionDependentButtons.add(this)
                addActionListener { }
            })
            add(JButton("Remove selected").apply {
                isEnabled = false
                selectionDependentButtons.add(this)
                addActionListener { }
            })
            add(JButton("Rotate clockwise").apply {
                isEnabled = false
                selectionDependentButtons.add(this)
                addActionListener { }
            })
            add(JButton("Rotate  all clockwise").apply { addActionListener { } })
        })

        subscribeTo(selectionsManager, pdf)
    }

    private fun getScreenHeightWithoutTaskBar(): Int {
        val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
        val taskBarHeight = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration).bottom
        return screenSize.height - taskBarHeight
    }

    override fun update(event: ObservableEvent) = when (event) {
        is PageSelected -> repaintCurrentPageImageViewWith(event.pageIndex)
        ThumbnailLoaded -> edt { pagesPreviewsPanel.updateUI() }
        AllPagesWereUnSelected -> edt { selectionDependentButtons.forEach { it.isEnabled = false } }
        FirstPageWasSelected -> edt { selectionDependentButtons.forEach { it.isEnabled = true } }
    }

    private fun repaintCurrentPageImageViewWith(pageIndex: Int) =
        currentPageImageView.repaintWith(pdf.getCurrentPageImage(pageIndex).fit(currentImageMaxDimension))
}