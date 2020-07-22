import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.Image
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.LinkedHashMap

enum class Rotation(val angle: Int) {
    NORTH(0), EAST(90), SOUTH(180), WEST(270)
}

data class DocumentState(val pages: LinkedHashMap<Int, Rotation>)

class PDFDocumentEditModel(val pdf: PDFDocument) {
    companion object {
        //TODO разобраться, почему gif не двигается
        //TODO подрезать размер gif
        private val loadingImage = ImageIO.read(PDFDocument::class.java.getResource("loading.gif"))
    }

    private val statesStack: LinkedList<DocumentState>
    val fileName = pdf.fileName
    var currentTitleImageThumbnail: JImage

    init {
        statesStack = initStatesStack()
        currentTitleImageThumbnail = initTitleImage()
    }

    private fun initTitleImage() = JImage(pdf.getPageImage(0).fit(50))

    fun removePages(indexes: Set<Int>) {
        val prevState = statesStack.last()
        val newState =
            DocumentState(prevState.pages.filterKeys { indexes.contains(it) } as LinkedHashMap<Int, Rotation>)
        statesStack.add(newState)
        changeTitleImageThumbnail(prevState, newState)
    }

    fun rotatePages(indexes: Set<Int>, rotation: Rotation) {
        val prevState = statesStack.last()
        val newState =
            DocumentState(prevState.pages
                .map { (k, v) -> if (indexes.contains(k)) k to rotation else k to v }
                .toMap(LinkedHashMap()))
        statesStack.add(newState)
        changeTitleImageThumbnail(prevState, newState)
    }

    fun restorePreviousState() {
        if (statesStack.size > 1) {
            val prevState = statesStack.pop()
            changeTitleImageThumbnail(prevState, statesStack.peek())
        }
    }

    private fun changeTitleImageThumbnail(prevState: DocumentState, newState: DocumentState) {
        val prevTitlePageState = prevState.pages.first()
        val (newTitlePageIndex, newTitlePageRotation) = newState.pages.first() ?: return

        if (prevTitlePageState != null) {
            val (prevTitlePageIndex, prevTitlePageRotation) = prevTitlePageState
            if (prevTitlePageIndex == newTitlePageIndex && prevTitlePageRotation == newTitlePageRotation) {
                return
            }
        }

        currentTitleImageThumbnail.repaintWith(pdf.getPageImage(newTitlePageIndex).fit(50).rotate(newTitlePageRotation))
    }

    private fun initStatesStack() = LinkedList<DocumentState>()
        .also { it.push(DocumentState((0 until pdf.numberOfPages).associateWithTo(LinkedHashMap()) { Rotation.NORTH })) }

    private fun getCurrentState(): DocumentState = statesStack.peek()

    fun getCurrentTitleImage(): Image {
        val (currentTitleImageIndex, currentTitleImageRotation) = getCurrentState().pages.first()!!
        return pdf.getPageImage(currentTitleImageIndex).rotate(currentTitleImageRotation)
    }

    fun getCurrentPageImage(pageIndex: Int): Image = pdf.getPageImage(pageIndex).rotate(getCurrentPageRotation(pageIndex))

    private fun getCurrentPageRotation(pageIndex: Int): Rotation = getCurrentState().pages[pageIndex] ?: Rotation.NORTH

    fun getCurrentPagesThumbnails(scope: CoroutineScope): Map<Int, JImage> =
        getCurrentState().pages.map { (pageIndex, rotation) ->
            val pagePreview = JImage(loadingImage)
            scope.launch { pagePreview.repaintWith(getPageThumbnail(pageIndex).rotate(rotation)) }
            pageIndex to pagePreview
        }.toMap()

    private fun getPageThumbnail(pageIndex: Int): Image = pdf.getPageThumbnail(pageIndex)
}
