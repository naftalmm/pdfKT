import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.SHIFT_DOWN_MASK
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
            fun MouseEvent.isCtrlClick() = button == 1 && modifiersEx == CTRL_DOWN_MASK
            fun MouseEvent.isShiftClick() = button == 1 && modifiersEx == SHIFT_DOWN_MASK

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