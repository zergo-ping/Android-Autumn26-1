package parser

import model.Player
import model.Position
import model.Team

/**
 * Разбор CSV с футболистами.
 *
 * Формат: `;`-разделённые строки с заголовком:
 * Name;Team;City;Position;Nationality;Agency;Transfer cost;Participations;
 * Goals;Assists;Yellow cards;Red cards
 *
 * Правила маппинга (см. README): пустое агентство — `null` и игрок остаётся,
 * пустые обязательные текстовые поля, нераспознанная позиция и нечисловые
 * числовые поля — строка пропускается и учитывается в [ParseResult.skippedCount].
 */
object CsvParser {

    private const val DELIMITER = ';'
    private const val EXPECTED_COLUMNS = 12

    /** Разбирает содержимое CSV из строки, пропуская строку заголовка. */
    fun parse(content: String): ParseResult = parse(content.lineSequence())

    /**
     * Разбирает CSV из потока строк, не загружая файл целиком в память
     * (последовательность ленива, источник закрывает вызывающий код).
     */
    fun parse(lines: Sequence<String>): ParseResult {
        var skipped = 0

        val players = lines
            .drop(1)
            .mapNotNull { line ->
                val player = mapLineOrNull(line)
                if (player == null) skipped++
                player
            }
            .toList()

        return ParseResult(players, skipped)
    }

    /** Разбирает CSV-ресурс с classpath. Возвращает `null`, если ресурс не найден. */
    fun parseResource(resourcePath: String, classLoader: ClassLoader = CsvParser::class.java.classLoader): ParseResult? {
        val stream = classLoader.getResourceAsStream(resourcePath) ?: return null
        return stream.bufferedReader().useLines { lines -> parse(lines) }
    }

    /** Строка CSV -> игрок, либо `null`, если строку нужно пропустить. */
    private fun mapLineOrNull(line: String): Player? {
        val cells = line.split(DELIMITER)
        if (cells.size != EXPECTED_COLUMNS) return null

        val name = cells[0].trim().takeIf { it.isNotEmpty() } ?: return null

        val teamTokens = cells[1].trim().split(Regex("\\s+"))
        if (teamTokens.size < 2) return null

        val team = Team(
            name = teamTokens[1],
            city = teamTokens[0],
        ).takeIf { it.name.isNotEmpty() && it.city.isNotEmpty() } ?: return null

        val city = cells[2].trim().takeIf { it.isNotEmpty() } ?: return null
        val position = Position.fromCsvTokenOrNull(cells[3]) ?: return null
        val nationality = cells[4].trim().takeIf { it.isNotEmpty() } ?: return null
        val agency = cells[5].trim().takeIf { it.isNotEmpty() }
        val transferCost = cells[6].trim().toLongOrNull() ?: return null
        val participations = cells[7].trim().toIntOrNull() ?: return null
        val goals = cells[8].trim().toIntOrNull() ?: return null
        val assists = cells[9].trim().toIntOrNull() ?: return null
        val yellowCards = cells[10].trim().toIntOrNull() ?: return null
        val redCards = cells[11].trim().toIntOrNull() ?: return null

        return Player(
            name = name,
            team = team,
            city = city,
            position = position,
            nationality = nationality,
            agency = agency,
            transferCost = transferCost,
            participations = participations,
            goals = goals,
            assists = assists,
            yellowCards = yellowCards,
            redCards = redCards,
        )
    }
}
