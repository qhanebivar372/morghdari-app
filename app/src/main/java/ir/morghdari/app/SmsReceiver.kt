package ir.morghdari.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

// دریافت پاسخ دستگاه؛ فقط پیامک‌های شماره‌ی دستگاه ذخیره می‌شوند
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = ctx.getSharedPreferences("app", Context.MODE_PRIVATE)
        val dev = Util.last10(prefs.getString("device", "") ?: "")
        if (dev.length < 7) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val from = msgs.firstOrNull()?.originatingAddress ?: return
        if (Util.last10(from) != dev) return
        val body = msgs.joinToString("") { it.messageBody ?: "" }.trim()
        val e = prefs.edit()
        if (body.startsWith("T:")) e.putString("status", body)
        if (body.startsWith("HEAT:")) e.putString("cfg", body)
        if (body.startsWith("ADM:")) e.putString("users", body)
        val log = (prefs.getString("log", "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
        log.add(0, Util.now() + "  ←  " + body)
        e.putString("log", log.take(15).joinToString("\n")).apply()
        ctx.sendBroadcast(Intent(Util.ACTION_UPDATE).setPackage(ctx.packageName))
    }
}
