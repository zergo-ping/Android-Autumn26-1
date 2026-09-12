import model.Player
import model.Position
import model.Team
import resolver.Resolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тесты каждого запроса IResolver (задание 2 из README)
 * на маленьком предсказуемом наборе игроков.
 */
class ResolverTest {

    private val alpha = Team("alpha", "CityA")
    private val beta = Team("beta", "CityB")

    private val alice = Player(
        name = "Alice", team = alpha, city = "CityA", position = Position.DEFENDER,
        nationality = "Germany", agency = null, transferCost = 100, participations = 10,
        goals = 5, assists = 1, yellowCards = 0, redCards = 2,
    )
    private val bob = Player(
        name = "Bob", team = alpha, city = "CityA", position = Position.FORWARD,
        nationality = "Russia", agency = "AG1", transferCost = 200, participations = 10,
        goals = 3, assists = 2, yellowCards = 1, redCards = 0,
    )
    private val carol = Player(
        name = "Carol", team = beta, city = "CityB", position = Position.MIDFIELD,
        nationality = "Germany", agency = "AG2", transferCost = 300, participations = 10,
        goals = 1, assists = 4, yellowCards = 2, redCards = 4,
    )

    private val resolver = Resolver(listOf(alice, bob, carol))

    @Test
    fun `count of players without agency`() {
        assertEquals(1, resolver.getCountWithoutAgency())
    }

    @Test
    fun `best scorer among defenders`() {
        // Среди защитников Alice (5) больше, чем второй защитник (2);
        // форвард Bob с 3 голами в выборку защитников не попадает.
        val quieter = bob.copyDefender(goals = 2)
        val result = Resolver(listOf(alice, quieter, bob)).getBestScorerDefender()

        assertEquals("Alice", result.first)
        assertEquals(5, result.second)
    }

    /** Копия bob — защитник с указанным числом голов (для проверки фильтра по позиции). */
    private fun Player.copyDefender(goals: Int) = Player(
        name = name, team = team, city = city, position = Position.DEFENDER,
        nationality = nationality, agency = agency, transferCost = transferCost,
        participations = participations, goals = goals, assists = assists,
        yellowCards = yellowCards, redCards = redCards,
    )

    @Test
    fun `position of the most expensive german player`() {
        // Немцы — Alice (100) и Carol (300); самая дорогая — Carol, полузащитник.
        assertEquals("полузащитник", resolver.getTheExpensiveGermanPlayerPosition())
    }

    @Test
    fun `team with maximal average red cards`() {
        // alpha: (2 + 0) / 2 = 1, beta: 4 / 1 = 4.
        assertEquals(beta, resolver.getTheRudestTeam())
    }

    @Test
    fun `average transfer cost by position sorted descending`() {
        val result = resolver.getAverageTransferCostByPosition()

        assertEquals(listOf(Position.MIDFIELD, Position.FORWARD, Position.DEFENDER), result.keys.toList())
        assertEquals(300.0, result[Position.MIDFIELD]!!)
        assertEquals(200.0, result[Position.FORWARD]!!)
        assertEquals(100.0, result[Position.DEFENDER]!!)
    }

    @Test
    fun `top-3 most valuable players with alphabetical tie-break`() {
        // Полезность: Carol = 1 + 2*4 = 9; Alice = 5 + 2*1 = 7; Bob = 3 + 2*2 = 7.
        // При равенстве 7 раньше по алфавиту Alice.
        assertEquals(listOf(carol, alice, bob), resolver.getMostValuablePlayers())
    }

    @Test
    fun `most popular agency per country`() {
        assertEquals(mapOf("Germany" to "AG2", "Russia" to "AG1"), resolver.getMostPopularAgencyByCountry())
    }

    @Test
    fun `agency tie is broken alphabetically`() {
        val zeta = bob.copyPlayer(name = "Zed", agency = "ZAgency")
        val alphaAgency = bob.copyPlayer(name = "Ann", agency = "AAgency")
        val result = Resolver(listOf(zeta, alphaAgency)).getMostPopularAgencyByCountry()

        assertEquals("AAgency", result["Russia"])
    }

    private fun Player.copyPlayer(name: String, agency: String) = Player(
        name = name, team = team, city = city, position = position,
        nationality = nationality, agency = agency, transferCost = transferCost,
        participations = participations, goals = goals, assists = assists,
        yellowCards = yellowCards, redCards = redCards,
    )

    @Test
    fun `goals share split by median transfer cost`() {
        // Медиана [100, 200, 300] = 200. Не дороже медианы: Alice (5) + Bob (3) = 8 голов,
        // дороже: Carol (1). Всего 9 голов → доли 8/9 и 1/9.
        val (atOrBelow, above) = resolver.getGoalsShareByMedianCost()

        assertEquals(8.0 / 9.0, atOrBelow, 1e-9)
        assertEquals(1.0 / 9.0, above, 1e-9)
        assertEquals(1.0, atOrBelow + above, 1e-9)
    }

    @Test
    fun `clean dataset resolves all queries without crash`() {
        val parseResult = parser.CsvParser.parseResource("fakePlayers.csv")
        assertTrue(parseResult != null && parseResult.players.isNotEmpty())

        val full = Resolver(parseResult.players)
        assertTrue(full.getCountWithoutAgency() >= 0)
        assertTrue(full.getBestScorerDefender().second >= 0)
        assertTrue(full.getTheExpensiveGermanPlayerPosition().isNotEmpty())
        assertTrue(full.getTheRudestTeam().name.isNotEmpty())

        val averages = full.getAverageTransferCostByPosition()
        assertTrue(averages.isNotEmpty())
        assertEquals(
            averages.values.sortedDescending(),
            averages.values.toList(),
            "средние стоимости должны быть отсортированы по убыванию",
        )

        assertEquals(3, full.getMostValuablePlayers().size)
        assertTrue(full.getMostPopularAgencyByCountry().isNotEmpty())

        val (below, above) = full.getGoalsShareByMedianCost()
        assertEquals(1.0, below + above, 1e-9)
    }
}
