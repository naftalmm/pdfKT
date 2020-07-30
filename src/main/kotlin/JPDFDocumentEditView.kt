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
        private val panelsOrder: LinkedHashMap<JSelectablePanel, Int> = LinkedHashMap()
        private var latestSelectedPanel: JSelectablePanel? = null
            set(value) {
                field = value
                notifySubscribers(PanelSelected(value ?: panelsOrder.asIterable().first().key))
            }

        val selectedPanels = LinkedHashSet<JSelectablePanel>()

        fun setPanelsOrder(panelsOrder: List<JSelectablePanel>, preserveSelection: Boolean = false) {
            val selectedPanelsIndexes =
                if (preserveSelection) selectedPanels.map { this.panelsOrder[it] ?: return } else emptyList()

            with(this.panelsOrder) {
                clear()
                panelsOrder.withIndex().forEach { this[it.value] = it.index }
            }

            selectedPanels.clear()
            if (!preserveSelection) {
                notifySubscribers(AllPagesWereUnSelected)
            } else {
                selectedPanelsIndexes.map { panelsOrder[it] }.forEach {
                    it.select()
                    selectedPanels.add(it)
                }
            }

            latestSelectedPanel = selectedPanels.lastOrNull()
        }

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

            latestSelectedPanel = selectedPanels.lastOrNull()
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
            val fromIndexInclusive = panelsOrder[latestSelectedPanel]
            if (fromIndexInclusive == null) {
                toggleSelection(item)
                return
            }

            val iterator = panelsOrder.keys.toList().listIterator(fromIndexInclusive)
            val reversed = fromIndexInclusive > panelsOrder[item]!!

            var it = latestSelectedPanel
            while (it != item) {
                it = if (reversed) iterator.previous() else iterator.next()
                it.select()
                selectedPanels.add(it)
                if (it == item) break
            }

            latestSelectedPanel = item
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
    private lateinit var pagesPreviews: List<JPagePreview>
    private val pagesPreviewsPanel = JPanel(FlowLayout())
    private val selectionDependentButtons = ArrayList<JButton>()

    init {
        initPagesPreviews()

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
            add(JButton("Rotate all counter-clockwise").apply {
                addActionListener {
                    pdf.rotateAllPagesCounterClockwise()
                    setPagesPreviews(preserveSelection = true)
                }
            })
            add(JButton("Rotate counter-clockwise").apply {
                isEnabled = false
                selectionDependentButtons.add(this)
                addActionListener {
                    pdf.rotatePagesCounterClockwise(getSelectedPagesIndexes())
                    setPagesPreviews(preserveSelection = true)
                }
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
                addActionListener {
                    pdf.rotatePagesClockwise(getSelectedPagesIndexes())
                    setPagesPreviews(preserveSelection = true)
                }
            })
            add(JButton("Rotate all clockwise").apply {
                addActionListener {
                    pdf.rotateAllPagesClockwise()
                    setPagesPreviews(preserveSelection = true)
                }
            })
        })

        subscribeTo(selectionsManager, pdf)
    }

    private fun initPagesPreviews() {
        pagesPreviews = getCurrentPagesPreviews()
        pagesPreviewsPanel.addAll(pagesPreviews)
        selectionsManager.setPanelsOrder(pagesPreviews)
    }

    private fun getCurrentPagesPreviews() = pdf.getCurrentPagesThumbnails(scope)
        .map { (pageIndex, thumbnail) -> JPagePreview(pageIndex, thumbnail, selectionsManager) }

    private fun setPagesPreviews(preserveSelection: Boolean = false) {
        pagesPreviews = getCurrentPagesPreviews()
        edt {
            with(pagesPreviewsPanel) {
                removeAll()
                addAll(pagesPreviews)
                validate()
                repaint()
            }
        }
        selectionsManager.setPanelsOrder(pagesPreviews, preserveSelection)
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

    private fun repaintCurrentPageImageViewWith(pageIndex: Int) {
        currentPageImageView.repaintWith(pdf.getCurrentPageImage(pageIndex).fit(currentImageMaxDimension))
        edt {
            validate()
            repaint()
        }
    }
}