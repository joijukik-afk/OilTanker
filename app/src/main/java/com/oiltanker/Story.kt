package com.oiltanker

// ============================================================================
//  داستان «نفتکش ۲: عملیات اعماق»
//  AmirAlavi85 — خلبان نخبه
//  فرمانده Nargil — فرمانده مرکز عملیات
//  کاپیتان — فرمانده نفتکش
//  Ghost — متخصص نفوذ (شخصیت جدید)
// ============================================================================

enum class Speaker {
    NARRATOR, AMIR, NARGIL, CAPTAIN, KRAKEN, LEVIATHAN, GHOST, ABYSS, SYSTEM
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
        Speaker.CAPTAIN -> "کاپیتان"
        Speaker.KRAKEN -> "Kraken"
        Speaker.LEVIATHAN -> "Leviathan"
        Speaker.GHOST -> "Ghost"
        Speaker.ABYSS -> "هسته ABYSS"
        Speaker.SYSTEM -> "سیستم"
    }

    fun color(s: Speaker): Long = when (s) {
        Speaker.NARRATOR -> 0xFFB0BEC5
        Speaker.AMIR -> 0xFF4FC3F7
        Speaker.NARGIL -> 0xFFFFB74D
        Speaker.CAPTAIN -> 0xFF81C784
        Speaker.KRAKEN -> 0xFFEF5350
        Speaker.LEVIATHAN -> 0xFF7E57C2
        Speaker.GHOST -> 0xFF26A69A
        Speaker.ABYSS -> 0xFFFF1744
        Speaker.SYSTEM -> 0xFF90A4AE
    }

    // ------------------------------------------------------------------
    //  پرده ۱: سینماتیک شروع (طولانی‌تر، ۲۲ خط)
    // ------------------------------------------------------------------
    val intro: List<DialogueLine> = listOf(
        DialogueLine(Speaker.NARRATOR, "سال ۲۰۴۵", 2f),
        DialogueLine(Speaker.NARRATOR, "دریاها دیگر آن‌طور که می‌شناختیم نیستند.", 3f),
        DialogueLine(Speaker.NARRATOR, "در اعماق خزر، چیزی بیدار شده.", 3f),
        DialogueLine(Speaker.NARRATOR, "چیزی که نباید بیدار می‌شد.", 3.5f),
        DialogueLine(Speaker.SYSTEM, "⚠ هشدار: طوفان درجه چهار در حال شکل‌گیری", 2.5f, red = true),
        DialogueLine(Speaker.CAPTAIN, "برج مراقبت، اینجا نفتکش! ما در طوفان گیر افتادیم!", 3.5f),
        DialogueLine(Speaker.CAPTAIN, "موتور آسیب دیده... مخازن نشت کرده!", 3.5f, shake = true),
        DialogueLine(Speaker.CAPTAIN, "کنترل کشتی از دستمون خارج شده!", 3f),
        DialogueLine(Speaker.SYSTEM, "⚠ هشدار: فشار مخازن بحرانی", 2.5f, red = true),
        DialogueLine(Speaker.CAPTAIN, "SOS! هر کسی این پیام رو می‌شنوه، کمک کنه!", 3.5f, shake = true),
        DialogueLine(Speaker.NARRATOR, "پیام به مرکز عملیات می‌رسد...", 2.5f),
        DialogueLine(Speaker.NARGIL, "AmirAlavi85، بیدار شو.", 2.5f),
        DialogueLine(Speaker.AMIR, "فرمانده؟ ساعت چنده؟", 2f),
        DialogueLine(Speaker.NARGIL, "ساعت مهم نیست. یه نفتکش تو طوفان گیر افتاده.", 3.5f),
        DialogueLine(Speaker.AMIR, "تو دریای خزر؟ این فصل طوفان نداره.", 3f),
        DialogueLine(Speaker.NARGIL, "این طوفان معمولی نیست.", 2.5f),
        DialogueLine(Speaker.NARGIL, "دما، فشار، امواج... همه چیز غیرعادی‌ست.", 3.5f),
        DialogueLine(Speaker.AMIR, "یعنی چی؟", 2f),
        DialogueLine(Speaker.NARGIL, "یعنی اینکه یه چیزی زیر آب بیدار شده.", 3f),
        DialogueLine(Speaker.SYSTEM, "🚀 مأموریت: عملیات اعماق فعال شد", 2.5f),
        DialogueLine(Speaker.NARGIL, "مأموریت تو: نفوذ به آب‌های خطرناک و رسیدن به نفتکش.", 4f),
        DialogueLine(Speaker.NARGIL, "هر چیزی که سر راهته — صخره، مین، دزد دریایی — نابود کن.", 4f),
        DialogueLine(Speaker.NARGIL, "و AmirAlavi85...", 2f),
        DialogueLine(Speaker.NARGIL, "اگه پیام عجیبی روی رادیو شنیدی، بهم بگو.", 3.5f),
        DialogueLine(Speaker.AMIR, "چه پیامی؟", 2f),
        DialogueLine(Speaker.NARGIL, "هنوز نمی‌دونم. ولی چیزی تو آب هست که...", 3.5f),
        DialogueLine(Speaker.NARGIL, "داره تماشامون می‌کنه.", 3f),
        DialogueLine(Speaker.AMIR, "روشن. می‌رم.", 2f),
        DialogueLine(Speaker.NARRATOR, "«نفتکش ۲: عملیات اعماق»", 3f)
    )

    // ------------------------------------------------------------------
    //  پرده ۲: فصل ۱ — آب‌های خطرناک (پیام‌های رادیویی ۱۵۰ متری)
    // ------------------------------------------------------------------
    val radioChatter: List<List<DialogueLine>> = listOf(
        // ۱
        listOf(
            DialogueLine(Speaker.NARGIL, "AmirAlavi85، آب‌های اطراف پر از صخره‌ست. مراقب باش.", 3.5f),
            DialogueLine(Speaker.AMIR, "چشم فرمانده.", 2f)
        ),
        // ۲
        listOf(
            DialogueLine(Speaker.NARGIL, "مین‌های دریایی تو آب پخش شدن. بهشون نزدیک نشو.", 3.5f),
            DialogueLine(Speaker.AMIR, "دارم می‌بینمشون. زیادن.", 2.5f)
        ),
        // ۳
        listOf(
            DialogueLine(Speaker.NARGIL, "شناسایی کردم: کوه یخ در مسیر توئه.", 3.5f),
            DialogueLine(Speaker.AMIR, "این تو دریای خزر؟ عجیبه...", 2.5f),
            DialogueLine(Speaker.NARGIL, "دقیقاً. از همون چیزایی که نباید باشن.", 3f)
        ),
        // ۴
        listOf(
            DialogueLine(Speaker.NARGIL, "کشتی دزد دریایی نزدیک می‌شه! شلیک می‌کنه.", 3.5f, shake = true),
            DialogueLine(Speaker.AMIR, "اینم عجیبه. دزد دریایی تو خزر؟", 2.5f)
        ),
        // ۵
        listOf(
            DialogueLine(Speaker.NARGIL, "نهنگ غول‌پیکر تو منطقه‌ست. ازش فاصله بگیر.", 3.5f),
            DialogueLine(Speaker.AMIR, "نهنگ؟ چقدر بزرگه؟", 2f),
            DialogueLine(Speaker.NARGIL, "بزرگ‌تر از اونی که بشه باور کرد.", 3f)
        ),
        // ۶
        listOf(
            DialogueLine(Speaker.NARGIL, "بشکه‌های نفت شناور تو آب رو جمع کن. حیاتی هستن.", 3.5f),
            DialogueLine(Speaker.AMIR, "دارم جمع می‌کنم.", 2f)
        ),
        // ۷
        listOf(
            DialogueLine(Speaker.NARGIL, "پاورآپ‌هایی تو آب پخش شدن. استفاده کن.", 3.5f),
            DialogueLine(Speaker.AMIR, "دقیقاً چیزی که لازم داشتم.", 2f)
        ),
        // ۸
        listOf(
            DialogueLine(Speaker.NARGIL, "دارم به موقعیت نفتکش نزدیک می‌شم...", 3.5f),
            DialogueLine(Speaker.AMIR, "منم همین‌طور فرمانده.", 2f),
            DialogueLine(Speaker.NARGIL, "یه چیز عجیب... سیگنال‌های نفتکش قطع شدن.", 3.5f)
        ),
        // ۹
        listOf(
            DialogueLine(Speaker.NARGIL, "دارم امواج رادیویی عجیبی می‌گیرم. تو آب.", 3.5f),
            DialogueLine(Speaker.AMIR, "چه نوع امواجی؟", 2f),
            DialogueLine(Speaker.NARGIL, "فرکانس‌هایی که نباید از یه نفتکش بیاد.", 3.5f)
        ),
        // ۱۰
        listOf(
            DialogueLine(Speaker.SYSTEM, "⚠ سیگنال ناشناخته شناسایی شد", 2.5f, red = true),
            DialogueLine(Speaker.AMIR, "فرمانده، این چیه؟", 2.5f),
            DialogueLine(Speaker.NARGIL, "نمی‌دونم. ولی بهم نزدیک شو...", 3.5f)
        ),
        // ۱۱
        listOf(
            DialogueLine(Speaker.NARGIL, "AmirAlavi85، ۵۰۰ متر دیگه تا نفتکش.", 3f),
            DialogueLine(Speaker.AMIR, "دارم میام.", 2f)
        ),
        // ۱۲
        listOf(
            DialogueLine(Speaker.NARGIL, "چیزی داره از زیر آب میاد...", 3.5f),
            DialogueLine(Speaker.AMIR, "این دیگه چیه؟", 3f, shake = true),
            DialogueLine(Speaker.NARGIL, "خدا به خیر کنه...", 3f)
        )
    )

    // ------------------------------------------------------------------
    //  پرده ۳: نبرد با Leviathan
    // ------------------------------------------------------------------
    val leviathanIntro: List<DialogueLine> = listOf(
        DialogueLine(Speaker.SYSTEM, "⚠ هشدار: موجود ناشناخته در حال نزدیک شدن", 3f, red = true),
        DialogueLine(Speaker.AMIR, "فرمانده، یه چیزی داره از زیر آب میاد...", 3f),
        DialogueLine(Speaker.NARGIL, "AmirAlavi85، اون Leviathan هست! پروژه ABYSS!", 3.5f, shake = true),
        DialogueLine(Speaker.LEVIATHAN, "غرش... غرش...", 2.5f, shake = true),
        DialogueLine(Speaker.AMIR, "پروژه چی؟", 2.5f),
        DialogueLine(Speaker.NARGIL, "الان وقت توضیح نیست! اسلحه معمولی روش اثر نداره!", 3.5f),
        DialogueLine(Speaker.NARGIL, "باید از مین‌ها استفاده کنی! اونا رو بساز و بکوبش!", 3.5f),
        DialogueLine(Speaker.AMIR, "می‌فهمم. پس بریم سراغش.", 2.5f),
        DialogueLine(Speaker.LEVIATHAN, "واحد هفت... بالاخره برگشتی.", 3.5f, red = true),
        DialogueLine(Speaker.AMIR, "چی گفت؟", 2.5f, shake = true),
        DialogueLine(Speaker.NARGIL, "هیچی. فقط بجنگ.", 2.5f)
    )

    val leviathanDefeat: List<DialogueLine> = listOf(
        DialogueLine(Speaker.LEVIATHAN, "غرش ضعیف...", 2f),
        DialogueLine(Speaker.AMIR, "تمومه! Leviathan نابود شد.", 2.5f),
        DialogueLine(Speaker.NARGIL, "آفرین AmirAlavi85! اما هنوز تموم نشده...", 3f),
        DialogueLine(Speaker.SYSTEM, "⚠ تهدید جدید در حال نزدیک شدن", 2.5f, red = true),
        DialogueLine(Speaker.AMIR, "چی داری میگی؟", 2.5f),
        DialogueLine(Speaker.NARGIL, "دستور عقب‌نشینی می‌دم! برگرد!", 3.5f, shake = true)
    )

    // ------------------------------------------------------------------
    //  پرده ۴: Kraken Strikes
    // ------------------------------------------------------------------
    val krakenAttack: List<DialogueLine> = listOf(
        DialogueLine(Speaker.SYSTEM, "⚠ هشدار: تهدید کلاس ۵ شناسایی شد", 2.5f, red = true),
        DialogueLine(Speaker.AMIR, "فرمانده! یه چیزی نفتکش رو گرفته!", 3f, shake = true),
        DialogueLine(Speaker.NARGIL, "اونا Kraken هستن! اختاپوس مکانیکی غول‌پیکر!", 3.5f, shake = true),
        DialogueLine(Speaker.KRAKEN, "هیسسس...", 2f, shake = true),
        DialogueLine(Speaker.NARGIL, "AmirAlavi85، باید سریع یه F-35 سوار بشی!", 3.5f),
        DialogueLine(Speaker.NARGIL, "از این به بعد تو حالت جنگنده هستی. موشک داری.", 3.5f),
        DialogueLine(Speaker.AMIR, "روشن! می‌رم بالا.", 2.5f),
        DialogueLine(Speaker.SYSTEM, "🚀 حالت جنگنده فعال شد", 2f),
        DialogueLine(Speaker.GHOST, "این کاناله؟ صدا میاد؟", 2.5f),
        DialogueLine(Speaker.AMIR, "کی هستی تو؟", 2f),
        DialogueLine(Speaker.GHOST, "اسمم Ghost. متخصص نفوذ.", 3f),
        DialogueLine(Speaker.GHOST, "فرمانده Nargil منو فرستاد کمکت.", 3f),
        DialogueLine(Speaker.NARGIL, "Ghost تنها کسیه که می‌تونه بهت کمک کنه.", 3.5f),
        DialogueLine(Speaker.NARGIL, "بهش اعتماد کن. فعلاً.", 2.5f)
    )

    // ------------------------------------------------------------------
    //  پرده ۵: مأموریت نجات
    // ------------------------------------------------------------------
    val rescueMission: List<DialogueLine> = listOf(
        DialogueLine(Speaker.NARGIL, "بیست هدف هوایی رو نابود کن تا مسیر باز بشه.", 3.5f),
        DialogueLine(Speaker.AMIR, "دارم می‌رم سراغشون.", 2f),
        DialogueLine(Speaker.GHOST, "مواظب موشک‌های دشمن باش! از پایین حمله می‌کنن!", 3.5f),
        DialogueLine(Speaker.NARGIL, "Ghost، تو اطلاعات رو بفرست.", 2.5f),
        DialogueLine(Speaker.GHOST, "دارم می‌فرستم. سرعتشون زیاده.", 3f)
    )

    val rescueSuccess: List<DialogueLine> = listOf(
        DialogueLine(Speaker.GHOST, "مسیر آزاد شد! نفتکش رو بردار و بیار!", 3.5f),
        DialogueLine(Speaker.NARGIL, "AmirAlavi85، مسیر آزاد شد! نفتکش رو بردار و بیار!", 3.5f),
        DialogueLine(Speaker.AMIR, "دریافت شد. دارم میام.", 2.5f),
        DialogueLine(Speaker.CAPTAIN, "ممنون! ما رو نجات دادی!", 3f),
        DialogueLine(Speaker.AMIR, "وظیفه‌م بود کاپیتان.", 2.5f),
        DialogueLine(Speaker.GHOST, "AmirAlavi85... یه چیزی باید بگم.", 3.5f),
        DialogueLine(Speaker.AMIR, "بگو.", 1.5f),
        DialogueLine(Speaker.GHOST, "اون Leviathan... تو رو می‌شناخت.", 3.5f),
        DialogueLine(Speaker.AMIR, "چی؟", 2f, shake = true),
        DialogueLine(Speaker.GHOST, "گفت «واحد هفت». این یعنی چی؟", 3.5f),
        DialogueLine(Speaker.NARGIL, "Ghost! این اطلاعات طبقه‌بندی شده!", 3.5f, red = true),
        DialogueLine(Speaker.GHOST, "باید بدونیم فرمانده!", 3f),
        DialogueLine(Speaker.NARGIL, "نه! الان نه!", 3f, shake = true)
    )

    // ------------------------------------------------------------------
    //  پرده ۶: پایان باز
    // ------------------------------------------------------------------
    val outro: List<DialogueLine> = listOf(
        DialogueLine(Speaker.NARGIL, "کارت درسته AmirAlavi85. نفتکش نجات پیدا کرد.", 3.5f),
        DialogueLine(Speaker.AMIR, "اما فرمانده... Kraken فرار کرد.", 3f),
        DialogueLine(Speaker.NARGIL, "می‌دونم. اون برمی‌گرده.", 3f),
        DialogueLine(Speaker.NARGIL, "و این بار با یه ارتش کامل میاد.", 3.5f),
        DialogueLine(Speaker.GHOST, "و «واحد هفت» چی؟", 2.5f),
        DialogueLine(Speaker.NARGIL, "...", 2f),
        DialogueLine(Speaker.AMIR, "فرمانده؟", 2f),
        DialogueLine(Speaker.NARGIL, "یه روزی می‌فهمی. ولی الان نه.", 3.5f),
        DialogueLine(Speaker.NARRATOR, "ادامه دارد...", 2.5f),
        DialogueLine(Speaker.NARRATOR, "«نفتکش ۳: واحد هفت» — به‌زودی", 3f)
    )

    // ------------------------------------------------------------------
    //  پیام‌های پایان بازی (کامل‌تر)
    // ------------------------------------------------------------------
    val gameOverLines: List<String> = listOf(
        "نفتکش از دست رفت...",
        "ما شکست خوردیم...",
        "باید دوباره تلاش کنیم...",
        "AmirAlavi85، تسلیم نشو!",
        "مأموریت ناتمام ماند...",
        "دشمن قوی‌تر از اونیه که فکر می‌کردیم."
    )

    val victoryLines: List<String> = listOf(
        "پیروزی!",
        "نفتکش نجات پیدا کرد!",
        "AmirAlavi85 قهرمان شد!",
        "کارت درسته سرباز!",
        "عملیات اعماق با موفقیت انجام شد.",
        "اما رازها هنوز باقی‌اند..."
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

    // ------------------------------------------------------------------
    //  پیام‌های مخصوص رویدادها
    // ------------------------------------------------------------------

    // پیام هشدار حمله باس (قبل از هر حمله ویژه)
    fun bossWarning(bossName: String): String = when (bossName) {
        "LEVIATHAN" -> "⚠ حمله Leviathan در راه است!"
        "KRAKEN" -> "⚠ حمله Kraken در راه است!"
        else -> "⚠ حمله ویژه!"
    }

    // پیام شکست باس
    fun bossDefeatMessage(bossName: String): String = when (bossName) {
        "LEVIATHAN" -> "Leviathan نابود شد!"
        "KRAKEN" -> "Kraken مجبور به فرار شد!"
        else -> "دشمن نابود شد!"
    }

    // پیام‌های سیستم
    const val SYSTEM_LOW_HEALTH = "⚠ جان شما در خطر است!"
    const val SYSTEM_SHIELD_ACTIVE = "🛡 سپر فعال شد"
    const val SYSTEM_MAGNET_ACTIVE = "🧲 آهن‌ربا فعال شد"
    const val SYSTEM_TURBO_ACTIVE = "⚡ توربو فعال شد"
    const val SYSTEM_NEW_RECORD = "🏆 رکورد جدید!"
    const val SYSTEM_DOCUMENT_FOUND = "📁 سند محرمانه پیدا شد"
    const val SYSTEM_BOSS_INCOMING = "☠ باس در راه است"
}
