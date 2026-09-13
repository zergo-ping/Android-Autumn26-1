package resolver

import model.Player
import model.Position
import model.Team

/**
 * Реализация запросов к набору игроков
 *
 * @param players уже разобранный и отфильтрованный парсером набор игроков.
 */
class Resolver(private val players: List<Player>) : IResolver {

    override fun getCountWithoutAgency(): Int =
        players.count { it.agency == null }

    override fun getBestScorerDefender(): Pair<String, Int> {
        val best = players
            .filter { it.position == Position.DEFENDER }
            .maxByOrNull { it.goals }
            ?: error("Нет защитников в наборе")
        return best.name to best.goals
    }

    override fun getTheExpensiveGermanPlayerPosition(): String {
        val best = players
            .filter { it.nationality.equals("Germany", ignoreCase = true) }
            .maxByOrNull { it.transferCost }
            ?: error("Нет немецких игроков в наборе")
        return best.position.russianName
    }

    override fun getTheRudestTeam(): Team =
        players
            .groupBy { it.team }
            .maxByOrNull { (_, members) -> members.map { it.redCards }.average() }
            ?.key
            ?: error("Нет игроков в наборе")

    override fun getAverageTransferCostByPosition(): Map<Position, Double> =
        players
            .groupBy { it.position }
            .mapValues { (_, members) -> members.map { it.transferCost.toDouble() }.average() }
            .toList()
            .sortedByDescending { (_, average) -> average }
            .toMap()

    override fun getMostValuablePlayers(): List<Player> =
        players
            .sortedWith(
                compareByDescending<Player> { it.goals + 2 * it.assists }
                    .thenBy { it.name },
            )
            .take(3)

    override fun getMostPopularAgencyByCountry(): Map<String, String> =
        players
            .filter { it.agency != null }
            .groupBy { it.nationality }
            .mapNotNull { (country, members) ->
                val popular = members
                    .groupingBy { it.agency!! }
                    .eachCount()
                    .minWithOrNull(
                        compareByDescending<Map.Entry<String, Int>> { it.value }
                            .thenBy { it.key },
                    )
                popular?.let { country to it.key }
            }
            .toMap()

    override fun getGoalsShareByMedianCost(): Pair<Double, Double> {
        require(players.isNotEmpty()) { "Нет игроков в наборе" }

        val costs = players.map { it.transferCost }.sorted()
        val median = if (costs.size % 2 == 1) {
            costs[costs.size / 2].toDouble()
        } else {
            (costs[costs.size / 2 - 1] + costs[costs.size / 2]) / 2.0
        }

        val totalGoals = players.sumOf { it.goals }
        if (totalGoals == 0) return 0.0 to 0.0

        val goalsAtOrBelowMedian = players
            .filter { it.transferCost <= median }
            .sumOf { it.goals }

        val shareAtOrBelow = goalsAtOrBelowMedian.toDouble() / totalGoals
        return shareAtOrBelow to 1.0 - shareAtOrBelow
    }
}
