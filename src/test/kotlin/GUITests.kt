import org.assertj.core.api.Assertions.assertThat
import org.assertj.swing.core.ComponentDragAndDrop
import org.assertj.swing.core.ComponentFinder
import org.assertj.swing.core.KeyPressInfo
import org.assertj.swing.core.KeyPressInfo.keyCode
import org.assertj.swing.core.Robot
import org.assertj.swing.core.matcher.JButtonMatcher
import org.assertj.swing.core.matcher.JLabelMatcher
import org.assertj.swing.driver.ComponentDriver
import org.assertj.swing.driver.ComponentDriver.propertyName
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager
import org.assertj.swing.edt.GuiActionRunner
import org.assertj.swing.fixture.AbstractComponentFixture
import org.assertj.swing.fixture.FrameFixture
import org.assertj.swing.fixture.JPanelFixture
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Point
import java.awt.event.InputEvent.CTRL_MASK
import java.awt.event.InputEvent.SHIFT_MASK
import java.awt.event.KeyEvent.VK_A
import java.awt.event.KeyEvent.VK_CONTROL
import java.awt.event.KeyEvent.VK_DELETE
import java.awt.event.KeyEvent.VK_END
import java.awt.event.KeyEvent.VK_HOME
import java.awt.event.KeyEvent.VK_O
import java.awt.event.KeyEvent.VK_SHIFT
import java.awt.event.KeyEvent.VK_Z
import java.io.File
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.reflect.KProperty

class PDFKTApplicationTest {
    private lateinit var window: FrameFixture
    private lateinit var finder: ComponentFinder
    private lateinit var tempDir: Path

    companion object {
        @Suppress("DEPRECATION")
        private val ctrlA: KeyPressInfo = keyCode(VK_A).modifiers(CTRL_MASK)

        @Suppress("DEPRECATION")
        private val ctrlO: KeyPressInfo = keyCode(VK_O).modifiers(CTRL_MASK)

        @Suppress("DEPRECATION")
        private val ctrlZ: KeyPressInfo = keyCode(VK_Z).modifiers(CTRL_MASK)

        @Suppress("DEPRECATION")
        private val shiftHome: KeyPressInfo = keyCode(VK_HOME).modifiers(SHIFT_MASK)

        @Suppress("DEPRECATION")
        private val shiftEnd: KeyPressInfo = keyCode(VK_END).modifiers(SHIFT_MASK)

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
        finder = window.robot().finder()
    }

    @AfterEach
    fun tearDown() = window.cleanUp()

    @Test
    fun `should show drop PDFs label when PDFs list is empty`() {
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
    fun `should support d'n'd rearrange items`() {
        addPDF("1")
        addPDF("2")
        with(finder.findByType<JPDFsList>()) {
            assertEquals("1", getCurrentPDFsState()[0].first.nameWithoutExtension)

            finder.findAllOfType<JPDFDocumentListItem>(this)[1].toFixture().dragAndDropTo(Point(0, 0))
            assertEquals("2", getCurrentPDFsState()[0].first.nameWithoutExtension)
        }
    }

    @Test
    fun `should merge in list order`() {
        renewTempDir()

        addPDF("1")
        addPDF("2")
        assertFileContentsEquals(getTestResource("12.pdf"), saveToTempDirAs("12.pdf"))

        //rearrange 2 & 1
        finder.findAllOfType<JPDFDocumentListItem>()[1].toFixture().dragAndDropTo(Point(0, 0))
        assertFileContentsEquals(getTestResource("21.pdf"), saveToTempDirAs("21.pdf"))
    }

    @Test
    fun `should select page on click`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        val pagePreviews = finder.findAllOfType<JPagePreview>(window.dialog().target()).map { it.toFixture() }
        with(pagePreviews[0]) {
            requireNotSelected()
            click()
            requireSelected()
        }
        assertEquals(1, pagePreviews.count { it.isSelected() })
    }

