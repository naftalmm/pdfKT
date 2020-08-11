import java.io.File
import java.nio.file.Path

class PDFTKSaver(private val input: List<Pair<File, DocumentState>>) {
    fun saveTo(output: Path) {
        val handles = getHandles(input.map { it.first })
        val pdftkInputFiles =
            handles.map { (file, handle) -> "$handle=\"${file.absolutePath}\"" }.joinToString(separator = " ")

        val pdftkCommand = input.flatMap { (file, state) ->
            getRanges(state).map { (range, rotation) ->
                val rangeStr = if (range.first == range.last) "${range.first}" else "${range.first}-${range.last}"
                val rotationStr = if (rotation == Rotation.NORTH) "" else rotation.toString().toLowerCase()
                "${handles[file]}$rangeStr$rotationStr"
            }
        }.joinToString(
            separator = " ",
            prefix = "pdftk $pdftkInputFiles cat ",
            postfix = " output \"${output.toAbsolutePath()}\""
        )
        println(pdftkCommand) //TODO
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