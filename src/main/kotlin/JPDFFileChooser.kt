import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

class JPDFFileChooser : JFileChooser() {
    init {
        fileFilter = FileNameExtensionFilter("PDF files", "pdf")
    }

    override fun approveSelection() {
        if (dialogType == SAVE_DIALOG && selectedFile.exists()) {
            val result = JOptionPane.showConfirmDialog(
                this,
                "The file exists, overwrite?",
                "Existing file", JOptionPane.YES_NO_CANCEL_OPTION)
            when (result) {
                JOptionPane.YES_OPTION -> super.approveSelection()
                JOptionPane.CANCEL_OPTION -> cancelSelection()
                else -> return
            }
        } else {
            super.approveSelection()
        }
    }
}