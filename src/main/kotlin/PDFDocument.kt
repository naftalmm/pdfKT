import kotlinx.coroutines.*
import org.icepdf.core.exceptions.PDFException
import org.icepdf.core.exceptions.PDFSecurityException
import org.icepdf.core.pobjects.Document
import org.icepdf.core.pobjects.Page
import org.icepdf.core.util.GraphicsRenderingHints
import java.awt.Image
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.LinkedHashMap

const val IMAGES_THUMBNAILS_SIZE = 200

enum class Rotation(val angle: Int) {
    NORTH(0), EAST(90), SOUTH(180), WEST(270)
}

data class DocumentState(val pages: LinkedHashMap<Int, Rotation>)

class PDFDocument(file: File) {
    val fileName: String = file.nameWithoutExtension

    private val document = Document()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pageImagesThumbnailsLoadingJob: Job
    private val imagesThumbnails: MutableMap<Int, Image> = ConcurrentHashMap()
    var currentTitleImage: Image

    private val statesStack: LinkedList<DocumentState>

    init {
        try {
            document.setFile(file.absolutePath)
        } catch (ex: PDFException) {
            println("Error parsing PDF document $ex")
        } catch (ex: PDFSecurityException) {
            println("Error encryption not supported $ex")
        } catch (ex: FileNotFoundException) {
            println("Error file not found $ex")
        } catch (ex: IOException) {
            println("Error IOException $ex")
        }

        pageImagesThumbnailsLoadingJob = initPageImagesLoadingJob()
        currentTitleImage = initTitleImage()
        statesStack = initStatesStack()
    }

    private fun initPageImagesLoadingJob() = scope.launch {
        val jobs = (0 until document.numberOfPages).map {
            scope.async {
                getPageThumbnail(it)
            }
        }
        jobs.awaitAll()
        // clean up resources
        document.dispose()
    }

    fun thumbnailsAreReady() = !pageImagesThumbnailsLoadingJob.isActive

    fun cancelPageImagesLoadingJob() {
        scope.coroutineContext.cancelChildren()
        document.dispose()
    }

    fun getPageThumbnail(page: Int) =
        imagesThumbnails.getOrPut(page) { getPageImage(page).fit(IMAGES_THUMBNAILS_SIZE) }

    private fun getPageImage(page: Int): Image =
        document.getPageImage(page, GraphicsRenderingHints.SCREEN, Page.BOUNDARY_CROPBOX, 0f, 1f)

    private fun initTitleImage() = getPageImage(0)

    fun removePages(indexes: Set<Int>) {
        val prevState = statesStack.last()
        val newState =
            DocumentState(prevState.pages.filterKeys { indexes.contains(it) } as LinkedHashMap<Int, Rotation>)
        statesStack.add(newState)
        changeTitleImage(prevState, newState)
    }

    fun rotatePages(indexes: Set<Int>, rotation: Rotation) {
        val prevState = statesStack.last()
        val newState =
            DocumentState(prevState.pages
                .map { (k, v) -> if (indexes.contains(k)) k to rotation else k to v }
                .toMap(LinkedHashMap()))
        statesStack.add(newState)
        changeTitleImage(prevState, newState)
    }

    fun restorePreviousState() {
        if (statesStack.size > 1) {
            val prevState = statesStack.pop()
            changeTitleImage(prevState, statesStack.peek())
        }
    }

    private fun changeTitleImage(prevState: DocumentState, newState: DocumentState) {
        val prevTitlePageState = prevState.pages.first()
        val (newTitlePageIndex, newTitlePageRotation) = newState.pages.first() ?: return

        if (prevTitlePageState != null) {
            val (prevTitlePageIndex, prevTitlePageRotation) = prevTitlePageState
            if (prevTitlePageIndex == newTitlePageIndex && prevTitlePageRotation == newTitlePageRotation) {
                return
            }
        }

        currentTitleImage = getPageImage(newTitlePageIndex).rotate(newTitlePageRotation.angle.toDouble())
    }

    private fun initStatesStack(): LinkedList<DocumentState> = LinkedList<DocumentState>()
        .also { it.push(DocumentState((0 until document.numberOfPages).associateWithTo(LinkedHashMap()) { Rotation.NORTH })) }

    fun getCurrentState(): DocumentState = statesStack.peek()
}

fun <K, V> LinkedHashMap<K, V>.first(): Map.Entry<K, V>? {
    val iterator = this.iterator()
    return if (iterator.hasNext()) iterator.next() else null
}
