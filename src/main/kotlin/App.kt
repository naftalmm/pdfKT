import java.awt.CardLayout
import java.awt.Component
import java.awt.Container
import java.awt.EventQueue
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.TransferHandler
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.event.KeyEvent.VK_F as F

const val DEFAULT_WIDTH = 1000
const val DEFAULT_HEIGHT = 750

class MyCardLayout : JPanel(CardLayout()) {
    private val componentPseudonyms = WeakHashMap<Component, String>()
    private var counter = 0

    fun show(comp: Component) {
        (this.layout as CardLayout).show(this, componentPseudonyms[comp])
    }

    override fun add(comp: Component): Component {
        synchronized(this) {
            val pseudonym = componentPseudonyms.getOrPut(comp) { counter++.toString() }
            super.add(comp, pseudonym)
            return comp
        }
    }
}

class App(title: String) : JFrame(), Observer {
    private val pdfsList = JPDFsList()
    private val dropPDFsLabel = JLabel("Drop PDFs here").apply {
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.CENTER
    }
    private val cardLayout = MyCardLayout().apply {
        add(dropPDFsLabel)
        add(pdfsList)
    }
    private var fcCurrentDirectory: File? = null
    init {
        createUI(title)
    }

    private fun createUI(title: String) {
//        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        setTitle(title)
        addMenuBar()
        add(cardLayout)

        subscribeTo(pdfsList)
        transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean =
                support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support)) {
                    return false
                }
                try {
                    @Suppress("UNCHECKED_CAST")
                    (support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>)
                        .filter { it.extension.equals("pdf", ignoreCase = true) }
                        .also { pdfsList.addPDFDocuments(it) }
                } catch (e: Exception) {
                    System.err.println(e)
                    return false
                }

                return true
            }
        }
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT)
        setLocationRelativeTo(null)
    }

    private fun addMenuBar() {
        jMenuBar = JMenuBar().apply {
            add(JMenu("File").apply {
                mnemonic = F
                add(JMenuItem("Add PDFs...").apply {
                    val ctrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK)
                    accelerator = ctrlO
                    addActionListener {
                        val fc = JFileChooser().apply {
                            isMultiSelectionEnabled = true
                            fileFilter = FileNameExtensionFilter("PDF files", "pdf")
                            currentDirectory = fcCurrentDirectory
                        }
                        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                            pdfsList.addPDFDocuments(fc.selectedFiles.asIterable())
                        }
                        fcCurrentDirectory = fc.currentDirectory
                    }
                })
            })
        }
    }

    override fun update(event: ObservableEvent) = when (event) {
        FirstPDFWasAdded -> edt { cardLayout.show(pdfsList) }
        AllPDFsWereRemoved -> edt { cardLayout.show(dropPDFsLabel) }
        else -> doNothing()
    }
}

private fun createAndShowGUI() {
    edt {
        val frame = App("pdfKT")
        frame.isVisible = true
    }
}

fun main() {
    createAndShowGUI()
}

fun edt(runnable: () -> Unit) {
    EventQueue.invokeLater(runnable)
}

fun Container.addAll(components: Iterable<Component>) = components.forEach { add(it) }