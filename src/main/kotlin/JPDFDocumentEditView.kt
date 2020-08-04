import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Frame
import java.awt.Toolkit
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JScrollPane

class JPDFDocumentEditView(owner: Frame, private val pdf: PDFDocumentEditModel) : JDialog(owner, pdf.fileName),
    Observer {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val currentPageView = JCurrentPageView(pdf)
    private val selectionsManager = SelectionsManager()
    private lateinit var pagesPreviews: List<JPagePreview>
    private val pagesPreviewsPanel = JPanel(FlowLayout())
    private val selectionDependentButtons = ArrayList<JButton>()

    init {
        initPagesPreviews()

        setSize(DEFAULT_WIDTH, getScreenHeightWithoutTaskBar())
        setLocationRelativeTo(null)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = scope.coroutineContext.cancelChildren()
        })

        isFocusable = true
        addKeyListener(object : KeyAdapter() {
            fun KeyEvent.isCtrlZ(): Boolean = isControlDown && keyCode == KeyEvent.VK_Z
            fun KeyEvent.isCtrlA(): Boolean = isControlDown && keyCode == KeyEvent.VK_A
            fun KeyEvent.isShiftHome(): Boolean = isShiftDown && keyCode == KeyEvent.VK_HOME
            fun KeyEvent.isShiftEnd(): Boolean = isShiftDown && keyCode == KeyEvent.VK_END

            override fun keyPressed(e: KeyEvent) = with(e) {
                when {
                    isCtrlZ() -> {
                        pdf.restorePreviousState()
                        refreshPagesPreviews()
                    }
                    isCtrlA() -> selectionsManager.selectAll()
                    isShiftHome() -> selectionsManager.selectAllFromLatestSelectedToFirst()
                    isShiftEnd() -> selectionsManager.selectAllFromLatestSelectedToLast()
                }
            }
        })

        layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)
        add(currentPageView)
        add(Box.createRigidArea(Dimension(0, 5)))
        add(JScrollPane(pagesPreviewsPanel))
        add(JPanel().apply {
            add(JButton("Rotate all counter-clockwise").apply {
                isFocusable = false
                addActionListener {
                    pdf.rotateAllPagesCounterClockwise()
                    refreshPagesPreviews(preserveSelection = true)
                }
            })
            add(JButton("Rotate counter-clockwise").apply {
                isFocusable = false
                isEnabled = false
                selectionDependentButtons.add(this)
                addActionListener {
                    pdf.rotatePagesCounterClockwise(getSelectedPagesIndexes())
                    refreshPagesPreviews(preserveSelection = true)
                }
            })
            add(JButton("Remove selected").apply {
                isFocusable = false
                isEnabled = false
                selectionDependentButtons.add(this)
                addActionListener {
                    pdf.removePages(getSelectedPagesIndexes())
                    refreshPagesPreviews()
                }
            })
            add(JButton("Rotate clockwise").apply {
                isFocusable = false
                isEnabled = false
                selectionDependentButtons.add(this)
                addActionListener {
                    pdf.rotatePagesClockwise(getSelectedPagesIndexes())
                    refreshPagesPreviews(preserveSelection = true)
                }
            })
            add(JButton("Rotate all clockwise").apply {
                isFocusable = false
                addActionListener {
                    pdf.rotateAllPagesClockwise()
                    refreshPagesPreviews(preserveSelection = true)
                }
            })
        })

        subscribeTo(selectionsManager)
        subscribeTo(pdf to listOf(ThumbnailLoaded::class))
    }

    private fun initPagesPreviews() {
        pagesPreviews = getCurrentPagesPreviews()
        pagesPreviewsPanel.addAll(pagesPreviews)
        selectionsManager.setPanelsOrder(pagesPreviews)
    }

    private fun getCurrentPagesPreviews() = pdf.getCurrentPagesThumbnails(scope)
        .map { (pageIndex, thumbnail) -> JPagePreview(pageIndex, thumbnail, selectionsManager) }

    private fun refreshPagesPreviews(preserveSelection: Boolean = false) {
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
        is PanelSelected -> currentPageView.setCurrentPage((event.panel as JPagePreview).pageIndex)
        ThumbnailLoaded -> edt { pagesPreviewsPanel.updateUI() }
        AllPagesWereUnSelected -> edt { selectionDependentButtons.forEach { it.isEnabled = false } }
        FirstPageWasSelected -> edt { selectionDependentButtons.forEach { it.isEnabled = true } }
        else -> doNothing()
    }.also {
        edt {
            validate()
            repaint()
        }
    }
}