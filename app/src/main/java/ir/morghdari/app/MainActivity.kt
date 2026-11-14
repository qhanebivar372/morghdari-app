package ir.morghdari.app

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    // بخش کاربران فقط وقتی لازم است که در دستگاه ENABLE_USERS = 1 باشد
    private val showUsers = false

    private lateinit var prefs: SharedPreferences
    private lateinit var content: FrameLayout
    private lateinit var nav: LinearLayout
    private var tab = 0

    private val cBg = Color.parseColor("#F6F1E7")
    private val cCard = Color.WHITE
    private val cLine = Color.parseColor("#E7DFCF")
    private val cGreen = Color.parseColor("#1F6B4A")
    private val cGreenDark = Color.parseColor("#134A33")
    private val cAmber = Color.parseColor("#F2B84B")
    private val cText = Color.parseColor("#2B2A26")
    private val cMuted = Color.parseColor("#7A7468")
    private val cChipOn = Color.parseColor("#DDEBE3")
    private val cChipOff = Color.parseColor("#EFE9DB")

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            // صفحه‌هایی که فیلد متنی ندارند دوباره ساخته می‌شوند تا نوشته‌ی کاربر پاک نشود
            if (tab == 0 || tab == 1) show(tab)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("app", MODE_PRIVATE)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        window.statusBarColor = cGreenDark

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(cBg)
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.setBackgroundColor(cCard)
        nav.elevation = dp(8).toFloat()
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(64)))
        setContentView(root)

        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS), 1)
        }
        show(0)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(updateReceiver, IntentFilter(Util.ACTION_UPDATE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, IntentFilter(Util.ACTION_UPDATE))
        }
        show(tab)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(updateReceiver)
    }

    // ---------- ناوبری ----------
    private fun show(i: Int) {
        tab = i
        content.removeAllViews()
        val page: View = when (i) {
            0 -> homePage()
            1 -> doorPage()
            2 -> autoPage()
            else -> settingsPage()
        }
        content.addView(page, FrameLayout.LayoutParams(-1, -1))
        buildNav()
    }

    private fun buildNav() {
        nav.removeAllViews()
        val items = listOf("🏠" to "خانه", "🚪" to "درب", "🎛" to "خودکار", "💬" to "پیام‌ها")
        for ((idx, it) in items.withIndex()) {
            val sel = idx == tab
            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.gravity = Gravity.CENTER
            col.addView(label(it.first, 20f, cText))
            col.addView(label(it.second, 12f, if (sel) cGreen else cMuted, bold = sel))
            if (sel) col.background = roundBg(cChipOn, 18)
            col.setOnClickListener { show(idx) }
            val lp = LinearLayout.LayoutParams(0, -1, 1f)
            lp.setMargins(dp(8), dp(6), dp(8), dp(6))
            nav.addView(col, lp)
        }
    }

    // ---------- صفحه‌ی خانه ----------
    private fun status(): Map<String, String> {
        val st = prefs.getString("status", null) ?: return emptyMap()
        val m = HashMap<String, String>()
        for (part in st.split(" ")) {
            val k = part.split(":", limit = 2)
            if (k.size == 2) m[k[0]] = k[1]
        }
        return m
    }

    private fun relayOn(m: Map<String, String>, k: String): Boolean = (m[k] ?: "").endsWith("1")
    private fun relayManual(m: Map<String, String>, k: String): Boolean = (m[k] ?: "").startsWith("M")

    private fun homePage(): View {
        val m = status()
        val col = pageColumn()

        // سربرگ
        val head = LinearLayout(this)
        head.orientation = LinearLayout.VERTICAL
        head.setPadding(dp(20), dp(24), dp(20), dp(22))
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cGreen, cGreenDark))
        g.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, dp(28).toFloat(), dp(28).toFloat(), dp(28).toFloat(), dp(28).toFloat())
        head.background = g
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val titles = LinearLayout(this)
        titles.orientation = LinearLayout.VERTICAL
        titles.addView(label("سالن ۱", 13f, Color.parseColor("#BFE3CF")))
        titles.addView(label("مرغداری من 🐔", 26f, Color.WHITE, bold = true))
        top.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        val refresh = label("↻", 24f, Color.WHITE)
        refresh.gravity = Gravity.CENTER
        refresh.background = roundBg(Color.parseColor("#2E8B62"), 24)
        refresh.setOnClickListener { sendSms("STAT") }
        top.addView(refresh, LinearLayout.LayoutParams(dp(48), dp(48)))
        head.addView(top)
        val t = prefs.getString("status_time", null)
        head.addView(label(if (t == null) "هنوز وضعیتی دریافت نشده؛ دکمه‌ی ↻ را بزنید" else "آخرین وضعیت ${fa(t)}", 13f, Color.parseColor("#BFE3CF")))
        col.addView(head)

        // اطلاعات سنسورها
        val stats = LinearLayout(this)
        stats.orientation = LinearLayout.HORIZONTAL
        stats.addView(statTile("🌡", fa(m["T"] ?: "--") + "°", "دما"), tileLp())
        stats.addView(statTile("💧", fa(m["H"] ?: "--") + "٪", "رطوبت"), tileLp())
        stats.addView(statTile("☀️", fa(m["L"] ?: "--") + "٪", "نور"), tileLp())
        val sp = LinearLayout.LayoutParams(-1, -2)
        sp.setMargins(dp(12), dp(14), dp(12), 0)
        col.addView(stats, sp)

        col.addView(sectionTitle("تجهیزات"))
        val grid1 = LinearLayout(this)
        grid1.orientation = LinearLayout.HORIZONTAL
        grid1.addView(deviceCard(m, 1, "🔥", "هیتر"), tileLp())
        grid1.addView(doorCard(m), tileLp())
        val grid2 = LinearLayout(this)
        grid2.orientation = LinearLayout.HORIZONTAL
        grid2.addView(deviceCard(m, 3, "🌀", "فن تهویه"), tileLp())
        grid2.addView(lampCard(m), tileLp())
        val gp = LinearLayout.LayoutParams(-1, -2)
        gp.setMargins(dp(12), 0, dp(12), 0)
        col.addView(grid1, gp)
        col.addView(grid2, gp)

        col.addView(sectionTitle("کانال‌های آزاد"))
        val extra = LinearLayout(this)
        extra.orientation = LinearLayout.HORIZONTAL
        for (i in listOf(4, 6)) extra.addView(extraCard(m, i), tileLp())
        col.addView(extra, gp)
        val extra2 = LinearLayout(this)
        extra2.orientation = LinearLayout.HORIZONTAL
        for (i in listOf(7, 8)) extra2.addView(extraCard(m, i), tileLp())
        col.addView(extra2, gp)

        col.addView(spacer(24))
        return scroll(col)
    }

    private fun statTile(emoji: String, value: String, name: String): View {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER
        c.setPadding(dp(8), dp(14), dp(8), dp(14))
        c.background = cardBg()
        c.addView(label(emoji, 22f, cText))
        c.addView(label(value, 22f, cText, bold = true))
        c.addView(label(name, 12f, cMuted))
        return c
    }

    private fun deviceCard(m: Map<String, String>, idx: Int, emoji: String, name: String): View {
        val k = "R$idx"
        val on = relayOn(m, k)
        val known = m.containsKey(k)
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(14), dp(14), dp(14), dp(12))
        c.background = cardBg()
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(label(emoji, 24f, cText))
        val nm = label(" $name", 16f, cText, bold = true)
        row.addView(nm)
        c.addView(row)
        c.addView(chip(if (!known) "نامشخص" else if (on) "روشن" else "خاموش", on))
        if (known) c.addView(label(if (relayManual(m, k)) "دستی" else "خودکار", 12f, cMuted))
        c.addView(smallBtn(if (on) "خاموش کن" else "روشن کن", true) { sendSms(if (on) "R${idx}OFF" else "R${idx}ON") })
        c.addView(smallBtn("خودکار", false) { sendSms("AUTO$idx") })
        return c
    }

    private fun doorCard(m: Map<String, String>): View {
        val known = m.containsKey("R2")
        val closed = relayOn(m, "R2")
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(14), dp(14), dp(14), dp(12))
        c.background = cardBg()
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(label("🚪", 24f, cText))
        row.addView(label(" درب", 16f, cText, bold = true))
        c.addView(row)
        c.addView(chip(if (!known) "نامشخص" else if (closed) "بسته" else "باز", !closed && known))
        if (known) c.addView(label(if (relayManual(m, "R2")) "دستی" else "خودکار", 12f, cMuted))
        c.addView(smallBtn(if (closed) "باز کن" else "ببند", true) { sendSms(if (closed) "DOOROPEN" else "DOORCLOSE") })
        c.addView(smallBtn("جزئیات ‹", false) { show(1) })
        return c
    }

    private fun lampCard(m: Map<String, String>): View {
        val lp = m["LP"] ?: ""
        val known = lp.length >= 2
        val manual = known && lp[0] == 'M'
        val pct = if (known) (lp.substring(1).toIntOrNull() ?: 0) else 0
        val on = pct > 0
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(14), dp(14), dp(14), dp(12))
        c.background = cardBg()
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(label("💡", 24f, cText))
        row.addView(label(" لامپ", 16f, cText, bold = true))
        c.addView(row)
        c.addView(chip(if (!known) "نامشخص" else if (on) "روشن ${fa(pct.toString())}٪" else "خاموش", on))
        if (known) c.addView(label(if (manual) "دستی" else "خودکار", 12f, cMuted))
        c.addView(smallBtn(if (on) "خاموش کن" else "روشن کن", true) { sendSms(if (on) "LAMP OFF" else "LAMP ON") })
        c.addView(smallBtn("خودکار", false) { sendSms("LAMP AUTO") })
        return c
    }

    private fun extraCard(m: Map<String, String>, idx: Int): View {
        val k = "R$idx"
        val on = relayOn(m, k)
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(10), dp(12), dp(10), dp(10))
        c.background = cardBg()
        c.gravity = Gravity.CENTER_HORIZONTAL
        c.addView(label("کانال ${fa(idx.toString())}", 14f, cText, bold = true))
        c.addView(chip(if (!m.containsKey(k)) "نامشخص" else if (on) "روشن" else "خاموش", on))
        c.addView(smallBtn(if (on) "خاموش" else "روشن", true) { sendSms(if (on) "R${idx}OFF" else "R${idx}ON") })
        return c
    }

    // ---------- صفحه‌ی درب ----------
    private fun doorPage(): View {
        val m = status()
        val col = pageColumn()
        col.addView(pageHeader("🚪", "درب سالن", "رله‌ی ۲ باز می‌کند و رله‌ی ۵ می‌بندد"))
        val known = m.containsKey("R2")
        val closed = relayOn(m, "R2")

        val st = LinearLayout(this)
        st.orientation = LinearLayout.VERTICAL
        st.gravity = Gravity.CENTER
        st.setPadding(dp(16), dp(22), dp(16), dp(18))
        st.background = cardBg()
        st.addView(label("وضعیت فعلی", 13f, cMuted))
        st.addView(label(if (!known) "نامشخص" else if (closed) "بسته است" else "باز است", 32f, cText, bold = true))
        st.addView(label(if (known) (if (relayManual(m, "R2")) "حالت دستی" else "حالت خودکار") else "برای دریافت وضعیت ↻ را بزنید", 13f, cMuted))
        val two = LinearLayout(this)
        two.orientation = LinearLayout.HORIZONTAL
        two.addView(bigBtn("باز کن", true) { sendSms("DOOROPEN") }, LinearLayout.LayoutParams(0, dp(52), 1f).also { it.setMargins(dp(4), dp(14), dp(4), 0) })
        two.addView(bigBtn("ببند", true) { sendSms("DOORCLOSE") }, LinearLayout.LayoutParams(0, dp(52), 1f).also { it.setMargins(dp(4), dp(14), dp(4), 0) })
        st.addView(two)
        st.addView(bigBtn("برگشت به حالت خودکار", false) { sendSms("AUTO2") }, LinearLayout.LayoutParams(-1, dp(48)).also { it.setMargins(dp(4), dp(8), dp(4), 0) })
        col.addView(st, cardLp())

        // تایمر
        val dOpen = edit("ساعت باز شدن (مثل 06:30)", InputType.TYPE_CLASS_DATETIME)
        val dClose = edit("ساعت بسته شدن (مثل 19:00)", InputType.TYPE_CLASS_DATETIME)
        val tc = card()
        tc.addView(label("⏰ تایمر ساعتی", 17f, cText, bold = true))
        tc.addView(label("با تایمر، درب فقط در این ساعت‌ها حرکت می‌کند و سنسور نور نادیده گرفته می‌شود.", 13f, cMuted))
        tc.addView(dOpen)
        tc.addView(dClose)
        tc.addView(bigBtn("فعال کردن تایمر", true) {
            val re = Regex("^([01]?[0-9]|2[0-3])(:[0-5][0-9])?$")
            val a = dOpen.text.toString().trim()
            val b = dClose.text.toString().trim()
            if (re.matches(a) && re.matches(b)) sendSms("SET DOOR $a $b") else toast("ساعت را مثل 06:30 بنویسید")
        }, btnLp())
        tc.addView(bigBtn("برگشت به سنسور نور", false) { sendSms("SET DOOR SENSOR") }, btnLp())
        col.addView(tc, cardLp())

        // مدت پالس
        val sOpen = edit("مدت برق رله‌ی باز کردن (ثانیه، مثل 3)", InputType.TYPE_CLASS_NUMBER)
        val sClose = edit("مدت برق رله‌ی بستن (ثانیه، مثل 2)", InputType.TYPE_CLASS_NUMBER)
        val pc = card()
        pc.addView(label("⏱ مدت حرکت درب", 17f, cText, bold = true))
        pc.addView(label("هر فرمان به رله فقط همین چند ثانیه برق می‌دهد. میکروسوییچ‌های درب حرکت را در بالا و پایین قطع می‌کنند.", 13f, cMuted))
        pc.addView(sOpen)
        pc.addView(sClose)
        pc.addView(bigBtn("ارسال مدت‌ها", true) {
            val a = sOpen.text.toString().trim().toIntOrNull()
            val b = sClose.text.toString().trim().toIntOrNull()
            if (a != null && b != null && a in 1..60 && b in 1..60) sendSms("SET DOORT $a $b") else toast("مدت را بین ۱ تا ۶۰ ثانیه بنویسید")
        }, btnLp())
        col.addView(pc, cardLp())

        // آستانه‌ی نور
        val lo = edit("آستانه‌ی نور برای باز شدن (٪)", InputType.TYPE_CLASS_NUMBER)
        val lc = card()
        lc.addView(label("☀️ سنسور نور", 17f, cText, bold = true))
        lc.addView(label("وقتی نور از «آستانه‌ی باز شدن» بیشتر شود درب باز می‌شود و وقتی از «آستانه‌ی بسته شدن» کمتر شود بسته می‌شود. اگر بسته شدن را نفرستید، نصف آستانه‌ی باز شدن است. نور باید ۱۰ دقیقه پایدار بماند. عدد نور فعلی را در صفحه‌ی خانه ببینید.", 13f, cMuted))
        val lcl = edit("آستانه‌ی نور برای بسته شدن (٪)", InputType.TYPE_CLASS_NUMBER)
        lc.addView(lo)
        lc.addView(bigBtn("ارسال آستانه‌ی باز شدن", true) {
            val a = lo.text.toString().trim().toIntOrNull()
            if (a != null && a in 1..100) sendSms("SET LOPEN $a") else toast("عدد را بین ۱ تا ۱۰۰ بنویسید")
        }, btnLp())
        lc.addView(lcl)
        lc.addView(bigBtn("ارسال آستانه‌ی بسته شدن", true) {
            val a = lcl.text.toString().trim().toIntOrNull()
            if (a != null && a in 1..99) sendSms("SET LCLOSE $a") else toast("عدد را بین ۱ تا ۹۹ بنویسید")
        }, btnLp())
        col.addView(lc, cardLp())

        col.addView(spacer(24))
        return scroll(col)
    }

    // ---------- صفحه‌ی خودکارسازی ----------
    private fun autoPage(): View {
        val col = pageColumn()
        col.addView(pageHeader("🎛", "خودکارسازی", "قانون‌های کار هیتر، فن و لامپ"))

        val heat = edit("دمای روشن شدن هیتر (°C)", InputType.TYPE_CLASS_NUMBER)
        val hc = card()
        hc.addView(label("🔥 هیتر · رله ۱", 17f, cText, bold = true))
        hc.addView(label("زیر این دما روشن می‌شود و ۲ درجه بالاتر خاموش می‌شود.", 13f, cMuted))
        hc.addView(heat)
        hc.addView(bigBtn("ارسال", true) {
            val a = heat.text.toString().trim().toIntOrNull()
            if (a != null && a in 0..50) sendSms("SET HEAT $a") else toast("عدد را بین ۰ تا ۵۰ بنویسید")
        }, btnLp())
        col.addView(hc, cardLp())

        val hum = edit("رطوبت روشن شدن فن (٪)", InputType.TYPE_CLASS_NUMBER)
        val fc = card()
        fc.addView(label("🌀 فن تهویه · رله ۳", 17f, cText, bold = true))
        fc.addView(label("بالای این رطوبت روشن می‌شود و ۳ درصد پایین‌تر خاموش می‌شود. هر ساعت هم ۱۰ دقیقه به‌طور خودکار روشن می‌شود.", 13f, cMuted))
        fc.addView(hum)
        fc.addView(bigBtn("ارسال", true) {
            val a = hum.text.toString().trim().toIntOrNull()
            if (a != null && a in 0..80) sendSms("SET HUM $a") else toast("عدد را بین ۰ تا ۸۰ بنویسید")
        }, btnLp())
        col.addView(fc, cardLp())

        val lon = edit("ساعت روشن شدن (۰ تا ۲۳)", InputType.TYPE_CLASS_NUMBER)
        val loff = edit("ساعت خاموش شدن (۰ تا ۲۳)", InputType.TYPE_CLASS_NUMBER)
        val lc = card()
        lc.addView(label("💡 لامپ روشنایی · رله ۴", 17f, cText, bold = true))
        lc.addView(label("حد مجاز روشن بودن لامپ (ساعت کامل)؛ مثلاً از ۵ تا ۲۱. با نور ۱۶ ساعته پایین‌تر، لامپ فقط پیش از طلوع و بعد از غروب در همین بازه روشن می‌شود.", 13f, cMuted))
        lc.addView(lon)
        lc.addView(loff)
        lc.addView(bigBtn("ارسال", true) {
            val a = lon.text.toString().trim().toIntOrNull()
            val b = loff.text.toString().trim().toIntOrNull()
            if (a != null && b != null && a in 0..23 && b in 0..23) sendSms("SET LAMP $a $b") else toast("ساعت‌ها را بین ۰ تا ۲۳ بنویسید")
        }, btnLp())
        col.addView(lc, cardLp())

        val fm = edit("مدت غروب و طلوع مصنوعی (دقیقه، مثل 30)", InputType.TYPE_CLASS_NUMBER)
        val fx = edit("بیشترین روشنایی (٪، مثل 80)", InputType.TYPE_CLASS_NUMBER)
        val fl = edit("روشنایی دستی (۰ تا ۱۰۰٪)", InputType.TYPE_CLASS_NUMBER)
        val lfc = card()
        lfc.addView(label("🌇 غروب و طلوع مصنوعی", 17f, cText, bold = true))
        lfc.addView(label("لامپ LED به‌جای روشن و خاموش ناگهانی، در این مدت آرام کم و زیاد می‌شود.", 13f, cMuted))
        lfc.addView(fm)
        lfc.addView(fx)
        lfc.addView(bigBtn("ارسال", true) {
            val a = fm.text.toString().trim().toIntOrNull()
            val b = fx.text.toString().trim().toIntOrNull()
            if (a != null && b != null && a in 1..120 && b in 5..100) sendSms("SET LFADE $a $b") else toast("مدت ۱ تا ۱۲۰ و روشنایی ۵ تا ۱۰۰ باشد")
        }, btnLp())
        lfc.addView(fl)
        lfc.addView(bigBtn("تنظیم روشنایی دستی", false) {
            val a = fl.text.toString().trim().toIntOrNull()
            if (a != null && a in 0..100) sendSms("LAMP $a") else toast("عدد را بین ۰ تا ۱۰۰ بنویسید")
        }, btnLp())
        col.addView(lfc, cardLp())

        val lt = edit("هدف مجموع نور روز (ساعت، مثل 16)", InputType.TYPE_CLASS_NUMBER)
        val ls = edit("افزایش روزانه‌ی هر طرف (دقیقه، مثل 15)", InputType.TYPE_CLASS_NUMBER)
        ls.setText("15")
        val ldc = card()
        ldc.addView(label("🌅 نور ۱۶ ساعته با سنسور نور", 17f, cText, bold = true))
        ldc.addView(label("سنسور نور بیرون، طلوع و غروب واقعی را پیدا می‌کند. هر روز ۱۵ دقیقه زودتر از طلوع روشن می‌شود و ۱۵ دقیقه دیرتر از غروب خاموش می‌شود تا مجموع نور روز و لامپ به هدف برسد. ساعت‌های بالا (روشن و خاموش) حد مجاز هستند. چند روز اول دستگاه طلوع و غروب را یاد می‌گیرد. آستانه‌ی روز و شب لامپ جدا از درب تنظیم می‌شود.", 13f, cMuted))
        ldc.addView(lt)
        ldc.addView(ls)
        ldc.addView(bigBtn("فعال کردن", true) {
            val a = lt.text.toString().trim().toIntOrNull()
            val b = ls.text.toString().trim().toIntOrNull()
            if (a != null && b != null && a in 1..24 && b in 5..60) sendSms("SET LLIGHT $a $b") else toast("هدف ۱ تا ۲۴ ساعت و گام ۵ تا ۶۰ دقیقه")
        }, btnLp())
        val dk = edit("زیر چند درصد نور = شب (مثل 10)", InputType.TYPE_CLASS_NUMBER)
        val br = edit("بالای چند درصد نور = روز (مثل 60)", InputType.TYPE_CLASS_NUMBER)
        ldc.addView(label("آستانه‌های لامپ (جدا از درب)", 14f, cText, bold = true))
        ldc.addView(dk)
        ldc.addView(br)
        ldc.addView(bigBtn("ارسال آستانه‌ها", false) {
            val a = dk.text.toString().trim().toIntOrNull()
            val b = br.text.toString().trim().toIntOrNull()
            if (a != null && b != null && a in 1..99 && b > a && b <= 100) sendSms("SET LLDR $a $b") else toast("عدد شب کمتر از عدد روز باشد")
        }, btnLp())
        ldc.addView(bigBtn("غیرفعال (فقط ساعت ثابت)", false) { sendSms("SET LLIGHT 0") }, btnLp())
        col.addView(ldc, cardLp())

        val cfg = card()
        cfg.addView(label("📋 تنظیمات فعلی دستگاه", 17f, cText, bold = true))
        val cfgText = label(prefs.getString("cfg", "هنوز دریافت نشده") ?: "", 14f, cMuted)
        cfg.addView(cfgText)
        cfg.addView(bigBtn("دریافت تنظیمات", false) { sendSms("CFG") }, btnLp())
        col.addView(cfg, cardLp())

        col.addView(spacer(24))
        return scroll(col)
    }

    // ---------- صفحه‌ی پیام‌ها و اتصال ----------
    private fun settingsPage(): View {
        val col = pageColumn()
        col.addView(pageHeader("💬", "پیام‌ها و اتصال", "اپ با پیامک به دستگاه فرمان می‌دهد"))

        val dev = edit("شماره‌ی سیم‌کارت دستگاه", InputType.TYPE_CLASS_PHONE)
        dev.setText(prefs.getString("device", ""))
        val pass = edit("رمز فرمان‌ها", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        pass.setText(prefs.getString("pass", ""))
        val cc = card()
        cc.addView(label("📡 اتصال به دستگاه", 17f, cText, bold = true))
        cc.addView(label("فقط شماره‌ی مدیر می‌تواند فرمان بدهد. هر فرمان یک پیامک می‌فرستد و پاسخ دستگاه همین‌جا ثبت می‌شود.", 13f, cMuted))
        cc.addView(dev)
        cc.addView(pass)
        cc.addView(bigBtn("ذخیره", true) {
            prefs.edit().putString("device", dev.text.toString().trim()).putString("pass", pass.text.toString().trim()).apply()
            toast("ذخیره شد")
        }, btnLp())
        col.addView(cc, cardLp())

        val clk = card()
        clk.addView(label("🕒 ساعت دستگاه", 17f, cText, bold = true))
        clk.addView(label("ساعت دستگاه را با ساعت گوشی یکی می‌کند. تایمر درب و برنامه‌ی لامپ به این ساعت نیاز دارند.", 13f, cMuted))
        clk.addView(bigBtn("همگام‌سازی با ساعت گوشی", true) {
            val c = java.util.Calendar.getInstance()
            sendSms("TIME %02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE)))
        }, btnLp())
        col.addView(clk, cardLp())

        if (showUsers) col.addView(usersCard(), cardLp())

        val lg = card()
        lg.addView(label("🧾 پیام‌های اخیر", 17f, cText, bold = true))
        val logText = prefs.getString("log", "") ?: ""
        lg.addView(label(if (logText.isBlank()) "هنوز پیامی نیست" else logText, 13f, cMuted))
        col.addView(lg, cardLp())

        col.addView(spacer(24))
        return scroll(col)
    }

    private fun usersCard(): View {
        val un = edit("شماره‌ی کاربر", InputType.TYPE_CLASS_PHONE)
        val up = edit("دسترسی‌ها مثل V2458A", InputType.TYPE_CLASS_TEXT)
        up.setText("V")
        val uc = card()
        uc.addView(label("👥 کاربران", 17f, cText, bold = true))
        uc.addView(label("V مشاهده، ۱ تا ۸ هر رله، S تنظیمات، A هشدار. مدیر جدید باید کد تأیید را با CONFIRM پس بفرستد.", 13f, cMuted))
        uc.addView(un)
        uc.addView(up)
        uc.addView(bigBtn("افزودن یا ویرایش", true) { sendAdmin("ADD ${un.text} ${up.text}") }, btnLp())
        uc.addView(bigBtn("حذف کاربر", false) { sendAdmin("DEL ${un.text}") }, btnLp())
        uc.addView(bigBtn("فهرست کاربران", false) { sendAdmin("LIST") }, btnLp())
        uc.addView(bigBtn("انتقال مدیریت به این شماره", false) { sendAdmin("ADMIN ${un.text}") }, btnLp())
        uc.addView(label(prefs.getString("users", "") ?: "", 13f, cMuted))
        return uc
    }

    // ---------- ارسال پیامک ----------
    private fun sendAdmin(cmd: String) {
        val pw = prefs.getString("pass", "") ?: ""
        if (pw.isEmpty()) {
            toast("اول رمز را در صفحه‌ی پیام‌ها ذخیره کنید")
            return
        }
        sendSms("$pw $cmd")
    }

    private fun sendSms(text: String) {
        val dev = prefs.getString("device", "") ?: ""
        if (Util.last10(dev).length < 7) {
            toast("اول شماره‌ی دستگاه را در صفحه‌ی پیام‌ها ذخیره کنید")
            return
        }
        try {
            val mgr: SmsManager = if (Build.VERSION.SDK_INT >= 31) getSystemService(SmsManager::class.java) else @Suppress("DEPRECATION") SmsManager.getDefault()
            mgr.sendTextMessage(dev, null, text, null, null)
            val log = (prefs.getString("log", "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
            val second = text.split(" ").getOrNull(1)
            val shown = if (second == "ADD" || second == "DEL" || second == "LIST" || second == "ADMIN" || second == "PASS") "••• " + text.substringAfter(" ") else text
            log.add(0, Util.now() + "  →  " + shown)
            prefs.edit().putString("log", log.take(15).joinToString("\n")).apply()
            toast("ارسال شد")
        } catch (e: Exception) {
            toast("ارسال نشد؛ اجازه‌ی پیامک را بدهید")
        }
    }

    // ---------- ابزارهای رابط ----------
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun fa(s: String): String {
        val sb = StringBuilder()
        for (ch in s) sb.append(if (ch in '0'..'9') ('۰' + (ch - '0')) else ch)
        return sb.toString()
    }

    private fun roundBg(color: Int, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(r).toFloat()
        return g
    }

    private fun cardBg(): GradientDrawable {
        val g = roundBg(cCard, 18)
        g.setStroke(dp(1), cLine)
        return g
    }

    private fun label(s: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(t.typeface, Typeface.BOLD)
        return t
    }

    private fun chip(s: String, on: Boolean): TextView {
        val t = label(s, 13f, if (on) cGreenDark else cMuted, bold = true)
        t.background = roundBg(if (on) cChipOn else cChipOff, 14)
        t.setPadding(dp(12), dp(4), dp(12), dp(4))
        val lp = LinearLayout.LayoutParams(-2, -2)
        lp.setMargins(0, dp(8), 0, dp(4))
        t.layoutParams = lp
        return t
    }

    private fun smallBtn(s: String, filled: Boolean, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = s
        b.isAllCaps = false
        b.textSize = 14f
        b.minHeight = 0
        b.minimumHeight = 0
        b.setPadding(dp(8), dp(8), dp(8), dp(8))
        if (filled) {
            b.setTextColor(Color.WHITE)
            b.background = roundBg(cGreen, 12)
        } else {
            b.setTextColor(cGreen)
            val g = roundBg(Color.TRANSPARENT, 12)
            g.setStroke(dp(1), cGreen)
            b.background = g
        }
        b.setOnClickListener { onClick() }
        val lp = LinearLayout.LayoutParams(-1, dp(42))
        lp.setMargins(0, dp(6), 0, 0)
        b.layoutParams = lp
        return b
    }

    private fun bigBtn(s: String, filled: Boolean, onClick: () -> Unit): Button {
        val b = smallBtn(s, filled, onClick)
        b.textSize = 16f
        b.layoutParams = null
        return b
    }

    private fun edit(hint: String, type: Int): EditText {
        val e = EditText(this)
        e.hint = hint
        e.inputType = type
        e.textSize = 16f
        e.setTextColor(cText)
        e.setHintTextColor(cMuted)
        e.textDirection = View.TEXT_DIRECTION_LTR
        e.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        e.setPadding(dp(14), 0, dp(14), 0)
        val g = roundBg(Color.parseColor("#FBF8F1"), 12)
        g.setStroke(dp(1), cLine)
        e.background = g
        val lp = LinearLayout.LayoutParams(-1, dp(48))
        lp.setMargins(0, dp(10), 0, 0)
        e.layoutParams = lp
        return e
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(16), dp(16), dp(16), dp(14))
        c.background = cardBg()
        return c
    }

    private fun cardLp(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(dp(16), dp(12), dp(16), 0)
        return lp
    }

    private fun btnLp(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(-1, dp(48))
        lp.setMargins(0, dp(10), 0, 0)
        return lp
    }

    private fun tileLp(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(0, -2, 1f)
        lp.setMargins(dp(4), dp(4), dp(4), dp(4))
        return lp
    }

    private fun sectionTitle(s: String): TextView {
        val t = label(s, 18f, cText, bold = true)
        t.setPadding(dp(20), dp(20), dp(20), dp(6))
        return t
    }

    private fun spacer(h: Int): View {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(-1, dp(h))
        return v
    }

    private fun pageColumn(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        return c
    }

    private fun pageHeader(emoji: String, title: String, sub: String): View {
        val h = LinearLayout(this)
        h.orientation = LinearLayout.VERTICAL
        h.setPadding(dp(20), dp(26), dp(20), dp(22))
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(cGreen, cGreenDark))
        g.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, dp(28).toFloat(), dp(28).toFloat(), dp(28).toFloat(), dp(28).toFloat())
        h.background = g
        h.addView(label("$emoji $title", 26f, Color.WHITE, bold = true))
        h.addView(label(sub, 13f, Color.parseColor("#BFE3CF")))
        return h
    }

    private fun scroll(inner: View): View {
        val s = ScrollView(this)
        s.isFillViewport = true
        s.addView(inner, ViewGroup.LayoutParams(-1, -2))
        return s
    }
}
