import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.Image
import java.util.LinkedList
import javax.swing.ImageIcon

enum class Rotation(val angle: Int) {
    NORTH(0), EAST(90), SOUTH(180), WEST(270);

    fun rotateClockwise() = when (this) {
        NORTH -> EAST
        EAST -> SOUTH
        SOUTH -> WEST
        WEST -> NORTH
    }

    fun rotateCounterClockwise() = when (this) {
        NORTH -> WEST
        EAST -> NORTH
        SOUTH -> EAST
        WEST -> SOUTH
    }
}

data class DocumentState(val pages: LinkedHashMap<Int, Rotation>)

class PDFDocumentEditModel(val pdf: PDFDocument) : MultiObservable by MultiObservableImpl() {
    companion object {
        private val loadingImage = ImageIcon(PDFDocument::class.java.getResource("loading.gif")).image
    }

    private val statesStack: LinkedList<DocumentState> = initStatesStack()
    val fileName = pdf.fileName

    private fun initStatesStack() = LinkedList<DocumentState>()
        .apply { push(DocumentState((0 until pdf.numberOfPages).associateWithTo(linkedMapOf()) { Rotation.NORTH })) }

    fun removePages(indexes: Set<Int>) {
        val newState = DocumentState(getCurrentState().pages.filterNotTo(linkedMapOf()) { indexes.contains(it.key) })
        changeState(newState)
    }

    fun rotateAllPagesClockwise() {
        val newState = DocumentState(getCurrentState().pages
            .mapValuesTo(linkedMapOf()) { (_, v) -> v.rotateClockwise() })
        changeState(newState)
    }

    fun rotatePagesClockwise(indexes: Set<Int>) {
        val newState = DocumentState(getCurrentState().pages
            .mapValuesTo(linkedMapOf()) { (k, v) -> if (indexes.contains(k)) v.rotateClockwise() else v })
        changeState(newState)
    }

    fun rotateAllPagesCounterClockwise() {
        val newState = DocumentState(getCurrentState().pages
            .mapValuesTo(linkedMapOf())  { (_, v) -> v.rotateCounterClockwise() })
        changeState(newState)
    }

    fun rotatePagesCounterClockwise(indexes: Set<Int>) {
        val newState = DocumentState(getCurrentState().pages
            .mapValuesTo(linkedMapOf()) { (k, v) -> if (indexes.contains(k)) v.rotateCounterClockwise() else v })
        changeState(newState)
    }

    fun restorePreviousState() {
        if (statesStack.size > 1) {
            val prevState = statesStack.removeLast()
            changeTitleImageThumbnail(prevState, statesStack.last())
        }
    }

    private fun changeState(newState: DocumentState) {
        val prevState = getCurrentState()
        statesStack.addLast(newState)
        changeTitleImageThumbnail(prevState, newState)
    }

    private fun changeTitleImageThumbnail(prevState: DocumentState, newState: DocumentState) {
        val (prevTitlePageIndex, prevTitlePageRotation) = prevState.pages.asIterable().first()
        val (newTitlePageIndex, newTitlePageRotation) = newState.pages.asIterable().first()
        if (prevTitlePageIndex == newTitlePageIndex && prevTitlePageRotation == newTitlePageRotation) {
            return
        }

        notifySubscribers(TitleImageChanged)
    }

    fun getCurrentTitleImage(): Image {
        val (currentTitleImageIndex, currentTitleImageRotation) = getCurrentState().pages.asIterable().first()
        return pdf.getPageImage(currentTitleImageIndex).rotate(currentTitleImageRotation)
    }

    fun getCurrentState(): DocumentState = statesStack.last()

    fun getCurrentPageImage(pageIndex: Int) = pdf.getPageImage(pageIndex).rotate(getCurrentPageRotation(pageIndex))

    private fun getCurrentPageRotation(pageIndex: Int) = getCurrentState().pages[pageIndex] ?: Rotation.NORTH

    fun getCurrentPagesThumbnails(scope: CoroutineScope): Map<Int, JImage> =
        getCurrentState().pages.mapValues { (pageIndex, rotation) ->
            val preloadedThumbnail = pdf.getPreloadedPageThumbnail(pageIndex)
            JImage(preloadedThumbnail?.rotate(rotation) ?: loadingImage).also { pagePreview ->
                if (preloadedThumbnail == null) {
                    scope.launch {
                        pagePreview.repaintWith(pdf.getPageThumbnail(pageIndex).rotate(rotation))
                        notifySubscribers(ThumbnailLoaded)
                    }
                }
            }
        }
}
