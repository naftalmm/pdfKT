import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import java.nio.file.Path
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.TransferHandler
import javax.swing.UIManager
import java.awt.event.KeyEvent.VK_F as F

const val DEFAULT_WIDTH = 1000
const val DEFAULT_HEIGHT = 750

class App : JFrame(), Observer {
    private val pdfsList = JPDFsList()
    private val workspace = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(JScrollPane(pdfsList).apply {
            alignmentX = Component.LEFT_ALIGNMENT
        })
        add(JButton("Save as PDF...").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            preferredSize *= 2
            addActionListener {
                val fc = JPDFFileChooser().apply { currentDirectory = fcCurrentDirectory }
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    PDFTKSaver(pdfsList.getCurrentPDFsState()).saveTo(fc.selectedFile.toPath())
                }
                fcCurrentDirectory = fc.currentDirectory
            }
        })
    }
    private val dropPDFsLabel = JLabel("Drop PDFs here").apply {
        horizontalAlignment = SwingConstants.CENTER
        verticalAlignment = SwingConstants.CENTER
    }
    private var fcCurrentDirectory: File? = null
    private val cardLayout = JPanelCardLayout().apply {
        add(dropPDFsLabel)
        add(workspace)
    }

    init {
        title = "pdfKT"
        addMenuBar()
        add(cardLayout)

        subscribeTo(pdfsList)
        transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean =
                support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support)) return false

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
        isVisible = true
    }

    private fun addMenuBar() {
        jMenuBar = JMenuBar().apply {
            add(JMenu("File").apply {
                mnemonic = F
                add(JMenuItem("Add PDFs...").apply {
                    val ctrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK)
                    accelerator = ctrlO

                    addActionListener {
                        val fc = JPDFFileChooser().apply {
                            isMultiSelectionEnabled = true
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
        FirstPDFWasAdded -> edt { cardLayout.show(workspace) }
        AllPDFsWereRemoved -> edt { cardLayout.show(dropPDFsLabel) }
        else -> doNothing()
    }
}

private fun createAndShowGUI() = edt {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    App()
}

fun main() = createAndShowGUI()

fun edt(runnable: () -> Unit) {
    EventQueue.invokeLater(runnable)
}

fun Container.addAll(components: Iterable<Component>) = components.forEach { add(it) }

private operator fun Dimension.times(i: Int) = Dimension(this.width * i, this.height * i)

fun Path.deleteOnExit() = this.toFile().deleteOnExit()