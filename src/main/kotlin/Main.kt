import parser.CsvParser.parseResource
import resolver.Resolver
import visualize.VisualizePriseAndPerf
import java.util.Locale

fun main(args: Array<String>) {
    Locale.setDefault(Locale.ROOT)

    val parseResult = parseResource("fakePlayersDirty.csv")
        ?: error("Ресурс fakePlayersDirty.csv не найден")
    val players = parseResult.players
    val resolver = Resolver(players)

    println("=== Парсинг задание 1 ===")
    println("Разобрано игроков: ${players.size}, пропущено строк: ${parseResult.skippedCount}")

    println()
    println("=== Запросы (задание 2) ===")

    println("1. Игроков без агентства: ${resolver.getCountWithoutAgency()}")

    val (bestDefender, bestDefenderGoals) = resolver.getBestScorerDefender()
    println("2. Лучший бомбардир среди защитников: $bestDefender ($bestDefenderGoals голов)")

    println("3. Позиция самого дорогого немца: ${resolver.getTheExpensiveGermanPlayerPosition()}")

    val rudestTeam = resolver.getTheRudestTeam()
    println("4. Самая «грубая» команда: ${rudestTeam.name} (${rudestTeam.city})")

    println("5. Средняя трансферная стоимость по позициям:")
    resolver.getAverageTransferCostByPosition().forEach { (position, average) ->
        println("   ${position.russianName}: ${"%,.0f".format(average)}")
    }

    println("6. Топ-3 самых полезных игроков (голы + 2 × передачи):")
    resolver.getMostValuablePlayers().forEachIndexed { index, player ->
        println("   ${index + 1}. ${player.name} (${player.team.name}) — ${player.goals + 2 * player.assists}")
    }

    println("7. Самое популярное агентство по странам:")
    resolver.getMostPopularAgencyByCountry().forEach { (country, agency) ->
        println("   $country: $agency")
    }

    val (belowMedianShare, aboveMedianShare) = resolver.getGoalsShareByMedianCost()
    println(
        "8. Доли голов относительно медианной стоимости: " +
            "не дороже медианы — ${"%.1f%%".format(belowMedianShare * 100)}, " +
            "дороже — ${"%.1f%%".format(aboveMedianShare * 100)}",
    )



    // Графики запускаются отдельно, чтобы консольная часть и CI не зависели от GUI:
    // ./gradlew run --args="visualize"
    if (args.contains("visualize")) {
        VisualizePriseAndPerf.show(players)
    }
}
