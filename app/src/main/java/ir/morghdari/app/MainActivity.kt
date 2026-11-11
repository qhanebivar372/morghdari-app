package ir.morghdari.app

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    // بخش کاربران فقط وقتی لازم است که در دستگاه ENABLE_USERS = 1 باشد
    private val SHOW_USERS = false
    private lateinit var prefs: SharedPreferences
    private lateinit var etDevice: EditText
    private lateinit var etPass: EditText
    private lateinit var tvStatus: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvLog: TextView
    private lateinit var tvUsers: TextView
    private val green = Color.parseColor("#1F6B4A")
    private val relayNames = listOf("هیتر", "درب", "فن تهویه", "لامپ", "بستن درب", "کانال ۶", "کانال ۷", "کانال ۸")
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = refresh()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("app", MODE_PRIVATE)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.parseColor("#F4F1EA")); addView(root) })

        root.addView(title("کنترل مرغداری"))

        // اتصال
        root.addView(section("اتصال به دستگاه"))
        etDevice = edit("شماره‌ی سیم‌کارت دستگاه", InputType.TYPE_CLASS_PHONE).also { it.setText(prefs.getString("device", "")) }
        etPass = edit("رمز فرمان‌ها", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD).also { it.setText(prefs.getString("pass", "")) }
        root.addView(card(etDevice, etPass, btn("ذخیره") { save() }))

        // وضعیت
        root.addView(section("وضعیت"))
        tvStatus = text("هنوز وضعیتی دریافت نشده"); tvInfo = text("")
        root.addView(card(tvStatus, tvInfo, btn("دریافت وضعیت") { send("STAT") }))

        // رله‌ها
        root.addView(section("کنترل رله‌ها"))
        val relays = card()
        for (i in 1..8) {
            if (i == 5) continue  // رله‌ی ۵ بستن درب است و با ردیف درب کنترل می‌شود
            relays.addView(text("رله $i · ${relayNames[i - 1]}").apply { setPadding(0, dp(8), 0, 0); textSize = 16f })
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            if (i == 2) {
                row.addView(btn("باز کن") { send("DOOROPEN") }, w1())
                row.addView(btn("ببند") { send("DOORCLOSE") }, w1())
            } else {
                row.addView(btn("روشن") { send("R${i}ON") }, w1())
                row.addView(btn("خاموش") { send("R${i}OFF") }, w1())
            }
            if (i <= 4) row.addView(btn("خودکار") { send("AUTO$i") }, w1())
            relays.addView(row)
        }
        relays.addView(text("روشن و خاموش یا باز و بسته، رله را دستی می‌کند. «خودکار» دوباره کنترل را به دستگاه برمی‌گرداند. کانال ۵ تا ۸ فقط دستی‌اند.").apply { textSize = 13f })
        root.addView(relays)

        // تایمر درب
        root.addView(section("تایمر درب"))
        val dOpen = edit("ساعت باز شدن (مثل 06:30)", InputType.TYPE_CLASS_DATETIME)
        val dClose = edit("ساعت بسته شدن (مثل 19:00)", InputType.TYPE_CLASS_DATETIME)
        val dSecOpen = edit("مدت برق رله‌ی باز کردن (ثانیه، مثل 3)", InputType.TYPE_CLASS_NUMBER)
        val dSecClose = edit("مدت برق رله‌ی بستن (ثانیه، مثل 2)", InputType.TYPE_CLASS_NUMBER)
        root.addView(card(dOpen, dClose,
            btn("فعال کردن تایمر درب") {
                val re = Regex("^([01]?\\d|2[0-3])(:[0-5]\\d)?$")
                if (re.matches(dOpen.text.toString().trim()) && re.matches(dClose.text.toString().trim()))
                    send("SET DOOR ${dOpen.text.toString().trim()} ${dClose.text.toString().trim()}")
                else toast("ساعت را مثل 06:30 بنویسید")
            },
            btn("برگشت به سنسور نور") { send("SET DOOR SENSOR") },
            dSecOpen, dSecClose,
            btn("ارسال مدت برق رله‌های درب") {
                val a = dSecOpen.text.toString().trim().toIntOrNull()
                val b = dSecClose.text.toString().trim().toIntOrNull()
                if (a != null && b != null && a in 1..60 && b in 1..60) send("SET DOORT $a $b")
                else toast("مدت را بین ۱ تا ۶۰ ثانیه بنویسید")
            },
            text("در حالت تایمر، سنسور نور درب را کنترل نمی‌کند. ساعت دستگاه از شبکه‌ی سیم‌کارت گرفته می‌شود.").apply { textSize = 13f }))

        // تنظیمات
        root.addView(section("تنظیمات خودکار"))
        val heat = edit("دمای روشن شدن هیتر (°C)", InputType.TYPE_CLASS_NUMBER)
        val hum = edit("رطوبت روشن شدن فن (٪)", InputType.TYPE_CLASS_NUMBER)
        val lopen = edit("آستانه‌ی نور باز شدن درب (٪)", InputType.TYPE_CLASS_NUMBER)
        val lon = edit("ساعت روشن شدن لامپ (۰ تا ۲۳)", InputType.TYPE_CLASS_NUMBER)
        val loff = edit("ساعت خاموش شدن لامپ (۰ تا ۲۳)", InputType.TYPE_CLASS_NUMBER)
        root.addView(card(heat, hum, lopen, lon, loff,
            btn("ارسال تنظیمات") {
                if (heat.text.isNotBlank()) send("SET HEAT ${heat.text}")
                if (hum.text.isNotBlank()) send("SET HUM ${hum.text}")
                if (lopen.text.isNotBlank()) send("SET LOPEN ${lopen.text}")
                if (lon.text.isNotBlank() && loff.text.isNotBlank()) send("SET LAMP ${lon.text} ${loff.text}")
            },
            btn("دریافت تنظیمات فعلی") { send("CFG") },
            btn("همگام‌سازی ساعت دستگاه با گوشی") {
                val c = java.util.Calendar.getInstance()
                send("TIME %02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE)))
            },
            text("حد بسته شدن درب خودکار نصف حد باز شدن است.").apply { textSize = 13f }))

        // کاربران
        tvUsers = text("")
        if (SHOW_USERS) {
        root.addView(section("کاربران (فقط مدیر)"))
        val un = edit("شماره‌ی کاربر", InputType.TYPE_CLASS_PHONE)
        val up = edit("دسترسی‌ها مثل V2458A", InputType.TYPE_CLASS_TEXT).also { it.setText("V") }
        tvUsers.text = prefs.getString("users", "") ?: ""
        root.addView(card(un, up,
            btn("افزودن یا ویرایش کاربر") { sendAdmin("ADD ${un.text} ${up.text}") },
            btn("حذف کاربر") { sendAdmin("DEL ${un.text}") },
            btn("فهرست کاربران") { sendAdmin("LIST") },
            btn("انتقال مدیریت به این شماره") { sendAdmin("ADMIN ${un.text}") },
            tvUsers,
            text("دسترسی‌ها: V مشاهده، ۱ تا ۸ هر رله، S تنظیمات، A هشدار. مدیر جدید باید کد تأییدی را که پیامک می‌شود با CONFIRM پس بفرستد.").apply { textSize = 13f }))
        }

        // گزارش
        root.addView(section("پیام‌های اخیر"))
        tvLog = text("")
        root.addView(card(tvLog))

        if (Build.VERSION.SDK_INT >= 23 &&
            (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
             checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(updateReceiver, IntentFilter(Util.ACTION_UPDATE), RECEIVER_NOT_EXPORTED)
        else registerReceiver(updateReceiver, IntentFilter(Util.ACTION_UPDATE))
        refresh()
    }

    override fun onPause() { super.onPause(); unregisterReceiver(updateReceiver) }

    private fun save() {
        prefs.edit().putString("device", etDevice.text.toString().trim()).putString("pass", etPass.text.toString().trim()).apply()
        toast("ذخیره شد")
    }

    private fun refresh() {
        val st = prefs.getString("status", null)
        if (st != null) {
            val m = st.split(" ").mapNotNull { val k = it.split(":", limit = 2); if (k.size == 2) k[0] to k[1] else null }.toMap()
            fun rel(k: String): String { val v = m[k] ?: return "؟"; return (if (v.endsWith("1")) "روشن" else "خاموش") + (if (v.startsWith("M")) " (دستی)" else " (خودکار)") }
            tvStatus.text = "دما ${m["T"]}°  ·  رطوبت ${m["H"]}٪  ·  نور ${m["L"]}٪"
            tvInfo.text = "هیتر: ${rel("R1")}\nدرب: ${rel("R2")} (باز و بسته با دو رله‌ی ۲ و ۵)\nفن: ${rel("R3")}\nلامپ: ${rel("R4")}\nکانال ۶: ${rel("R6")}\nکانال ۷: ${rel("R7")}\nکانال ۸: ${rel("R8")}\nساعت دستگاه: ${m["CLK"]}   دسترسی من: ${m["P"]}"
        }
        tvUsers.text = prefs.getString("users", "") ?: ""
        tvLog.text = prefs.getString("log", "") ?: ""
    }

    // ارسال فرمان معمولی
    private fun send(cmd: String) = sms(cmd)
    // فرمان مدیریتی با رمز
    private fun sendAdmin(cmd: String) {
        val pw = prefs.getString("pass", "") ?: ""
        if (pw.isEmpty()) { toast("اول رمز را ذخیره کنید"); return }
        sms("$pw $cmd")
    }

    private fun sms(text: String) {
        val dev = prefs.getString("device", "") ?: ""
        if (Util.last10(dev).length < 7) { toast("اول شماره‌ی دستگاه را ذخیره کنید"); return }
        try {
            val mgr = if (Build.VERSION.SDK_INT >= 31) getSystemService(SmsManager::class.java) else @Suppress("DEPRECATION") SmsManager.getDefault()
            mgr.sendTextMessage(dev, null, text, null, null)
            val log = (prefs.getString("log", "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
            val shown = if (text.split(" ").getOrNull(1) in listOf("ADD", "DEL", "LIST", "ADMIN", "PASS")) text.substringAfter(" ").let { "••• $it" } else text
            log.add(0, Util.now() + "  →  " + shown)
            prefs.edit().putString("log", log.take(15).joinToString("\n")).apply()
            toast("ارسال شد")
            refresh()
        } catch (e: Exception) { toast("ارسال نشد: اجازه‌ی پیامک را بدهید") }
    }

    // --- ساخت رابط ---
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun w1() = LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) }
    private fun title(s: String) = TextView(this).apply { text = s; textSize = 26f; setTextColor(Color.parseColor("#1D2B24")); setTypeface(typeface, android.graphics.Typeface.BOLD) }
    private fun section(s: String) = TextView(this).apply { text = s; textSize = 17f; setTextColor(Color.parseColor("#1D2B24")); setTypeface(typeface, android.graphics.Typeface.BOLD); setPadding(0, dp(18), 0, dp(8)) }
    private fun text(s: String) = TextView(this).apply { text = s; textSize = 15f; setTextColor(Color.parseColor("#3F4943")); setPadding(0, dp(4), 0, dp(4)) }
    private fun edit(hint: String, type: Int) = EditText(this).apply { this.hint = hint; inputType = type; textSize = 16f; textDirection = View.TEXT_DIRECTION_LTR; gravity = Gravity.START; minHeight = dp(48) }
    private fun btn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; setTextColor(Color.WHITE); isAllCaps = false; minHeight = dp(48)
        background = GradientDrawable().apply { setColor(green); cornerRadius = dp(12).toFloat() }
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) }
    }
    private fun card(vararg views: View) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(10))
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(16).toFloat(); setStroke(dp(1), Color.parseColor("#E3DED2")) }
        views.forEach { addView(it) }
    }
}
