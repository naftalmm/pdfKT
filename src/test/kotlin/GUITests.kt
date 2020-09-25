import org.assertj.swing.core.ComponentDragAndDrop
import org.assertj.swing.core.KeyPressInfo.keyCode
import org.assertj.swing.core.matcher.JButtonMatcher
import org.assertj.swing.core.matcher.JLabelMatcher.withText
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
import java.util.concurrent.Callable


class PDFKTApplicationTest {
    private lateinit var window: FrameFixture
    private lateinit var dnd: ComponentDragAndDrop

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
        window = FrameFixture(frame)
        window.show() // shows the frame to test
        window.maximize()

        dnd = ComponentDragAndDrop(window.robot())
    }

    @Test
    fun shouldShowDropPDFsLabelWhenPDFsListIsEmpty() {
        val dropPDFsLabel = window.label(withText("Drop PDFs here"))
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

        val label2 = window.label(withText("2")).target()
        dnd.drag(label2, Point(label2.x, label2.y))
        dnd.drop(label2, Point(0, 0))
        Assertions.assertEquals("2", pdfsList.getCurrentPDFsState()[0].first.nameWithoutExtension)
    }

    @Suppress("DEPRECATION")
    private fun addPDF(name: String) {
        window.pressAndReleaseKey(keyCode(VK_O).modifiers(CTRL_MASK))
            .fileChooser().selectFile(getTestResource("$name.pdf")).approve()
    }

    private fun getTestResource(name: String): File =
        File(URLDecoder.decode(SimpleTest::class.java.getResource("/$name").file, "UTF-8"))

    @AfterEach
    fun tearDown() {
        window.cleanUp()
    }
}