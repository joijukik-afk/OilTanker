package com.oiltanker

// ============================================================================
//  داستان «نفتکش: عملیات نجات»
//  AmirAlavi85 — خلبان نخبه
//  فرمانده Nargil — فرمانده مرکز عملیات
// ============================================================================

enum class Speaker {
    NARRATOR, AMIR, NARGIL, CAPTAIN, KRAKEN, LEVIATHAN, SYSTEM
}

data class DialogueLine(
    val speaker: Speaker,
    val text: String,
    val duration: Float = 3.5f,
    val shake: Boolean = false,
    val red: Boolean = false
)

object Story {

    // ------------------------------------------------------------------
    //  نام شخصیت‌ها
    // ------------------------------------------------------------------
    fun name(s: Speaker): String = when (s) {
        Speaker.NARRATOR -> "نقال"
        Speaker.AMIR -> "AmirAlavi85"
        Speaker.NARGIL -> "فرمانده Nargil"
        Speaker.CAPTAIN -> "کاپیتان رضایی"
        Speaker.KRAKEN -> "Kraken"
        Speaker.LEVIATHAN -> "Leviathan"
        Speaker.SYSTEM -> "سیستم"
    }

    fun color(s: Speaker): Long = when (s) {
        Speaker.NARRATOR -> 0xFFB0BEC5
        Speaker.AMIR -> 0xFF4FC3F7
        Speaker.NARGIL -> 0xFFFFB74D
        Speaker.CAPTAIN -> 0xFF81C784
        Speaker.KRAKEN -> 0xFFEF5350
        Speaker.LEVIATHAN -> 0xFF7E57C2
        Speaker.SYSTEM -> 0xFF90A4AE
    }

    // ------------------------------------------------------------------
    //  پرده ۱: سینماتیک شروع
    // ------------------------------------------------------------------
    val intro: List<DialogueLine> = listOf(
        DialogueLine(Speaker.NARRATOR, "سال ۲۰۴۵ — دریای خزر", 2.5f),
        DialogueLine(Speaker.NARRATOR, "طوفان شدیدی در راه است...", 3f),
        DialogueLine(Speaker.CAPTAIN, "برج مراقبت، اینجا نفتکش! ما در طوفان گیر افتادیم!", 3.5f),
        DialogueLine(Speaker.CAPTAIN, "موتور آسیب دیده... کنترل از دستمون خارج شده!", 3.5f, shake = true),
        DialogueLine(Speaker.SYSTEM, "⚠ هشدار: فشار مخازن بحرانی", 2.5f, red = true),
        DialogueLine(Speaker.CAPTAIN, "SOS! SOS! هر کسی این پیام رو می‌شنوه، کمک کنه!", 3.5f, shake = true),
        DialogueLine(Speaker.NARRATOR, "پیام اضطراری به مرکز عملیات می‌رسد...", 3f),
        DialogueLine(Speaker.NARGIL, "AmirAlavi85، اونجا رو ببین!", 2.5f),
        DialogueLine(Speaker.AMIR, "دریافت شد فرمانده. نفتکش ما در خطره.", 3f),
        DialogueLine(Speaker.NARGIL, "مأموریت تو: نفوذ به آب‌های خطرناک و رسیدن به نفتکش.", 3.5f),
        DialogueLine(Speaker.NARGIL, "هر چیزی که سر راهته — صخره، مین، دزد دریایی — نابود کن.", 3.5f),
        DialogueLine(Speaker.AMIR, "روشن! می‌رم.", 2f),
        DialogueLine(Speaker.NARRATOR, "«نفتکش: عملیات نجات»", 3f)
    )

