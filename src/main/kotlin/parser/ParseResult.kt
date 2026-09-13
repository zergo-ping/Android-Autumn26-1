package parser

import model.Player

/**
 * Итог разбора CSV: распознанные игроки и количество строк,
 */
data class ParseResult(
    val players: List<Player>,
    val skippedCount: Int,
)
