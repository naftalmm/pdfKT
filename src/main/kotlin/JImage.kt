import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import java.awt.Image.SCALE_SMOOTH
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin


internal class JImage(private val img: Image) : JPanel() {
    override fun paintComponent(g: Graphics) {
        g.drawImage(img, 0, 0, null)
    }

    init {
        layout = null

        val size = Dimension(img.getWidth(), img.getHeight())
        preferredSize = size
        minimumSize = size
        maximumSize = size
        setSize(size)
    }

    fun rotate(angle: Double): JImage {
        val angleRad = Math.toRadians(angle)
        val sin = abs(sin(angleRad))
        val cos = abs(cos(angleRad))
        val neww = floor(width * cos + height * sin).toInt()
        val newh = floor(height * cos + width * sin).toInt()

        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = result.createGraphics()
        g.translate((neww - width) / 2, (newh - height) / 2)
        g.rotate(angleRad, width / 2.toDouble(), height / 2.toDouble())
        g.drawRenderedImage(img.toBufferedImage(), null)
        g.dispose()
        return JImage(result)
    }
}

fun Image.getWidth() = getWidth(null)
fun Image.getHeight() = getHeight(null)

fun Image.toBufferedImage(): BufferedImage {
    if (this is BufferedImage) {
        return this
    }
    val bufferedImage = BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB)

    val graphics2D = bufferedImage.createGraphics()
    graphics2D.drawImage(this, 0, 0, null)
    graphics2D.dispose()

    return bufferedImage
}

fun Image.fit(maxSize: Int): Image {
    val width = getWidth()
    val height = getHeight()
    return if (width > maxSize || height > maxSize) {
        when {
            (width > height) -> getScaledInstance(maxSize, -1, SCALE_SMOOTH)
            else -> getScaledInstance(-1, maxSize, SCALE_SMOOTH)
        }
    } else this
}