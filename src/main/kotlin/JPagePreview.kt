import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JLabel

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

            override fun mouseClicked(e: MouseEvent) = with(e) {
                when {
                    isShiftClick() -> selectionsManager.rangeSelectFromLatestSelectedTo(this@JPagePreview)
                    isCtrlClick() -> selectionsManager.toggleSelection(this@JPagePreview)
                    else -> selectionsManager.setSelection(this@JPagePreview)
                }
            }
        })
    }
}