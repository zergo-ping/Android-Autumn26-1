package model

enum class Position(val russianName: String) {
    FORWARD("нападающий"),
    MIDFIELD("полузащитник"),
    DEFENDER("защитник"),
    GOALKEEPER("вратарь");

    companion object {
        fun fromCsvTokenOrNull(token: String): Position? =
            entries.firstOrNull { it.name.equals(token.trim(), ignoreCase = true) }
    }
}
