import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object PDFTK {
    private val pdftk by lazy { getPathToPDFTKFolder().resolve("pdftk") }

    private fun getPathToPDFTKFolder(): Path {
        val tempDir = Files.createTempDirectory("pdftk").also { it.deleteOnExit() }

        val uri = PDFTK::class.java.getResource("bin").toURI()
        if (uri.scheme == "jar") {
            val (jar, path) = uri.toString().split("!")
            val binFolder = FileSystems.newFileSystem(URI.create(jar), emptyMap<String, Any>()).getPath(path)

            Files.walk(binFolder)
                .filter { Files.isRegularFile(it) }
                .forEach { input ->
                    val fileName = input.fileName.toString()
                    PDFTK::class.java.getResourceAsStream("bin/$fileName").use { fis ->
                        Files.copy(fis, tempDir.resolve(fileName).also { it.deleteOnExit() })
                    }
                }
        } else {
            val binFolder = Paths.get(uri)
            Files.walk(binFolder)
                .filter { Files.isRegularFile(it) }
                .forEach { input -> Files.copy(input, tempDir.resolve(input.fileName).also { it.deleteOnExit() }) }
        }

        return tempDir
    }

    fun cat(input: List<Pair<File, DocumentState>>, output: Path) {
        val handles = getHandles(input.map { it.first })
        val pdftkInputFiles = handles.map { (file, handle) -> "$handle=\"${file.absolutePath}\"" }.toTypedArray()

        val pdftkCatCommand = input.flatMap { (file, state) ->
            getRanges(state).map { (range, rotation) ->
                val rangeStr = if (range.first == range.last) "${range.first}" else "${range.first}-${range.last}"
                val rotationStr = if (rotation == Rotation.NORTH) "" else rotation.toString().toLowerCase()
                "${handles[file]}$rangeStr$rotationStr"
            }
        }.toTypedArray()

        run(*pdftkInputFiles, "cat", *pdftkCatCommand, "output", output.toAbsolutePath().toString())
    }

    fun run(vararg pdftkCommand: String) {
        val proc = Runtime.getRuntime().exec(arrayOf(pdftk.toString(), *pdftkCommand))
        BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines -> lines.forEach { println(it) } }
        BufferedReader(InputStreamReader(proc.errorStream)).useLines { lines -> lines.forEach { System.err.println(it) } }
    }

    private fun getHandles(files: List<File>): Map<File, String> {
        val handles = handlesGenerator().iterator()
        return files.associateWith { handles.next() }
    }

    private fun handlesGenerator() = generateSequence("A") {
        val prefix = it.substring(0, it.lastIndex)
        when (val i = it.last()) {
            'Z' -> prefix + "AA"
            else -> prefix + (i + 1)
        }
    }

    private fun getRanges(state: DocumentState): List<Pair<IntRange, Rotation>> {
        val result = ArrayList<Pair<IntRange, Rotation>>()
        var rangeStart = 0
        var rangeEnd = 0
        var currentRangeRotation = state.pages[rangeStart]!!
        val lastPageIndex = state.pages.asIterable().last().key
        for ((pageIndex, rotation) in state.pages) {
            if (pageIndex == rangeStart) continue
            if (pageIndex != rangeEnd + 1 || rotation != currentRangeRotation) {
                result.add((rangeStart + 1..rangeEnd + 1) to currentRangeRotation)

                currentRangeRotation = rotation
                rangeStart = pageIndex
            }

            rangeEnd = pageIndex

            if (pageIndex == lastPageIndex) {
                result.add((rangeStart + 1..pageIndex + 1) to currentRangeRotation)
            }
        }

        return result
    }
}