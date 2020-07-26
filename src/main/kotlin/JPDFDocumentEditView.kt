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
        private val panelsOrder: MutableList<JSelectablePanel> = ArrayList()

        fun setPanelsOrder(panelsOrder: List<JSelectablePanel>) {
            with(this.panelsOrder) {
                clear()
                addAll(panelsOrder)
            }

            selectedPanels.clear()
            notifySubscribers(AllPagesWereUnSelected)
            latestSelectedPanelIndex = 0
        }

        private var latestSelectedPanelIndex = 0
            set(value) {
                field = value
                notifySubscribers(PanelSelected(panelsOrder[value]))
            }

        val selectedPanels = LinkedHashSet<JSelectablePanel>()

        fun toggleSelection(item: JSelectablePanel) {
            if (item.toggleSelect()) {
                selectedPanels.add(item)
            } else {
                selectedPanels.remove(item)
            }

            when (selectedPanels.size) {
                0 -> notifySubscribers(AllPagesWereUnSelected)
                1 -> notifySubscribers(FirstPageWasSelected)
            }

            latestSelectedPanelIndex =
                if (selectedPanels.isNotEmpty()) panelsOrder.indexOf(selectedPanels.last()) else 0
        }

        fun setSelection(item: JSelectablePanel) {
            clearSelection()
            toggleSelection(item)
        }

        private fun clearSelection() {
            selectedPanels.forEach { it.unselect() }
            selectedPanels.clear()
        }

        fun rangeSelectFromLatestSelectedTo(item: JSelectablePanel) {
            if (selectedPanels.isEmpty()) {
                toggleSelection(item)
                return
            }

            val itemIndex = panelsOrder.indexOf(item)
            selectRange(latestSelectedPanelIndex, itemIndex)
            latestSelectedPanelIndex = itemIndex
        }

        private fun selectRange(fromIndexInclusive: Int, toIndexInclusive: Int) {
            val range =
                if (fromIndexInclusive < toIndexInclusive) fromIndexInclusive..toIndexInclusive
                else (fromIndexInclusive downTo toIndexInclusive)
            range.map { panelsOrder[it] }.forEach {
                it.select()
                selectedPanels.add(it)
            }
        }
    }

    class JPagePreview(val pageIndex: Int, thumbnail: JImage, selectionsManager: SelectionsManager) :
        JSelectablePanel() {
        init {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(thumbnail)
            add(JLabel((pageIndex + 1).toString()))
            addMouseListener(object : MouseAdapter() {
                fun MouseEvent.isCtrlClick(): Boolean {
                    val ctrlClick = InputEvent.BUTTON1_MASK or InputEvent.CTRL_MASK
                    return modifiers and ctrlClick == ctrlClick
                }

                fun MouseEvent.isShiftClick(): Boolean {
                    val shiftClick = InputEvent.BUTTON1_MASK or InputEvent.SHIFT_MASK
                    return modifiers and shiftClick == shiftClick
                }

                override fun mouseClicked(e: MouseEvent) {
                    when {
                        e.isShiftClick() -> selectionsManager.rangeSelectFromLatestSelectedTo(this@JPagePreview)
                        e.isCtrlClick() -> selectionsManager.toggleSelection(this@JPagePreview)
                        else -> selectionsManager.setSelection(this@JPagePreview)
                    }
                }
            })
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val currentImageMaxDimension = 800
    private val currentPageImageView = JImage(pdf.getCurrentTitleImage().fit(currentImageMaxDimension))
    private val selectionsManager = SelectionsManager()
    private var pagesPreviews = getCurrentPagesPreviews()
    private val pagesPreviewsPanel = JPanel(FlowLayout()).apply { addAll(pagesPreviews) }
    private val selectionDependentButtons = ArrayList<JButton>()

    init {
        selectionsManager.setPanelsOrder(pagesPreviews)

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
                addActionListener {
                    pdf.removePages(getSelectedPagesIndexes())
                    setPagesPreviews()
                }
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

    private fun getCurrentPagesPreviews() = pdf.getCurrentPagesThumbnails(scope)
        .map { (pageIndex, thumbnail) -> JPagePreview(pageIndex, thumbnail, selectionsManager) }

    private fun setPagesPreviews() {
        pagesPreviews = getCurrentPagesPreviews()
        edt {
            with(pagesPreviewsPanel) {
                removeAll()
                addAll(pagesPreviews)
                validate()
                repaint()
            }
        }
        selectionsManager.setPanelsOrder(pagesPreviews)
    }

    private fun getSelectedPagesIndexes() =
        selectionsManager.selectedPanels.map { it as JPagePreview }.map { it.pageIndex }.toSet()

    private fun getScreenHeightWithoutTaskBar(): Int {
        val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
        val taskBarHeight = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration).bottom
        return screenSize.height - taskBarHeight
    }

    override fun update(event: ObservableEvent) = when (event) {
        is PanelSelected -> repaintCurrentPageImageViewWith((event.panel as JPagePreview).pageIndex)
        ThumbnailLoaded -> edt { pagesPreviewsPanel.updateUI() }
        AllPagesWereUnSelected -> edt { selectionDependentButtons.forEach { it.isEnabled = false } }
        FirstPageWasSelected -> edt { selectionDependentButtons.forEach { it.isEnabled = true } }
    }

    private fun repaintCurrentPageImageViewWith(pageIndex: Int) =
        currentPageImageView.repaintWith(pdf.getCurrentPageImage(pageIndex).fit(currentImageMaxDimension))
}