import org.assertj.swing.core.ComponentDragAndDrop
import org.assertj.swing.core.ComponentFinder
import org.assertj.swing.core.KeyPressInfo.keyCode
import org.assertj.swing.core.Robot
import org.assertj.swing.core.matcher.JButtonMatcher
import org.assertj.swing.core.matcher.JLabelMatcher
import org.assertj.swing.driver.ComponentDriver
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager
import org.assertj.swing.edt.GuiActionRunner
import org.assertj.swing.fixture.AbstractComponentFixture
import org.assertj.swing.fixture.FrameFixture
import org.assertj.swing.fixture.JPanelFixture
import org.bouncycastle.util.test.SimpleTest
import org.junit.jupiter.api.*
import java.awt.Color
import java.awt.Component
import java.awt.Point
import java.awt.event.InputEvent.CTRL_MASK
import java.awt.event.KeyEvent.VK_O
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.util.concurrent.Callable
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import kotlin.reflect.KProperty

class PDFKTApplicationTest {
    private lateinit var window: FrameFixture
    private lateinit var tempDir: File

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUpOnce() {
            FailOnThreadViolationRepaintManager.install()
        }

        @JvmStatic
        @AfterAll
        fun tearDownOnce() {
            FailOnThreadViolationRepaintManager.uninstall()
        }
    }

    @BeforeEach
    fun setUp() {
        val frame = GuiActionRunner.execute(Callable { App() })
        val size = frame.size //remember original size, because after next line it will be changed
        window = FrameFixture(frame).apply {
            show() // shows the frame to test
            resizeTo(size) //restore original size
        }
    }

    @AfterEach
    fun tearDown() = window.cleanUp()

    @Test
    fun shouldShowDropPDFsLabelWhenPDFsListIsEmpty() {
        val dropPDFsLabel = window.label(JLabelMatcher.withText("Drop PDFs here"))
        //should be shown initially
        dropPDFsLabel.requireVisible()

        //add some PDF
        addPDF("1")
        dropPDFsLabel.requireNotVisible()

        //delete added item
        window.button(JButtonMatcher.withText("Delete")).click()
        dropPDFsLabel.requireVisible()
    }

    @Test
    fun shouldSupportDnDRearrangeItems() {
        addPDF("1")
        addPDF("2")
        val pdfsList = window.robot().finder().findByType<JPDFsList>()
        Assertions.assertEquals("1", pdfsList.getCurrentPDFsState()[0].first.nameWithoutExtension)

        window.label(JLabelMatcher.withText("2")).dragAndDropTo(Point(0, 0))
        Assertions.assertEquals("2", pdfsList.getCurrentPDFsState()[0].first.nameWithoutExtension)
    }

    @Test
    fun shouldMergeInListOrder() {
        renewTempDir()

        addPDF("1")
        addPDF("2")
        assertFileContentsEquals(getTestResource("12.pdf"), saveToTempDirAs("12.pdf"))

        //rearrange 2 & 1
        window.label(JLabelMatcher.withText("2")).dragAndDropTo(Point(0, 0))
        assertFileContentsEquals(getTestResource("21.pdf"), saveToTempDirAs("21.pdf"))
    }

    @Test
    fun shouldSelectPageOnClick() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        val pagePreviews = window.dialog().robot().finder().findAllOfType<JPagePreview>()
        with(JPanelFixture(window.robot(), pagePreviews[0])) {
            click()
            requireSelected()
        }
    }

    @Suppress("DEPRECATION")
    private fun addPDF(name: String) {
        window.pressAndReleaseKey(keyCode(VK_O).modifiers(CTRL_MASK))
            .fileChooser().selectFile(getTestResource("$name.pdf")).approve()
    }

    private fun renewTempDir() {
        tempDir = createTempDir(prefix = "pdfKT_test").also { it.deleteOnExit() }
    }

    private fun saveToTempDirAs(name: String): File {
        window.button(JButtonMatcher.withText("Save as PDF...")).click()
        val result = tempDir.resolve(name)
        with(window.fileChooser()) {
            fileNameTextBox().setText(result.absolutePath)
            approve()
        }
        return result
    }
}

private fun <S, C : Component, D : ComponentDriver> AbstractComponentFixture<S, C, D>.dragAndDropTo(where: Point) {
    val dnd = robot().dnd
    val target = target()
    dnd.drag(target, Point(target.x, target.y))
    dnd.drop(target, where)
}

val Robot.dnd: ComponentDragAndDrop by ComponentDragAndDropDelegate()

class ComponentDragAndDropDelegate {
    lateinit var value: ComponentDragAndDrop

    operator fun getValue(thisRef: Robot, property: KProperty<*>): ComponentDragAndDrop {
        if (!this::value.isInitialized) {
            value = ComponentDragAndDrop(thisRef)
        }
        return value
    }
}

inline fun <reified T> ComponentFinder.findAllOfType(): List<T> = findAll { it is T }.map { it as T }
inline fun <reified T: Component> ComponentFinder.findByType(): T = findByType(T::class.java)

fun JPanelFixture.requireSelected(): Boolean {
    val border = target().border
    return border is LineBorder && border.lineColor == Color.BLUE
}

fun JPanelFixture.requireNotSelected() = target().border is EmptyBorder

private fun getTestResource(name: String): File =
    File(URLDecoder.decode(SimpleTest::class.java.getResource("/$name").file, "UTF-8"))

fun assertFileContentsEquals(expected: File, actual: File): Boolean {
    val expectedSize: Long = Files.size(expected.toPath())
    when {
        expectedSize != Files.size(actual.toPath()) -> return false
        expectedSize < 2048 -> return expected.readBytes().contentEquals(actual.readBytes())
        else -> {
            expected.bufferedReader().use { bf1 ->
                actual.bufferedReader().use { bf2 ->
                    var ch: Int
                    while (bf1.read().also { ch = it } != -1) {
                        if (ch != bf2.read()) {
                            return false
                        }
                    }
                }
            }
            return true
        }
    }
}