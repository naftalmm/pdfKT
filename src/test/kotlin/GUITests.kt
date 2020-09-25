import org.assertj.swing.core.KeyPressInfo.keyCode
import org.assertj.swing.core.matcher.JButtonMatcher
import org.assertj.swing.core.matcher.JLabelMatcher.withText
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager
import org.assertj.swing.edt.GuiActionRunner
import org.assertj.swing.fixture.FrameFixture
import org.bouncycastle.util.test.SimpleTest
import org.junit.jupiter.api.*
import java.awt.Event.CTRL_MASK
import java.awt.event.KeyEvent.VK_O
import java.io.File
import java.net.URLDecoder
import java.util.concurrent.Callable


class PDFKTApplicationTest {
    private lateinit var window: FrameFixture

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
    }

    @Test
    fun shouldShowDropPDFsLabelWhenPDFsListIsEmpty() {
        val dropPDFsLabel = window.label(withText("Drop PDFs here"))
        //should be shown initially
        dropPDFsLabel.requireVisible()

        //add some PDF
        window.pressAndReleaseKey(keyCode(VK_O).modifiers(CTRL_MASK))
            .fileChooser().selectFile(getTestResource("1.pdf")).approve()
        dropPDFsLabel.requireNotVisible()

        //delete added item
        window.button(JButtonMatcher.withText("Delete")).click()
        dropPDFsLabel.requireVisible()
    }

    private fun getTestResource(name: String): File =
        File(URLDecoder.decode(SimpleTest::class.java.getResource("/${name}").file, "UTF-8"))

    @AfterEach
    fun tearDown() {
        window.cleanUp()
    }
}