    @Test
    fun `should reset page selection on click`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            pressAndReleaseKey(ctrlA)
            val pagePreviews = finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }
            with(pagePreviews[0]) {
                click()
                requireSelected()
            }
            assertEquals(1, pagePreviews.count { it.isSelected() })
        }
    }

    @Test
    fun `should add to selected pages on Ctrl+click on not selected page`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        val pagePreviews = finder.findAllOfType<JPagePreview>(window.dialog().target()).map { it.toFixture() }
        pagePreviews[0].ctrlClick().requireSelected()
        pagePreviews[1].ctrlClick()

        pagePreviews[0].requireSelected()
        pagePreviews[1].requireSelected()
        assertEquals(2, pagePreviews.count { it.isSelected() })
    }

    @Test
    fun `should remove from selected pages on Ctrl+click on selected page`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        val pagePreviews = finder.findAllOfType<JPagePreview>(window.dialog().target()).map { it.toFixture() }
        pagePreviews[0].click().requireSelected()
        pagePreviews[0].ctrlClick().requireNotSelected()
    }

    @Test
    fun `should select range on Shift+click`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        val pagePreviews = finder.findAllOfType<JPagePreview>(window.dialog().target()).map { it.toFixture() }
        pagePreviews[2].shiftClick().requireSelected()
        assertEquals(1, pagePreviews.count { it.isSelected() })

        pagePreviews[1].shiftClick()
        pagePreviews[2].requireSelected()
        pagePreviews[1].requireSelected()
        assertEquals(2, pagePreviews.count { it.isSelected() })

        pagePreviews[0].shiftClick()
        assertTrue(pagePreviews.all { it.isSelected() })

        //repeated shift+click doesn't remove selection
        pagePreviews[0].shiftClick()
        assertTrue(pagePreviews.all { it.isSelected() })
    }

    @Test
    @Disabled("AssertJ Swing bug - Shift modifier is not working correctly")
    fun `should select range from latest selected to first on Shift+Home`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val pagePreviews = finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }

            //should do nothing when there is no previous selection
            pressAndReleaseKey(shiftHome)
            assertTrue(pagePreviews.all { it.isNotSelected() })

            pagePreviews[1].click().requireSelected()
            pressAndReleaseKey(shiftHome)

            pagePreviews[0].requireSelected()
            pagePreviews[1].requireSelected()
            assertEquals(2, pagePreviews.count { it.isSelected() })

            pressAndReleaseKey(shiftHome) //should do nothing
            assertEquals(2, pagePreviews.count { it.isSelected() })
        }
    }

    @Test
    @Disabled("AssertJ Swing bug - Shift modifier is not working correctly")
    fun `should select range from latest selected to last on Shift+End`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val pagePreviews = finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }

            pressAndReleaseKey(shiftEnd) //should do nothing when there is no previous selection
            assertTrue(pagePreviews.all { it.isNotSelected() })

            pagePreviews[1].click().requireSelected()
            pressAndReleaseKey(shiftEnd)

            pagePreviews[1].requireSelected()
            pagePreviews[2].requireSelected()
            assertEquals(2, pagePreviews.count { it.isSelected() })

            pressAndReleaseKey(shiftEnd) //should do nothing
            assertEquals(2, pagePreviews.count { it.isSelected() })
        }
    }

    @Test
    fun `should select all on Ctrl+A`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            pressAndReleaseKey(ctrlA)
            val pagePreviews = finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }
            assertTrue(pagePreviews.all { it.isSelected() })
            pressAndReleaseKey(ctrlA) //should do nothing
            assertTrue(pagePreviews.all { it.isSelected() })
        }
    }

    @Test
    fun `should revert to previous state on Ctrl+Z`() {
        renewTempDir()
        addPDF("123")

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findAllOfType<JPagePreview>(target()).last().toFixture().click()
            pressAndReleaseKey(keyCode(VK_DELETE))
            assertEquals(2, finder.findAllOfType<JPagePreview>(target()).size)

            finder.findAllOfType<JPagePreview>(target()).last().toFixture().click()
            pressAndReleaseKey(keyCode(VK_DELETE))
            assertEquals(1, finder.findAllOfType<JPagePreview>(target()).size)

            pressAndReleaseKey(ctrlZ)
            assertEquals(2, finder.findAllOfType<JPagePreview>(target()).size)
            pressAndReleaseKey(ctrlZ)
            assertEquals(3, finder.findAllOfType<JPagePreview>(target()).size)
            pressAndReleaseKey(ctrlZ) //should do nothing in initial state

            close()
        }

        assertFileContentsEquals(getTestResource("123.pdf"), saveToTempDirAs("123.pdf"))
    }

    @Test
    fun `should not allow rotation when no pages are selected`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            assertTrue(finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }.all { it.isNotSelected() })
            button(JButtonMatcher.withText("Rotate clockwise")).requireDisabled()
            button(JButtonMatcher.withText("Rotate counter-clockwise")).requireDisabled()
        }
    }

    @Test
    fun `should not allow deletion when no pages are selected`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            assertTrue(finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }.all { it.isNotSelected() })
            button(JButtonMatcher.withText("Remove selected")).requireDisabled()
        }
    }

    @Test
    fun `should not allow delete all pages`() {
        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val removeSelectedBtn = button(JButtonMatcher.withText("Remove selected"))
            val pagePreviews = finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }
            pagePreviews[0].click()
            removeSelectedBtn.requireEnabled()

            pressAndReleaseKey(ctrlA)
            removeSelectedBtn.requireDisabled()
        }
    }

    @Test
    fun `should delete selected pages`() {
        renewTempDir()

        addPDF("123")
        window.button(JButtonMatcher.withText("Edit")).click()
        /* Delete with button */
        with(window.dialog()) {
            val pagePreviews = finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }
            pagePreviews.last().click()
            button(JButtonMatcher.withText("Remove selected")).click()

            assertEquals(2, finder.findAllOfType<JPagePreview>(target()).size)

            close()
        }
        assertFileContentsEquals(getTestResource("12.pdf"), saveToTempDirAs("12.pdf"))

        window.button(JButtonMatcher.withText("Edit")).click()
        /* Delete with hotkey */
        with(window.dialog()) {
            val pagePreviews = finder.findAllOfType<JPagePreview>(target()).map { it.toFixture() }
            pagePreviews.last().click()
            pressAndReleaseKey(keyCode(VK_DELETE))

            assertEquals(1, finder.findAllOfType<JPagePreview>(target()).size)

            close()
        }
        assertFileContentsEquals(getTestResource("1.pdf"), saveToTempDirAs("1.pdf"))
    }

    @Test
    fun `should rotate selected pages clockwise`() {
        renewTempDir()
        addPDF("1")

        /* rotate by 90 degrees */
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            button(JButtonMatcher.withText("Rotate clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_90.pdf"),
            saveToTempDirAs("1_rotated_clockwise_90.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            button(JButtonMatcher.withText("Rotate clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_180.pdf"),
            saveToTempDirAs("1_rotated_clockwise_180.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            button(JButtonMatcher.withText("Rotate clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_270.pdf"),
            saveToTempDirAs("1_rotated_clockwise_270.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            button(JButtonMatcher.withText("Rotate clockwise")).click()
            close()
        }
        assertFileContentsEquals(getTestResource("1.pdf"), saveToTempDirAs("1.pdf"))

        /* rotate by 180 degrees */
        renewTempDir()

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            val rotateBtn = button(JButtonMatcher.withText("Rotate counter-clockwise"))
            repeat(2) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_180.pdf"),
            saveToTempDirAs("1_rotated_clockwise_180.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            val rotateBtn = button(JButtonMatcher.withText("Rotate counter-clockwise"))
            repeat(2) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(getTestResource("1.pdf"), saveToTempDirAs("1.pdf"))

        /* rotate by 360 + 90 degrees */
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            val rotateBtn = button(JButtonMatcher.withText("Rotate counter-clockwise"))
            repeat(5) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_90.pdf"),
            saveToTempDirAs("1_rotated_clockwise_90.pdf")
        )
    }

    @Test
    fun `should rotate selected pages counter-clockwise`() {
        renewTempDir()
        addPDF("1")

        /* rotate by 90 degrees */
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            button(JButtonMatcher.withText("Rotate counter-clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_270.pdf"),
            saveToTempDirAs("1_rotated_counter-clockwise_90.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            button(JButtonMatcher.withText("Rotate counter-clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_180.pdf"),
            saveToTempDirAs("1_rotated_counter-clockwise_180.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            button(JButtonMatcher.withText("Rotate counter-clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_90.pdf"),
            saveToTempDirAs("1_rotated_counter-clockwise_270.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            button(JButtonMatcher.withText("Rotate counter-clockwise")).click()
            close()
        }
        assertFileContentsEquals(getTestResource("1.pdf"), saveToTempDirAs("1.pdf"))

        /* rotate by 180 degrees */
        renewTempDir()

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            val rotateBtn = button(JButtonMatcher.withText("Rotate counter-clockwise"))
            repeat(2) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_180.pdf"),
            saveToTempDirAs("1_rotated_counter-clockwise_180.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            val rotateBtn = button(JButtonMatcher.withText("Rotate counter-clockwise"))
            repeat(2) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(getTestResource("1.pdf"), saveToTempDirAs("1.pdf"))

        /*rotate by 360 + 90 degrees */
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            finder.findByType<JPagePreview>(target()).toFixture().click()
            val rotateBtn = button(JButtonMatcher.withText("Rotate counter-clockwise"))
            repeat(5) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(
            getTestResource("1_rotated_clockwise_270.pdf"),
            saveToTempDirAs("1_rotated_counter-clockwise_90.pdf")
        )
    }

    @Test
    fun `should rotate all selected pages clockwise`() {
        renewTempDir()
        addPDF("123")

        /* rotate by 90 degrees */
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            button(JButtonMatcher.withText("Rotate all clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_90.pdf"),
            saveToTempDirAs("123_all_rotated_clockwise_90.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            button(JButtonMatcher.withText("Rotate all clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_180.pdf"),
            saveToTempDirAs("123_all_rotated_clockwise_180.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            button(JButtonMatcher.withText("Rotate all clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_270.pdf"),
            saveToTempDirAs("123_all_rotated_clockwise_270.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            button(JButtonMatcher.withText("Rotate all clockwise")).click()
            close()
        }
        assertFileContentsEquals(getTestResource("123.pdf"), saveToTempDirAs("123.pdf"))

        /* rotate by 180 degrees */
        renewTempDir()

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val rotateBtn = button(JButtonMatcher.withText("Rotate all counter-clockwise"))
            repeat(2) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_180.pdf"),
            saveToTempDirAs("123_all_rotated_clockwise_180.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val rotateBtn = button(JButtonMatcher.withText("Rotate all counter-clockwise"))
            repeat(2) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(getTestResource("123.pdf"), saveToTempDirAs("123.pdf"))

        /* rotate by 360 + 90 degrees */
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val rotateBtn = button(JButtonMatcher.withText("Rotate all counter-clockwise"))
            repeat(5) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_90.pdf"),
            saveToTempDirAs("123_all_rotated_clockwise_90.pdf")
        )
    }

    @Test
    fun `should rotate all selected pages counter-clockwise`() {
        renewTempDir()
        addPDF("123")

        /* rotate by 90 degrees */
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            button(JButtonMatcher.withText("Rotate all counter-clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_270.pdf"),
            saveToTempDirAs("123_all_rotated_counter-clockwise_90.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            button(JButtonMatcher.withText("Rotate all counter-clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_180.pdf"),
            saveToTempDirAs("123_all_rotated_counter-clockwise_180.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            button(JButtonMatcher.withText("Rotate all counter-clockwise")).click()
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_90.pdf"),
            saveToTempDirAs("123_all_rotated_counter-clockwise_270.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            button(JButtonMatcher.withText("Rotate all counter-clockwise")).click()
            close()
        }
        assertFileContentsEquals(getTestResource("123.pdf"), saveToTempDirAs("123.pdf"))

        /* rotate by 180 degrees */
        renewTempDir()

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val rotateBtn = button(JButtonMatcher.withText("Rotate all counter-clockwise"))
            repeat(2) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_180.pdf"),
            saveToTempDirAs("123_all_rotated_counter-clockwise_180.pdf")
        )

        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val rotateBtn = button(JButtonMatcher.withText("Rotate all counter-clockwise"))
            repeat(2) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(getTestResource("123.pdf"), saveToTempDirAs("123.pdf"))

        /* rotate by 360 + 90 degrees */
        window.button(JButtonMatcher.withText("Edit")).click()
        with(window.dialog()) {
            val rotateBtn = button(JButtonMatcher.withText("Rotate all counter-clockwise"))
            repeat(5) { rotateBtn.click() }
            close()
        }
        assertFileContentsEquals(
            getTestResource("123_all_rotated_clockwise_270.pdf"),
            saveToTempDirAs("123_all_rotated_counter-clockwise_90.pdf")
        )
    }

    @Test
    fun `should show additional dialog on saving to existing file`() {
        renewTempDir()
        addPDF("1")
        val savedFile = saveToTempDirAs("1.pdf")
        assertFileContentsEquals(getTestResource("1.pdf"), savedFile)

        addPDF("2")
        /* Press 'Cancel' in overwrite existing file dialog */
        window.button(JButtonMatcher.withText("Save as PDF...")).click()
        with(window.fileChooser()) {
            fileNameTextBox().setText(savedFile.absolutePath)
            approve()
            window.optionPane().cancelButton().click()
            assertFileContentsEquals(getTestResource("1.pdf"), savedFile)
        }

        /* Press 'No' in overwrite existing file dialog */
        window.button(JButtonMatcher.withText("Save as PDF...")).click()
        with(window.fileChooser()) {
            fileNameTextBox().setText(savedFile.absolutePath)
            approve()
            window.optionPane().noButton().click()
            assertFileContentsEquals(getTestResource("1.pdf"), savedFile)
        }

        /* Press 'Yes' in overwrite existing file dialog */
        window.button(JButtonMatcher.withText("Save as PDF...")).click()
        with(window.fileChooser()) {
            fileNameTextBox().setText(savedFile.absolutePath)
            approve()
            window.optionPane().yesButton().click()
            assertFileContentsEquals(getTestResource("12.pdf"), savedFile)
        }
    }

    private fun addPDF(name: String) {
        window.pressAndReleaseKey(ctrlO).fileChooser().selectFile(getTestResource("$name.pdf")).approve()
    }

    private fun renewTempDir() {
        tempDir = createTempDirectory(prefix = "pdfKT_test").also { it.deleteOnExit() }
    }

    private fun saveToTempDirAs(name: String): File {
        window.button(JButtonMatcher.withText("Save as PDF...")).click()
        val result = tempDir.resolve(name)
        with(window.fileChooser()) {
            fileNameTextBox().setText(result.absolutePathString())
            approve()
        }
        return result.toFile()
    }

    private fun JPanel.toFixture(): JPanelFixture = JPanelFixture(window.robot(), this)
}

private fun <S, C : Component, D : ComponentDriver> AbstractComponentFixture<S, C, D>.dragAndDropTo(where: Point) {
    val dnd = robot().dnd
    val target = target()
    dnd.drag(target, Point(target.bounds.x / 2, target.bounds.y / 2))
    dnd.drop(target, where)
}

private inline fun <reified S, C : Component, D : ComponentDriver> AbstractComponentFixture<S, C, D>.ctrlClick(): S {
    pressKey(VK_CONTROL)
    click()
    releaseKey(VK_CONTROL)
    return S::class.java.cast(this)
}

private inline fun <reified S, C : Component, D : ComponentDriver> AbstractComponentFixture<S, C, D>.shiftClick(): S {
    pressKey(VK_SHIFT)
    click()
    releaseKey(VK_SHIFT)
    return S::class.java.cast(this)
}

val Robot.dnd by LazyDelegate<Robot, ComponentDragAndDrop> { ComponentDragAndDrop(this) }

class LazyDelegate<T : Any, P : Any>(private val initializer: T.() -> P) {
    private lateinit var value: P

    operator fun getValue(thisRef: T, property: KProperty<*>): P {
        if (!this::value.isInitialized) {
            value = initializer.invoke(thisRef)
        }
        return value
    }
}

inline fun <reified T> ComponentFinder.findAllOfType(): List<T> = findAll { it is T }.map { it as T }
inline fun <reified T> ComponentFinder.findAllOfType(root: Container): List<T> =
    findAll(root) { it is T }.map { it as T }

inline fun <reified T : Component> ComponentFinder.findByType(): T = findByType(T::class.java)
inline fun <reified T : Component> ComponentFinder.findByType(root: Container): T = findByType(root, T::class.java)

fun JPanelFixture.isSelected(): Boolean {
    val border = target().border
    return border is LineBorder && border.lineColor == Color.BLUE
}

fun JPanelFixture.requireSelected(): JPanelFixture {
    assertThat(isSelected()).`as`(propertyName(target(), "selected")).isTrue
    return this
}

fun JPanelFixture.isNotSelected() = target().border is EmptyBorder

fun JPanelFixture.requireNotSelected(): JPanelFixture {
    assertThat(isNotSelected()).`as`(propertyName(target(), "selected")).isTrue
    return this
}

private fun getTestResource(name: String): File =
    File(URLDecoder.decode(PDFKTApplicationTest::class.java.getResource("/$name")!!.file, "UTF-8"))

fun assertFileContentsEquals(expected: File, actual: File): Boolean {
    val expectedSize: Long = Files.size(expected.toPath())
    when {
        expectedSize != Files.size(actual.toPath()) -> return false
        expectedSize <= DEFAULT_BUFFER_SIZE -> return expected.readBytes().contentEquals(actual.readBytes())
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