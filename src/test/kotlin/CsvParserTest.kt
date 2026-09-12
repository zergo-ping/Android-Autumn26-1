import model.Player
import model.Position
import model.Team
import parser.CsvParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Тесты правил маппинга «строка CSV → Player» (задание 1 из README).
 */
class CsvParserTest {

    private val header =
        "Name;Team;City;Position;Nationality;Agency;Transfer cost;Participations;Goals;Assists;Yellow cards;Red cards"

    /** Собирает строку CSV с заголовком и переданными строками данных. */
    private fun csv(vararg rows: String) = (listOf(header) + rows).joinToString("\n")

    private fun parseSingleRow(row: String): Player {
        val result = CsvParser.parse(csv(row))
        assertEquals(0, result.skippedCount)
        return result.players.single()
    }

    @Test
    fun `valid row maps to player with all fields`() {
        val player = parseSingleRow(
            "Iva Streich;Nevada whales;South Carolina;MIDFIELD;Colombia;D'Amore LLC;75012006;22;19;6;3;7",
        )

        assertEquals("Iva Streich", player.name)
        assertEquals(Team("whales", "Nevada"), player.team)
        assertEquals("South Carolina", player.city)
        assertEquals(Position.MIDFIELD, player.position)
        assertEquals("Colombia", player.nationality)
        assertEquals("D'Amore LLC", player.agency)
        assertEquals(75012006L, player.transferCost)
        assertEquals(22, player.participations)
        assertEquals(19, player.goals)
        assertEquals(6, player.assists)
        assertEquals(3, player.yellowCards)
        assertEquals(7, player.redCards)
    }

    @Test
    fun `empty agency becomes null and player stays in result`() {
        val player = parseSingleRow(
            "Ms. Adolph Hartmann;North Carolina dolphins;Tennessee;FORWARD;Croatia;;52944545;27;10;5;5;8",
        )

        assertNull(player.agency)
    }

    @Test
    fun `position in any case and with spaces is recognized`() {
        val variants = listOf("  DeFeNdEr  ", "FORWARD", "  midfield", "Goalkeeper")
        val expected = listOf(Position.DEFENDER, Position.FORWARD, Position.MIDFIELD, Position.GOALKEEPER)

        variants.forEachIndexed { index, token ->
            val player = parseSingleRow(
                "Name X;Team Y;City Z;$token;Russia;Agency;1000;10;5;2;1;0",
            )
            assertEquals(expected[index], player.position, "для токена '$token'")
        }
    }

    @Test
    fun `unrecognized position makes row skipped`() {
        val result = CsvParser.parse(csv("Name X;Team Y;City Z;Striker;Russia;Agency;1000;10;5;2;1;0"))

        assertEquals(0, result.players.size)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun `extra spaces in text fields are trimmed`() {
        val player = parseSingleRow(
            "  Iva   Streich  ;  Nevada   whales  ;  South Carolina  ;MIDFIELD;  Colombia  ;  D'Amore LLC  ;100;1;2;3;4;5",
        )

        assertEquals("Iva   Streich", player.name)
        assertEquals("whales", player.team.name)
        assertEquals("Nevada", player.team.city)
        assertEquals("South Carolina", player.city)
        assertEquals("Colombia", player.nationality)
        assertEquals("D'Amore LLC", player.agency)
    }

    @Test
    fun `empty or non-numeric numeric fields make rows skipped and counted`() {
        val rows = listOf(
            "Name X;Team Y;City Z;FORWARD;Russia;Agency;;10;5;2;1;0",      // пустая стоимость
            "Name X;Team Y;City Z;FORWARD;Russia;Agency;abc;10;5;2;1;0",   // нечисловая стоимость
            "Name X;Team Y;City Z;FORWARD;Russia;Agency;1000;;5;2;1;0",    // пустые матчи
            "Name X;Team Y;City Z;FORWARD;Russia;Agency;1000;10;-five;2;1;0", // нечисловые голы
            "Name X;Team Y;City Z;FORWARD;Russia;Agency;1000;10;5;2;1;",   // меньше колонок
        )

        val result = CsvParser.parse(csv(*rows.toTypedArray()))

        assertEquals(0, result.players.size)
        assertEquals(rows.size, result.skippedCount)
    }

    @Test
    fun `empty required text fields make row skipped`() {
        val rows = listOf(
            ";Team Y;City Z;FORWARD;Russia;Agency;1000;10;5;2;1;0",        // пустое имя
            "Name X; ;City Z;FORWARD;Russia;Agency;1000;10;5;2;1;0",       // пустая команда
            "Name X;Team Y;;FORWARD;Russia;Agency;1000;10;5;2;1;0",        // пустой город
            "Name X;Team Y;City Z;FORWARD;;Agency;1000;10;5;2;1;0",        // пустая страна
            "Name X;Team Y;City Z;;Russia;Agency;1000;10;5;2;1;0",         // пустая позиция
        )

        val result = CsvParser.parse(csv(*rows.toTypedArray()))

        assertEquals(0, result.players.size)
        assertEquals(rows.size, result.skippedCount)
    }

    @Test
    fun `dirty dataset parses without crash and counts skipped rows`() {
        val result = CsvParser.parseResource("fakePlayersDirty.csv")
        kotlin.test.assertNotNull(result)
        assertEquals(250, result.players.size + result.skippedCount)
    }
}