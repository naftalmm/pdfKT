import org.icepdf.core.exceptions.PDFException
import org.icepdf.core.exceptions.PDFSecurityException
import org.icepdf.core.pobjects.Document
import org.icepdf.core.pobjects.Page
import org.icepdf.core.util.GraphicsRenderingHints
import java.awt.Image
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

const val IMAGES_THUMBNAILS_SIZE = 200

class PDFDocument(file: File) {
    val fileName: String = file.nameWithoutExtension
    val numberOfPages: Int

    private val document = Document()
    private val imagesThumbnails: MutableMap<Int, Image> = ConcurrentHashMap()

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

        numberOfPages = document.numberOfPages
    }

    fun getPageThumbnail(page: Int) =
        imagesThumbnails.getOrPut(page) { getPageImage(page).fit(IMAGES_THUMBNAILS_SIZE) }

    fun getPageImage(page: Int): Image =
        document.getPageImage(page, GraphicsRenderingHints.SCREEN, Page.BOUNDARY_CROPBOX, 0f, 1f)
}

fun <K, V> LinkedHashMap<K, V>.first(): Map.Entry<K, V>? {
    val iterator = this.iterator()
    return if (iterator.hasNext()) iterator.next() else null
}
