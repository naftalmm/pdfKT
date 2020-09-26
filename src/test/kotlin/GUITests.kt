import org.assertj.swing.core.ComponentDragAndDrop
import org.assertj.swing.core.KeyPressInfo.keyCode
import org.assertj.swing.core.matcher.JButtonMatcher
import org.assertj.swing.core.matcher.JLabelMatcher
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager
import org.assertj.swing.edt.GuiActionRunner
import org.assertj.swing.fixture.FrameFixture
import org.bouncycastle.util.test.SimpleTest
import org.junit.jupiter.api.*
import java.awt.Point
import java.awt.event.InputEvent.CTRL_MASK
import java.awt.event.KeyEvent.VK_O
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.util.concurrent.Callable


class PDFKTApplicationTest {
    private lateinit var window: FrameFixture
    private lateinit var dnd: ComponentDragAndDrop
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
        val size = frame.size //remember original size cause after next line it will be changed
        window = FrameFixture(frame)
        window.show() // shows the frame to test
        window.resizeTo(size) //restore original size

        dnd = ComponentDragAndDrop(window.robot())

        tempDir = createTempDir(prefix = "pdfKT_test").also { it.deleteOnExit() } //TODO create only for certain tests
    }

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
        val pdfsList = window.robot().finder().findByType(JPDFsList::class.java)
        Assertions.assertEquals("1", pdfsList.getCurrentPDFsState()[0].first.nameWithoutExtension)

        val label2 = window.label(JLabelMatcher.withText("2")).target()
        dnd.drag(label2, Point(label2.x, label2.y))
        dnd.drop(label2, Point(0, 0))
        Assertions.assertEquals("2", pdfsList.getCurrentPDFsState()[0].first.nameWithoutExtension)
    }

    @Test
    fun shouldMergeInListOrder() {
        addPDF("1")
        addPDF("2")
        assertFileContentsEquals(getTestResource("12.pdf"), saveToTempDirAs("12.pdf"))

        //rearrange 2 & 1
        val label2 = window.label(JLabelMatcher.withText("2")).target()
        dnd.drag(label2, Point(label2.x, label2.y))
        dnd.drop(label2, Point(0, 0))
        assertFileContentsEquals(getTestResource("21.pdf"), saveToTempDirAs("21.pdf"))
    }

    private fun saveToTempDirAs(name: String): File {
        val result: File = tempDir.resolve(name)
        window.button(JButtonMatcher.withText("Save as PDF...")).click()
        window.fileChooser().fileNameTextBox().setText(result.absolutePath)
        window.fileChooser().approve()
        return result
    }

    @Suppress("DEPRECATION")
    private fun addPDF(name: String) {
        window.pressAndReleaseKey(keyCode(VK_O).modifiers(CTRL_MASK))
            .fileChooser().selectFile(getTestResource("$name.pdf")).approve()
    }

    @AfterEach
    fun tearDown() {
        window.cleanUp()
    }
}

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