    // ------------------------------------------------------------------
    //  پرده ۲: فصل اول — آب‌های خطرناک (هر ۱۵۰ متر)
    // ------------------------------------------------------------------
    val radioChatter: List<List<DialogueLine>> = listOf(
        listOf(
            DialogueLine(Speaker.NARGIL, "AmirAlavi85، آب‌های اطراف پر از صخره‌ست. مراقب باش.", 3.5f),
            DialogueLine(Speaker.AMIR, "چشم فرمانده.", 2f)
        ),
        listOf(
            DialogueLine(Speaker.NARGIL, "مین‌های دریایی تو آب پخش شدن. بهشون نزدیک نشو.", 3.5f),
            DialogueLine(Speaker.AMIR, "دارم می‌بینمشون.", 2f)
        ),
        listOf(
            DialogueLine(Speaker.NARGIL, "شناسایی کردم: کوه یخ در مسیر توئه.", 3.5f),
            DialogueLine(Speaker.AMIR, "این تو دریای خزر؟ عجیبه...", 2.5f)
        ),
        listOf(
            DialogueLine(Speaker.NARGIL, "کشتی دزد دریایی نزدیک می‌شه! حواست باشه شلیک می‌کنه.", 3.5f, shake = true),
            DialogueLine(Speaker.AMIR, "باشه، جاخالی می‌دم.", 2f)
        ),
        listOf(
            DialogueLine(Speaker.NARGIL, "نهنگ غول‌پیکر تو منطقه‌ست. ازش فاصله بگیر.", 3.5f),
            DialogueLine(Speaker.AMIR, "این یکی خیلی بزرگه...", 2.5f)
        ),
        listOf(
            DialogueLine(Speaker.NARGIL, "بشکه‌های نفت شناور تو آب رو جمع کن. حیاتی هستن.", 3.5f),
            DialogueLine(Speaker.AMIR, "دارم جمع می‌کنم.", 2f)
        ),
        listOf(
            DialogueLine(Speaker.NARGIL, "خبر خوب: پاورآپ‌هایی تو آب پخش شدن. استفاده کن.", 3.5f),
            DialogueLine(Speaker.AMIR, "دقیقاً چیزی که لازم داشتم.", 2f)
        ),
        listOf(
            DialogueLine(Speaker.NARGIL, "دارم به موقعیت نفتکش نزدیک می‌شم...", 3.5f),
            DialogueLine(Speaker.AMIR, "منم همین‌طور فرمانده.", 2f)
        )
    )

    // ------------------------------------------------------------------
    //  پرده ۳: نبرد Leviathan
    // ------------------------------------------------------------------
    val leviathanIntro: List<DialogueLine> = listOf(
        DialogueLine(Speaker.SYSTEM, "⚠ هشدار: موجود ناشناخته در حال نزدیک شدن", 3f, red = true),
        DialogueLine(Speaker.AMIR, "فرمانده، یه چیزی داره از زیر آب میاد...", 3f),
        DialogueLine(Speaker.NARGIL, "AmirAlavi85، اون Leviathan هست! یه نهنگ مکانیکی!", 3.5f, shake = true),
        DialogueLine(Speaker.LEVIATHAN, "غرش... غرش...", 2.5f, shake = true),
        DialogueLine(Speaker.NARGIL, "اسلحه معمولی روش اثر نداره! باید از مین‌ها استفاده کنی!", 3.5f),
        DialogueLine(Speaker.AMIR, "می‌فهمم. پس بریم سراغش.", 2.5f)
    )

    val leviathanDefeat: List<DialogueLine> = listOf(
        DialogueLine(Speaker.LEVIATHAN, "غرش ضعیف...", 2f),
        DialogueLine(Speaker.AMIR, "تمومه! Leviathan نابود شد.", 2.5f),
        DialogueLine(Speaker.NARGIL, "آفرین AmirAlavi85! اما هنوز تموم نشده...", 3f)
    )

    // ------------------------------------------------------------------
    //  پرده ۴: Kraken Strikes
    // ------------------------------------------------------------------
    val krakenAttack: List<DialogueLine> = listOf(
        DialogueLine(Speaker.SYSTEM, "⚠ هشدار: تهدید جدید شناسایی شد", 2.5f, red = true),
        DialogueLine(Speaker.AMIR, "فرمانده! یه چیزی نفتکش رو گرفته!", 3f, shake = true),
        DialogueLine(Speaker.NARGIL, "اونا Kraken هستن! اختاپوس مکانیکی غول‌پیکر!", 3.5f, shake = true),
        DialogueLine(Speaker.KRAKEN, "هیسسس...", 2f, shake = true),
        DialogueLine(Speaker.NARGIL, "AmirAlavi85، باید سریع یه F-35 سوار بشی!", 3.5f),
        DialogueLine(Speaker.NARGIL, "از این به بعد تو حالت جنگنده هستی. موشک داری.", 3.5f),
        DialogueLine(Speaker.AMIR, "روشن! می‌رم بالا.", 2.5f),
        DialogueLine(Speaker.SYSTEM, "🚀 حالت جنگنده فعال شد", 2f)
    )

