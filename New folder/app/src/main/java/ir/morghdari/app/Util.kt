package ir.morghdari.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Util {
    const val ACTION_UPDATE = "ir.morghdari.app.UPDATE"
    fun last10(s: String): String = s.filter { it.isDigit() }.takeLast(10)
    fun now(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())
}
