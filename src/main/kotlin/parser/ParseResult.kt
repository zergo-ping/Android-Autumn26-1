package parser

import model.Player

/**
 * Итог разбора CSV: распознанные игроки и количество строк,
 * отброшенных как некорректные (см. правила маппинга в README).
 */
data class ParseResult(
    val players: List<Player>,
    val skippedCount: Int,
)