    // ------------------------------------------------------------------
    //  پرده ۵: مأموریت نجات
    // ------------------------------------------------------------------
    val rescueMission: List<DialogueLine> = listOf(
        DialogueLine(Speaker.NARGIL, "بیست هدف هوایی رو نابود کن تا مسیر باز بشه.", 3.5f),
        DialogueLine(Speaker.AMIR, "دارم می‌رم سراغشون.", 2f),
        DialogueLine(Speaker.NARGIL, "مواظب موشک‌های دشمن باش!", 2.5f)
    )

    val rescueSuccess: List<DialogueLine> = listOf(
        DialogueLine(Speaker.NARGIL, "AmirAlavi85، مسیر آزاد شد! نفتکش رو بردار و بیار!", 3.5f),
        DialogueLine(Speaker.AMIR, "دریافت شد. دارم میام.", 2.5f),
        DialogueLine(Speaker.CAPTAIN, "ممنون! ما رو نجات دادی!", 3f),
        DialogueLine(Speaker.AMIR, "وظیفه‌م بود کاپیتان.", 2.5f)
    )

    // ------------------------------------------------------------------
    //  پرده ۶: پایان باز
    // ------------------------------------------------------------------
    val outro: List<DialogueLine> = listOf(
        DialogueLine(Speaker.NARGIL, "کارت درسته AmirAlavi85. نفتکش نجات پیدا کرد.", 3.5f),
        DialogueLine(Speaker.AMIR, "اما فرمانده... Kraken فرار کرد.", 3f),
        DialogueLine(Speaker.NARGIL, "می‌دونم. اون برمی‌گرده.", 3f),
        DialogueLine(Speaker.NARGIL, "و این بار با یه ارتش کامل میاد.", 3.5f),
        DialogueLine(Speaker.NARRATOR, "ادامه دارد...", 2.5f),
        DialogueLine(Speaker.NARRATOR, "«نفتکش ۳» — به‌زودی", 3f)
    )

    // ------------------------------------------------------------------
    //  پیام‌های پایان بازی
    // ------------------------------------------------------------------
    val gameOverLines: List<String> = listOf(
        "نفتکش از دست رفت...",
        "ما شکست خوردیم...",
        "باید دوباره تلاش کنیم...",
        "AmirAlavi85، تسلیم نشو!"
    )

    val victoryLines: List<String> = listOf(
        "پیروزی!",
        "نفتکش نجات پیدا کرد!",
        "AmirAlavi85 قهرمان شد!",
        "کارت درسته سرباز!"
    )

    // ------------------------------------------------------------------
    //  پیام‌های مأموریت
    // ------------------------------------------------------------------
    fun missionText(chapter: Int): String = when (chapter) {
        1 -> "فصل ۱: عبور از آب‌های خطرناک"
        2 -> "فصل ۲: نابودی Leviathan"
        3 -> "فصل ۳: نجات نفتکش از Kraken"
        else -> "فصل نامعلوم"
    }

    fun objectiveText(chapter: Int): String = when (chapter) {
        1 -> "هدف: ۳۰۰۰ متر بقا"
        2 -> "هدف: نابود کردن Leviathan"
        3 -> "هدف: ۲۰ هدف هوایی"
        else -> "هدف: نامعلوم"
    }

    // ------------------------------------------------------------------
    //  راهنما (برای نمایش در دیالوگ)
    // ------------------------------------------------------------------
    val tutorialHints: List<DialogueLine> = listOf(
        DialogueLine(Speaker.NARGIL, "برای حرکت کشتی، انگشتت رو بالا و پایین بکش.", 3.5f),
        DialogueLine(Speaker.NARGIL, "برای شلیک، دکمه «شلیک» پایین-راست رو بزن.", 3.5f),
        DialogueLine(Speaker.NARGIL, "بشکه‌های طلایی رو جمع کن تا امتیاز بگیری.", 3.5f),
        DialogueLine(Speaker.NARGIL, "قلب‌های قرمز یه جان اضافه می‌کنن.", 3.5f)
    )
}
