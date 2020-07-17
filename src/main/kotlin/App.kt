import java.awt.EventQueue
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.JFrame
import javax.swing.TransferHandler

const val DEFAULT_WIDTH = 1000
const val DEFAULT_HEIGHT = 750

class App(title: String) : JFrame() {
    init {
        createUI(title)
    }

    private fun createUI(title: String) {
//        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        setTitle(title)
        val pdfsList = JPDFsList(this)
        add(pdfsList)
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