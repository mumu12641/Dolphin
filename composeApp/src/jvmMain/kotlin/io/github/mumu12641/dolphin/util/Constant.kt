package io.github.mumu12641.dolphin.util

object Constant {
    val VENUES = listOf("西体", "光体", "游泳馆")

    val TIME_SLOTS = listOf(
        "8:00:00-10:00:00", "10:00:00-12:00:00", "12:00:00-14:00:00",
        "14:00:00-16:00:00", "16:00:00-18:00:00", "18:00:00-20:00:00", "20:00:00-22:00:00"
    )

    val VENUE_IDS = mapOf(
        "西体" to "69",
        "光体" to "45",
        "游泳馆" to "117"
    )

    val VENUE_COLS = mapOf(
        "西体" to 3,
        "光体" to 5,
        "游泳馆" to 3
    )

    val COURT_IDS = mapOf(
        "西体" to mapOf(
            9 to "584", 7 to "300", 5 to "298", 6 to "299",
            4 to "297", 8 to "301", 1 to "134", 2 to "295", 3 to "296"
        ),
        "光体" to mapOf(
            1 to "110", 2 to "133", 3 to "215", 4 to "216", 5 to "218", 6 to "217",
            7 to "219", 8 to "220", 9 to "221", 10 to "222", 11 to "223", 12 to "224",
            13 to "368", 14 to "369", 15 to "370", 16 to "371", 17 to "372", 18 to "373",
            19 to "374", 20 to "375", 21 to "376", 22 to "377"
        ),
        "游泳馆" to mapOf(
            1 to "587", 2 to "588", 3 to "589", 4 to "590", 5 to "591",
            6 to "592", 7 to "593", 8 to "594", 9 to "595"
        )
    )

    val VENUE_COURT_COUNTS = COURT_IDS.mapValues { it.value.size }
}
