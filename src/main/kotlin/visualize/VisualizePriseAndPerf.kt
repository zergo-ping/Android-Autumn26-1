package visualize

import model.Player
import model.Position
import model.Team
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.awt.BasicStroke
import java.awt.Color
import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

/**
 * Базовая задача — топ-10 команд по суммарной трансферной стоимости,
 * продвинутая — связь стоимости и голов у нападающих со скользящим
 * средним тренда и окраской точек по числу жёлтых карточек.
 */
object VisualizePriseAndPerf {

    private const val TOP_TEAMS_COUNT = 10
    private const val TREND_WINDOW = 9

    /** Диапазон жёлтых карточек и цвет точек для него. */
    private data class YellowBucket(val label: String, val range: IntRange, val color: Color)

    private val yellowBuckets = listOf(
        YellowBucket("0 ж.к.", 0..0, Color(67, 160, 71)),
        YellowBucket("1–2 ж.к.", 1..2, Color(251, 192, 45)),
        YellowBucket("3–5 ж.к.", 3..5, Color(239, 108, 0)),
        YellowBucket("6+ ж.к.", 6..Int.MAX_VALUE, Color(222, 53, 74)),
    )

    /** Базовая задача: топ-[topN] команд по суммарной трансферной стоимости. */
    fun topTeamsByTransferCost(players: List<Player>, topN: Int = TOP_TEAMS_COUNT): List<Pair<Team, Long>> =
        players
            .groupBy { it.team }
            .mapValues { (_, members) -> members.sumOf { it.transferCost } }
            .toList()
            .sortedByDescending { (_, totalCost) -> totalCost }
            .take(topN)

    private fun topTeamsChart(players: List<Player>): JFreeChart {
        val dataset = DefaultCategoryDataset()
        topTeamsByTransferCost(players).forEach { (team, totalCost) ->
            dataset.addValue(totalCost.toDouble(), "Стоимость", "${team.name} (${team.city})")
        }

        return ChartFactory.createBarChart(
            "Топ-$TOP_TEAMS_COUNT команд по суммарной трансферной стоимости",
            "Команда",
            "Суммарная трансферная стоимость",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false,
        )
    }

    /**
     * Продвинутая задача: нападающие, стоимость vs голы.
     * Точки окрашены по числу жёлтых карточек, тренд — скользящее среднее
     * по окну [TREND_WINDOW] (считается через `windowed`).
     */
    private fun forwardsCostGoalsChart(players: List<Player>): JFreeChart {
        val forwards = players
            .filter { it.position == Position.FORWARD }
            .sortedBy { it.transferCost }
        require(forwards.isNotEmpty()) { "В наборе нет нападающих" }

        val dataset = XYSeriesCollection()
        val scatterSeries = yellowBuckets.map { bucket ->
            XYSeries(bucket.label).also { dataset.addSeries(it) }
        }
        forwards.forEach { player ->
            val bucketIndex = yellowBuckets.indexOfFirst { player.yellowCards in it.range }
            scatterSeries[bucketIndex].add(player.transferCost.toDouble(), player.goals.toDouble())
        }


        val trendSeries = XYSeries("Скользящее среднее (окно $TREND_WINDOW)")
        dataset.addSeries(trendSeries)
        forwards.windowed(TREND_WINDOW, 1, partialWindows = true).forEachIndexed { index, window ->
            val averageGoals = window.sumOf { it.goals }.toDouble() / window.size
            trendSeries.add(forwards[index].transferCost.toDouble(), averageGoals)
        }

        val chart = ChartFactory.createScatterPlot(
            "Нападающие: трансферная стоимость и голы",
            "Трансферная стоимость",
            "Голы",
            dataset,
        )

        val renderer = XYLineAndShapeRenderer()
        yellowBuckets.forEachIndexed { index, bucket ->
            renderer.setSeriesPaint(index, bucket.color)
            renderer.setSeriesLinesVisible(index, false)
            renderer.setSeriesShapesVisible(index, true)
        }
        val trendIndex = yellowBuckets.size
        renderer.setSeriesPaint(trendIndex, Color.DARK_GRAY)
        renderer.setSeriesStroke(trendIndex, BasicStroke(2.5f))
        renderer.setSeriesLinesVisible(trendIndex, true)
        renderer.setSeriesShapesVisible(trendIndex, false)
        chart.xyPlot.renderer = renderer

        return chart
    }

    /** Показывает оба графика варианта 2 в окнах с вкладками. */
    fun show(players: List<Player>) {
        SwingUtilities.invokeLater {
            val tabs = JTabbedPane().apply {
                addTab("Топ-10 команд по стоимости", ChartPanel(topTeamsChart(players)))
                addTab("Нападающие: стоимость и голы", ChartPanel(forwardsCostGoalsChart(players)))
            }

            JFrame("Вариант 2. Цена и результативность").apply {
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                setSize(1100, 700)
                add(tabs)
                setLocationRelativeTo(null)
                isVisible = true
            }
        }
    }
}
