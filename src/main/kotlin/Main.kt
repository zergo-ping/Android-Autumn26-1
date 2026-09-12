import parser.CsvParser
import parser.CsvParser.parseResource
import visualize.VisualizePriseAndPerf

fun main(args: Array<String>) {
    val parseResult = parseResource("fakePlayersDirty.csv")
        ?: error("Ресурс fakePlayersDirty.csv не найден")

        VisualizePriseAndPerf.show(parseResult.players)

}