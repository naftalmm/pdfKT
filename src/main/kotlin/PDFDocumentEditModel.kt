import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.Image
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

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

class PDFDocumentEditModel(private val pdf: PDFDocument) : Observable<ThumbnailLoaded> {
    companion object {
        //TODO разобраться, почему gif не двигается
        //TODO подрезать размер gif
        private val loadingImage = ImageIO.read(PDFDocument::class.java.getResource("loading.gif"))
    }

    override val subscribers: MutableList<Observer> = ArrayList()
    private val statesStack: LinkedList<DocumentState>
    val fileName = pdf.fileName
    var currentTitleImageThumbnail: JImage

    init {
        statesStack = initStatesStack()
        currentTitleImageThumbnail = initTitleImage()
    }

    private fun initStatesStack() = LinkedList<DocumentState>()
        .apply { push(DocumentState((0 until pdf.numberOfPages).associateWithTo(LinkedHashMap()) { Rotation.NORTH })) }

    private fun initTitleImage() = JImage(pdf.getPageImage(0).fit(50))

    fun removePages(indexes: Set<Int>) {
        val newState = DocumentState(getCurrentState().pages.filterNotTo(LinkedHashMap()) { indexes.contains(it.key) })
        changeState(newState)
    }

    fun rotatePagesClockwise(indexes: Set<Int>) {
        val newState = DocumentState(getCurrentState().pages
            .map { (k, v) -> k to if (indexes.contains(k)) v.rotateClockwise() else v }
            .toMap(LinkedHashMap()))
        changeState(newState)
    }

    fun rotatePagesCounterClockwise(indexes: Set<Int>) {
        val newState = DocumentState(getCurrentState().pages
            .map { (k, v) -> k to if (indexes.contains(k)) v.rotateCounterClockwise() else v }
            .toMap(LinkedHashMap()))
        changeState(newState)
    }

    fun restorePreviousState() {
        if (statesStack.size > 1) {
            val prevState = statesStack.removeLast()
            changeTitleImageThumbnail(prevState, statesStack.last())
        }
    }

    private fun changeState(newState: DocumentState) {
        statesStack.addLast(newState)
        changeTitleImageThumbnail(statesStack.last(), newState)
    }

    private fun changeTitleImageThumbnail(prevState: DocumentState, newState: DocumentState) {
        val (prevTitlePageIndex, prevTitlePageRotation) = prevState.pages.asIterable().first()
        val (newTitlePageIndex, newTitlePageRotation) = newState.pages.asIterable().first()
        if (prevTitlePageIndex == newTitlePageIndex && prevTitlePageRotation == newTitlePageRotation) {
            return
        }

        currentTitleImageThumbnail.repaintWith(pdf.getPageImage(newTitlePageIndex).fit(50).rotate(newTitlePageRotation))
    }

    fun getCurrentTitleImage(): Image {
        val (currentTitleImageIndex, currentTitleImageRotation) = getCurrentState().pages.asIterable().first()
        return pdf.getPageImage(currentTitleImageIndex).rotate(currentTitleImageRotation)
    }

    private fun getCurrentState(): DocumentState = statesStack.last()

    fun getCurrentPageImage(pageIndex: Int) = pdf.getPageImage(pageIndex).rotate(getCurrentPageRotation(pageIndex))

    private fun getCurrentPageRotation(pageIndex: Int) = getCurrentState().pages[pageIndex] ?: Rotation.NORTH

    fun getCurrentPagesThumbnails(scope: CoroutineScope): Map<Int, JImage> =
        getCurrentState().pages.map { (pageIndex, rotation) ->
            val pagePreview = JImage(loadingImage)
            scope.launch { pagePreview.repaintWith(getPageThumbnail(pageIndex).rotate(rotation)); notifySubscribers() }
            pageIndex to pagePreview
        }.toMap()

    private fun getPageThumbnail(pageIndex: Int): Image = pdf.getPageThumbnail(pageIndex)
    override fun getEvent() = ThumbnailLoaded
}
