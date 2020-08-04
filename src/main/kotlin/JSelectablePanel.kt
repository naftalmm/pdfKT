import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.border.Border

open class JSelectablePanel : JPanel() {
    companion object {
        private val blueBorder: Border =
            BorderFactory.createLineBorder(Color.BLUE)
        private val emptyBorder: Border =
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
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