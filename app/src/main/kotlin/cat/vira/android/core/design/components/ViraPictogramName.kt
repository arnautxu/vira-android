package cat.vira.android.core.design.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import cat.vira.android.R

/// Noms dels pictogrames (`ViraPictogram.Name` de l'iPhone), i com es passa d'un nom a un fitxer.
///
/// Els noms són els de l'iPhone (`"picto-flag"`), no els del recurs (`picto_flag`), perquè són els
/// que es desen a les files (`symbolName` d'una tasca o d'un hàbit) i se sincronitzen: una tasca
/// creada a l'iPhone ha de portar la mateixa icona aquí. `drawable(name)` fa la traducció.
///
/// Les taules de sota es van generar a partir de `ViraPictogram.swift` i dels fitxers de
/// `res/drawable*`; un pictograma nou s'afegeix a la constant **i** a la taula.
object ViraPictogramName {
    const val calendar = "picto-calendar"
    const val sun = "picto-sun"
    const val moon = "picto-moon"
    const val medication = "picto-medication"
    const val habits = "picto-habits"
    const val organization = "picto-organization"
    const val focus = "picto-focus"
    const val focusModes = "picto-focus-modes"
    const val pomodoro = "picto-pomodoro"
    const val timer = "picto-timer"
    const val journal = "picto-journal"
    const val cards = "picto-cards"
    const val shop = "picto-shop"
    /// **La moneda de Vira.** Mateix fitxer que `shop`, amb nom propi perquè representa la divisa:
    /// sempre que es parli de ViriCoins es fa servir aquesta, mai `coin`/`coins` (Arnau, 2026-08-07).
    const val viriCoin = "picto-shop"
    const val support = "picto-support"
    const val audio = "picto-audio"
    const val mindfulnessAnxiety = "viri-mindfulness-anxiety"
    const val mindfulnessFocus = "viri-mindfulness-focus"
    const val mindfulnessDisconnect = "viri-mindfulness-disconnect"
    const val viriPhone = "viri-phone"
    const val viriMeditating = "viri-meditating"
    const val viriMedal = "viri-medal"
    const val viriFrozen = "viri-frozen"
    const val viriAsleep = "viri-asleep"
    const val viriStreakLost = "viri-streak-lost"
    const val viriFocused = "viri-focused"
    const val viriStudying = "viri-studying"
    const val viriChill = "viri-chill"
    const val focusBackdrop = "focus-backdrop"
    const val mindfulnessBackdrop = "mindfulness-backdrop"
    const val viriHero = "viri-hero"
    const val viriEntryWaving = "viri-entry-waving"
    const val viriEntryCalendar = "viri-entry-calendar"
    const val viriEntryThinking = "viri-entry-thinking"
    const val viriEntryAngel = "viri-entry-angel"
    const val viriEntryWriting = "viri-entry-writing"
    const val viriEntryTrophy = "viri-entry-trophy"
    const val viriEntryBodyguard = "viri-entry-bodyguard"
    const val viriEntryCelebrate = "viri-entry-celebrate"
    const val viriSceneFriends = "viri-scene-friends"
    const val viriScenePrivacy = "viri-scene-privacy"
    const val viriSceneCalendar = "viri-scene-calendar"
    const val viriScenePlanEmpty = "viri-scene-plan-empty"
    const val viriSceneBike = "viri-scene-bike"
    const val viriSceneLaunch = "viri-scene-launch"
    /// Ratxa a zero: deu escenes càlides que roten per dia.
    val viriStreakStartScenes: List<String> = (1..10).map { "viri-streak-start-%02d".format(it) }
    const val viriMeditatingCutout = "viri-meditating-cutout"
    const val streakFlame = "picto-flame"
    const val viriStreak = "viri-streak"
    const val streakBackdrop = "streak-backdrop"
    const val streakLostBackdrop = "streak-backdrop-lost"
    /// Encara no hi és: qui el demana fa servir `resolved(…)` amb el retall de sempre.
    const val streakFrozenBackdrop = "streak-backdrop-frozen"
    /// Encara no hi és: es demana amb `resolved(viriTrophy, fallback = viriMedal)`.
    const val viriTrophy = "viri-trophy"
    const val badgeWeekStreak = "badge-week-streak"
    const val badgeMonthStreak = "badge-month-streak"
    const val viriProfile = "picto-profile"
    const val xp = "picto-xp"

    // Làmina de gamificació (2026-07-31)
    const val coin = "picto-coin"
    const val coins = "picto-coins"
    const val trophyClay = "picto-trophy-clay"
    const val medalClay = "picto-medal-clay"
    const val crown = "picto-crown"
    const val gem = "picto-gem"
    const val gift = "picto-gift"
    const val chest = "picto-chest"
    const val badgeStar = "picto-badge-star"
    const val badgeXP = "picto-badge-xp"
    const val rocket = "picto-rocket"
    const val bolt = "picto-bolt"
    const val flameClay = "picto-flame-clay"
    const val freeze = "picto-freeze"
    const val heart = "picto-heart"
    const val shield = "picto-shield"
    const val key = "picto-key"
    const val target = "picto-target"
    const val flag = "picto-flag"
    const val confetti = "picto-confetti"
    const val potion = "picto-potion"
    const val ticket = "picto-ticket"
    const val hourglass = "picto-hourglass"
    const val dice = "picto-dice"
    const val levelUp = "picto-level-up"

    // Nivell i accessoris (2026-08-06). Art encarregat, no arribat encara: cauen a `levelUp`
    // fins que hi siguin, com `medicationForm`.
    val levelFrameBronze: String get() = resolved("picto-level-frame-bronze", fallback = levelUp)
    val levelFrameSilver: String get() = resolved("picto-level-frame-silver", fallback = levelUp)
    val levelFrameGold: String get() = resolved("picto-level-frame-gold", fallback = levelUp)
    val levelFrameDiamond: String get() = resolved("picto-level-frame-diamond", fallback = levelUp)

    // Icones d'hàbits (2026-08-06)
    const val habitWater = "picto-habit-water"
    const val habitBike = "picto-habit-bike"
    const val habitNutrition = "picto-habit-nutrition"
    const val habitSleep = "picto-habit-sleep"
    const val habitMeditate = "picto-habit-meditate"
    const val habitStrength = "picto-habit-strength"
    const val habitTasks = "picto-habit-tasks"
    const val habitWake = "picto-habit-wake"
    const val habitStudy = "picto-habit-study"
    const val habitWalk = "picto-habit-walk"
    const val habitMusic = "picto-habit-music"
    const val habitMorning = "picto-habit-morning"
    const val habitRead = "picto-habit-read"
    const val habitWrite = "picto-habit-write"
    const val habitRun = "picto-habit-run"
    const val habitYoga = "picto-habit-yoga"

    // Ampliació del vocabulari d'hàbits (2026-08-12)
    const val habitHygiene = "picto-habit-hygiene"
    const val habitPlants = "picto-habit-plants"
    /// **No és a `habitIcons`**: és el mateix objecte que `habitYoga`.
    const val habitStretch = "picto-habit-stretch"
    const val habitFriends = "picto-habit-friends"
    const val habitCook = "picto-habit-cook"
    const val habitDisconnect = "picto-habit-disconnect"
    const val habitFruit = "picto-habit-fruit"
    const val habitGratitude = "picto-habit-gratitude"
    const val habitTidy = "picto-habit-tidy"
    const val habitOuting = "picto-habit-outing"
    const val habitPlan = "picto-habit-plan"
    const val habitSunscreen = "picto-habit-sunscreen"
    const val habitBreathe = "picto-habit-breathe"
    const val habitPhone = "picto-habit-phone"

    // Icones de calendari (2026-08-07)
    const val planChecklist = "picto-plan-checklist"
    const val planCheck = "picto-plan-check"
    const val planReminder = "picto-plan-reminder"
    const val planAlarm = "picto-plan-alarm"
    const val planNote = "picto-plan-note"
    const val planPin = "picto-plan-pin"
    const val planWrite = "picto-plan-write"
    const val planStar = "picto-plan-star"
    const val planMeeting = "picto-plan-meeting"
    const val planGroup = "picto-plan-group"
    const val planVideocall = "picto-plan-videocall"
    const val planTalk = "picto-plan-talk"
    const val planPresentation = "picto-plan-presentation"
    const val planRepeat = "picto-plan-repeat"
    const val planImportant = "picto-plan-important"
    const val planDeadline = "picto-plan-deadline"

    // Vocabulari de calendari, làmina nova (2026-08-12)
    const val calToday = "picto-cal-today"
    const val calWeek = "picto-cal-week"
    const val calMonth = "picto-cal-month"
    const val calReminder = "picto-cal-reminder"
    const val calDeadline = "picto-cal-deadline"
    const val calTask = "picto-cal-task"
    const val calEvent = "picto-cal-event"
    const val calBreak = "picto-cal-break"
    const val calClass = "picto-cal-class"
    const val calExam = "picto-cal-exam"
    const val calWorkout = "picto-cal-workout"
    const val calMeeting = "picto-cal-meeting"
    const val calCall = "picto-cal-call"
    const val calVideocall = "picto-cal-videocall"
    const val calBirthday = "picto-cal-birthday"
    const val calParty = "picto-cal-party"
    const val calCoffee = "picto-cal-coffee"
    const val calMeal = "picto-cal-meal"
    const val calShopping = "picto-cal-shopping"
    const val calBill = "picto-cal-bill"
    const val calHome = "picto-cal-home"
    const val calCleaning = "picto-cal-cleaning"
    const val calDog = "picto-cal-dog"
    const val calDoctor = "picto-cal-doctor"
    const val calTrip = "picto-cal-trip"
    const val calFlight = "picto-cal-flight"
    const val calFreetime = "picto-cal-freetime"
    const val calSun = "picto-cal-sun"
    const val calRain = "picto-cal-rain"
    /// **No és a `planIcons`** i no hi pot entrar: la medicació arriba a l'agenda des de la pauta.
    const val calMedication = "picto-cal-medication"

    // Gamificació, làmina coral (2026-08-12)
    const val gameFlag = "picto-game-flag"
    const val gameRocket = "picto-game-rocket"
    const val gameCrown = "picto-game-crown"
    const val gameMedal = "picto-game-medal"
    const val gameTrophy = "picto-game-trophy"
    const val gameLevelUp = "picto-game-level-up"
    const val gameTarget = "picto-game-target"
    const val gameGift = "picto-game-gift"
    const val gameBadge = "picto-game-badge"
    const val gameStar = "picto-game-star"
    const val gameLaurel = "picto-game-laurel"
    const val gamePodium = "picto-game-podium"
    const val gamePuzzle = "picto-game-puzzle"
    const val gameChallenge = "picto-game-challenge"
    const val gameChest = "picto-game-chest"
    const val gameConfetti = "picto-game-confetti"
    const val gameDice = "picto-game-dice"
    const val gameFlame = "picto-game-flame"
    const val gameStreak = "picto-game-streak"
    const val gameCoins = "picto-game-coins"

    // Icones d'Ajustos (2026-08-12)
    const val setAccount = "picto-set-account"
    const val setNotifications = "picto-set-notifications"
    const val setGeneral = "picto-set-general"
    const val setPrivacy = "picto-set-privacy"
    const val setProgress = "picto-set-progress"
    const val setToggle = "picto-set-toggle"
    const val setAppearance = "picto-set-appearance"
    const val setLanguage = "picto-set-language"
    const val setSound = "picto-set-sound"
    const val setAccessibility = "picto-set-accessibility"

    /// El recurs d'un nom desat, o `null` si el fitxer no hi és (un asset encarregat que encara no
    /// ha arribat, o un nom que no és de Vira).
    @DrawableRes
    fun drawable(name: String): Int? = drawables[name]

    /// Si l'asset existeix. Un nom que no hi és no peta: `ViraPictogram` hi deixa el buit.
    fun exists(name: String): Boolean = name in drawables

    /// El pictograma demanat si hi és, i un substitut mentre no hi sigui.
    fun resolved(name: String, fallback: String): String = if (exists(name)) name else fallback

    /// Una icona per forma de medicament. **Encara no hi són**: totes cauen a `medication` fins
    /// que arribin, i llavors el codi les agafa soles.
    fun medicationForm(rawValue: String): String = resolved("picto-med-$rawValue", fallback = medication)

    /// L'etiqueta de TalkBack d'un pictograma triable (`pictogram.<nom>` del catàleg de textos), o
    /// `null` si no en té cap. És copy de producte, no el nom intern del fitxer.
    @StringRes
    fun accessibilityLabel(name: String): Int? = labels[name]

    /// Icones triables en crear una tasca o un esdeveniment. La primera és la que es queda si no
    /// es tria res. **Cap forma de medicació**: la medicació entra a l'agenda des de la pauta.
    /// La làmina de calendari del 2026-08-12 va en bloc al final: són dues famílies visuals.
    val planIcons: List<String> = listOf(
        flag, calendar, planDeadline, planAlarm, planReminder, planRepeat, planChecklist, planWrite,
        planNote, planPin, planStar, planImportant, planCheck, target, organization, planPresentation,
        planMeeting, planGroup, planVideocall, planTalk, habitStudy, habitRead, pomodoro,
        calToday, calWeek, calMonth, calReminder, calDeadline, calBreak, calTask, calEvent, calClass,
        calExam, calWorkout, calMeeting, calCall, calVideocall, calBirthday, calParty, calCoffee,
        calMeal, calShopping, calBill, calHome, calCleaning, calDog, calDoctor, calTrip, calFlight,
        calFreetime, calSun, calRain,
    )

    /// Icones triables en crear un hàbit, agrupades per moment del dia i tipus d'activitat.
    /// `habits` és la genèrica i per defecte.
    val habitIcons: List<String> = listOf(
        habits, habitMorning, habitWake, habitHygiene, habitWater, habitNutrition, habitFruit,
        habitCook, habitSleep, habitRun, habitWalk, habitOuting, habitBike, habitStrength, habitYoga,
        habitMeditate, habitBreathe, habitStudy, habitRead, habitWrite, habitGratitude, habitTasks,
        habitPlan, habitTidy, habitPlants, habitFriends, habitPhone, habitDisconnect, habitMusic,
        habitSunscreen,
    )

    /// Icones triables en crear un mode de concentració. `focusModes` és la per defecte.
    val focusModeIcons: List<String> = listOf(
        focusModes, pomodoro, timer, focus, organization, journal, viriFocused, viriStudying,
    )

    private val drawables: Map<String, Int> = mapOf(
    "badge-month-streak" to R.drawable.badge_month_streak,
    "badge-week-streak" to R.drawable.badge_week_streak,
    "focus-backdrop" to R.drawable.focus_backdrop,
    "mindfulness-backdrop" to R.drawable.mindfulness_backdrop,
    "picto-audio" to R.drawable.picto_audio,
    "picto-badge-star" to R.drawable.picto_badge_star,
    "picto-badge-xp" to R.drawable.picto_badge_xp,
    "picto-bolt" to R.drawable.picto_bolt,
    "picto-cal-bill" to R.drawable.picto_cal_bill,
    "picto-cal-birthday" to R.drawable.picto_cal_birthday,
    "picto-cal-break" to R.drawable.picto_cal_break,
    "picto-cal-call" to R.drawable.picto_cal_call,
    "picto-cal-class" to R.drawable.picto_cal_class,
    "picto-cal-cleaning" to R.drawable.picto_cal_cleaning,
    "picto-cal-coffee" to R.drawable.picto_cal_coffee,
    "picto-cal-deadline" to R.drawable.picto_cal_deadline,
    "picto-cal-doctor" to R.drawable.picto_cal_doctor,
    "picto-cal-dog" to R.drawable.picto_cal_dog,
    "picto-cal-event" to R.drawable.picto_cal_event,
    "picto-cal-exam" to R.drawable.picto_cal_exam,
    "picto-cal-flight" to R.drawable.picto_cal_flight,
    "picto-cal-freetime" to R.drawable.picto_cal_freetime,
    "picto-cal-home" to R.drawable.picto_cal_home,
    "picto-cal-meal" to R.drawable.picto_cal_meal,
    "picto-cal-medication" to R.drawable.picto_cal_medication,
    "picto-cal-meeting" to R.drawable.picto_cal_meeting,
    "picto-cal-month" to R.drawable.picto_cal_month,
    "picto-cal-party" to R.drawable.picto_cal_party,
    "picto-cal-rain" to R.drawable.picto_cal_rain,
    "picto-cal-reminder" to R.drawable.picto_cal_reminder,
    "picto-cal-shopping" to R.drawable.picto_cal_shopping,
    "picto-cal-sun" to R.drawable.picto_cal_sun,
    "picto-cal-task" to R.drawable.picto_cal_task,
    "picto-cal-today" to R.drawable.picto_cal_today,
    "picto-cal-trip" to R.drawable.picto_cal_trip,
    "picto-cal-videocall" to R.drawable.picto_cal_videocall,
    "picto-cal-week" to R.drawable.picto_cal_week,
    "picto-cal-workout" to R.drawable.picto_cal_workout,
    "picto-calendar" to R.drawable.picto_calendar,
    "picto-cards" to R.drawable.picto_cards,
    "picto-chest" to R.drawable.picto_chest,
    "picto-coin" to R.drawable.picto_coin,
    "picto-coins" to R.drawable.picto_coins,
    "picto-confetti" to R.drawable.picto_confetti,
    "picto-crown" to R.drawable.picto_crown,
    "picto-dice" to R.drawable.picto_dice,
    "picto-flag" to R.drawable.picto_flag,
    "picto-flame" to R.drawable.picto_flame,
    "picto-flame-clay" to R.drawable.picto_flame_clay,
    "picto-focus" to R.drawable.picto_focus,
    "picto-focus-modes" to R.drawable.picto_focus_modes,
    "picto-freeze" to R.drawable.picto_freeze,
    "picto-game-badge" to R.drawable.picto_game_badge,
    "picto-game-challenge" to R.drawable.picto_game_challenge,
    "picto-game-chest" to R.drawable.picto_game_chest,
    "picto-game-coins" to R.drawable.picto_game_coins,
    "picto-game-confetti" to R.drawable.picto_game_confetti,
    "picto-game-crown" to R.drawable.picto_game_crown,
    "picto-game-dice" to R.drawable.picto_game_dice,
    "picto-game-flag" to R.drawable.picto_game_flag,
    "picto-game-flame" to R.drawable.picto_game_flame,
    "picto-game-gift" to R.drawable.picto_game_gift,
    "picto-game-laurel" to R.drawable.picto_game_laurel,
    "picto-game-level-up" to R.drawable.picto_game_level_up,
    "picto-game-medal" to R.drawable.picto_game_medal,
    "picto-game-podium" to R.drawable.picto_game_podium,
    "picto-game-puzzle" to R.drawable.picto_game_puzzle,
    "picto-game-rocket" to R.drawable.picto_game_rocket,
    "picto-game-star" to R.drawable.picto_game_star,
    "picto-game-streak" to R.drawable.picto_game_streak,
    "picto-game-target" to R.drawable.picto_game_target,
    "picto-game-trophy" to R.drawable.picto_game_trophy,
    "picto-gem" to R.drawable.picto_gem,
    "picto-gift" to R.drawable.picto_gift,
    "picto-habit-bike" to R.drawable.picto_habit_bike,
    "picto-habit-breathe" to R.drawable.picto_habit_breathe,
    "picto-habit-cook" to R.drawable.picto_habit_cook,
    "picto-habit-disconnect" to R.drawable.picto_habit_disconnect,
    "picto-habit-friends" to R.drawable.picto_habit_friends,
    "picto-habit-fruit" to R.drawable.picto_habit_fruit,
    "picto-habit-gratitude" to R.drawable.picto_habit_gratitude,
    "picto-habit-hygiene" to R.drawable.picto_habit_hygiene,
    "picto-habit-meditate" to R.drawable.picto_habit_meditate,
    "picto-habit-morning" to R.drawable.picto_habit_morning,
    "picto-habit-music" to R.drawable.picto_habit_music,
    "picto-habit-nutrition" to R.drawable.picto_habit_nutrition,
    "picto-habit-outing" to R.drawable.picto_habit_outing,
    "picto-habit-phone" to R.drawable.picto_habit_phone,
    "picto-habit-plan" to R.drawable.picto_habit_plan,
    "picto-habit-plants" to R.drawable.picto_habit_plants,
    "picto-habit-read" to R.drawable.picto_habit_read,
    "picto-habit-run" to R.drawable.picto_habit_run,
    "picto-habit-sleep" to R.drawable.picto_habit_sleep,
    "picto-habit-strength" to R.drawable.picto_habit_strength,
    "picto-habit-stretch" to R.drawable.picto_habit_stretch,
    "picto-habit-study" to R.drawable.picto_habit_study,
    "picto-habit-sunscreen" to R.drawable.picto_habit_sunscreen,
    "picto-habit-tasks" to R.drawable.picto_habit_tasks,
    "picto-habit-tidy" to R.drawable.picto_habit_tidy,
    "picto-habit-wake" to R.drawable.picto_habit_wake,
    "picto-habit-walk" to R.drawable.picto_habit_walk,
    "picto-habit-water" to R.drawable.picto_habit_water,
    "picto-habit-write" to R.drawable.picto_habit_write,
    "picto-habit-yoga" to R.drawable.picto_habit_yoga,
    "picto-habits" to R.drawable.picto_habits,
    "picto-heart" to R.drawable.picto_heart,
    "picto-hourglass" to R.drawable.picto_hourglass,
    "picto-journal" to R.drawable.picto_journal,
    "picto-key" to R.drawable.picto_key,
    "picto-level-up" to R.drawable.picto_level_up,
    "picto-medal-clay" to R.drawable.picto_medal_clay,
    "picto-medication" to R.drawable.picto_medication,
    "picto-moon" to R.drawable.picto_moon,
    "picto-organization" to R.drawable.picto_organization,
    "picto-plan-alarm" to R.drawable.picto_plan_alarm,
    "picto-plan-check" to R.drawable.picto_plan_check,
    "picto-plan-checklist" to R.drawable.picto_plan_checklist,
    "picto-plan-deadline" to R.drawable.picto_plan_deadline,
    "picto-plan-group" to R.drawable.picto_plan_group,
    "picto-plan-important" to R.drawable.picto_plan_important,
    "picto-plan-meeting" to R.drawable.picto_plan_meeting,
    "picto-plan-note" to R.drawable.picto_plan_note,
    "picto-plan-pin" to R.drawable.picto_plan_pin,
    "picto-plan-presentation" to R.drawable.picto_plan_presentation,
    "picto-plan-reminder" to R.drawable.picto_plan_reminder,
    "picto-plan-repeat" to R.drawable.picto_plan_repeat,
    "picto-plan-star" to R.drawable.picto_plan_star,
    "picto-plan-talk" to R.drawable.picto_plan_talk,
    "picto-plan-videocall" to R.drawable.picto_plan_videocall,
    "picto-plan-write" to R.drawable.picto_plan_write,
    "picto-pomodoro" to R.drawable.picto_pomodoro,
    "picto-potion" to R.drawable.picto_potion,
    "picto-profile" to R.drawable.picto_profile,
    "picto-rocket" to R.drawable.picto_rocket,
    "picto-set-accessibility" to R.drawable.picto_set_accessibility,
    "picto-set-account" to R.drawable.picto_set_account,
    "picto-set-appearance" to R.drawable.picto_set_appearance,
    "picto-set-general" to R.drawable.picto_set_general,
    "picto-set-language" to R.drawable.picto_set_language,
    "picto-set-notifications" to R.drawable.picto_set_notifications,
    "picto-set-privacy" to R.drawable.picto_set_privacy,
    "picto-set-progress" to R.drawable.picto_set_progress,
    "picto-set-sound" to R.drawable.picto_set_sound,
    "picto-set-toggle" to R.drawable.picto_set_toggle,
    "picto-shield" to R.drawable.picto_shield,
    "picto-shop" to R.drawable.picto_shop,
    "picto-sun" to R.drawable.picto_sun,
    "picto-support" to R.drawable.picto_support,
    "picto-target" to R.drawable.picto_target,
    "picto-ticket" to R.drawable.picto_ticket,
    "picto-timer" to R.drawable.picto_timer,
    "picto-trophy-clay" to R.drawable.picto_trophy_clay,
    "picto-xp" to R.drawable.picto_xp,
    "streak-backdrop" to R.drawable.streak_backdrop,
    "streak-backdrop-lost" to R.drawable.streak_backdrop_lost,
    "viri-asleep" to R.drawable.viri_asleep,
    "viri-chill" to R.drawable.viri_chill,
    "viri-entry-angel" to R.drawable.viri_entry_angel,
    "viri-entry-bodyguard" to R.drawable.viri_entry_bodyguard,
    "viri-entry-calendar" to R.drawable.viri_entry_calendar,
    "viri-entry-celebrate" to R.drawable.viri_entry_celebrate,
    "viri-entry-thinking" to R.drawable.viri_entry_thinking,
    "viri-entry-trophy" to R.drawable.viri_entry_trophy,
    "viri-entry-waving" to R.drawable.viri_entry_waving,
    "viri-entry-writing" to R.drawable.viri_entry_writing,
    "viri-focused" to R.drawable.viri_focused,
    "viri-frozen" to R.drawable.viri_frozen,
    "viri-hero" to R.drawable.viri_hero,
    "viri-medal" to R.drawable.viri_medal,
    "viri-meditating" to R.drawable.viri_meditating,
    "viri-meditating-cutout" to R.drawable.viri_meditating_cutout,
    "viri-mindfulness-anxiety" to R.drawable.viri_mindfulness_anxiety,
    "viri-mindfulness-disconnect" to R.drawable.viri_mindfulness_disconnect,
    "viri-mindfulness-focus" to R.drawable.viri_mindfulness_focus,
    "viri-phone" to R.drawable.viri_phone,
    "viri-scene-bike" to R.drawable.viri_scene_bike,
    "viri-scene-calendar" to R.drawable.viri_scene_calendar,
    "viri-scene-friends" to R.drawable.viri_scene_friends,
    "viri-scene-launch" to R.drawable.viri_scene_launch,
    "viri-scene-plan-empty" to R.drawable.viri_scene_plan_empty,
    "viri-scene-privacy" to R.drawable.viri_scene_privacy,
    "viri-streak" to R.drawable.viri_streak,
    "viri-streak-lost" to R.drawable.viri_streak_lost,
    "viri-streak-start-01" to R.drawable.viri_streak_start_01,
    "viri-streak-start-02" to R.drawable.viri_streak_start_02,
    "viri-streak-start-03" to R.drawable.viri_streak_start_03,
    "viri-streak-start-04" to R.drawable.viri_streak_start_04,
    "viri-streak-start-05" to R.drawable.viri_streak_start_05,
    "viri-streak-start-06" to R.drawable.viri_streak_start_06,
    "viri-streak-start-07" to R.drawable.viri_streak_start_07,
    "viri-streak-start-08" to R.drawable.viri_streak_start_08,
    "viri-streak-start-09" to R.drawable.viri_streak_start_09,
    "viri-streak-start-10" to R.drawable.viri_streak_start_10,
    "viri-studying" to R.drawable.viri_studying,
    )

    private val labels: Map<String, Int> = mapOf(
    "picto-cal-bill" to R.string.pictogram_picto_cal_bill,
    "picto-cal-birthday" to R.string.pictogram_picto_cal_birthday,
    "picto-cal-break" to R.string.pictogram_picto_cal_break,
    "picto-cal-call" to R.string.pictogram_picto_cal_call,
    "picto-cal-class" to R.string.pictogram_picto_cal_class,
    "picto-cal-cleaning" to R.string.pictogram_picto_cal_cleaning,
    "picto-cal-coffee" to R.string.pictogram_picto_cal_coffee,
    "picto-cal-deadline" to R.string.pictogram_picto_cal_deadline,
    "picto-cal-doctor" to R.string.pictogram_picto_cal_doctor,
    "picto-cal-dog" to R.string.pictogram_picto_cal_dog,
    "picto-cal-event" to R.string.pictogram_picto_cal_event,
    "picto-cal-exam" to R.string.pictogram_picto_cal_exam,
    "picto-cal-flight" to R.string.pictogram_picto_cal_flight,
    "picto-cal-freetime" to R.string.pictogram_picto_cal_freetime,
    "picto-cal-home" to R.string.pictogram_picto_cal_home,
    "picto-cal-meal" to R.string.pictogram_picto_cal_meal,
    "picto-cal-meeting" to R.string.pictogram_picto_cal_meeting,
    "picto-cal-month" to R.string.pictogram_picto_cal_month,
    "picto-cal-party" to R.string.pictogram_picto_cal_party,
    "picto-cal-rain" to R.string.pictogram_picto_cal_rain,
    "picto-cal-reminder" to R.string.pictogram_picto_cal_reminder,
    "picto-cal-shopping" to R.string.pictogram_picto_cal_shopping,
    "picto-cal-sun" to R.string.pictogram_picto_cal_sun,
    "picto-cal-task" to R.string.pictogram_picto_cal_task,
    "picto-cal-today" to R.string.pictogram_picto_cal_today,
    "picto-cal-trip" to R.string.pictogram_picto_cal_trip,
    "picto-cal-videocall" to R.string.pictogram_picto_cal_videocall,
    "picto-cal-week" to R.string.pictogram_picto_cal_week,
    "picto-cal-workout" to R.string.pictogram_picto_cal_workout,
    "picto-calendar" to R.string.pictogram_picto_calendar,
    "picto-flag" to R.string.pictogram_picto_flag,
    "picto-focus" to R.string.pictogram_picto_focus,
    "picto-focus-modes" to R.string.pictogram_picto_focus_modes,
    "picto-habit-bike" to R.string.pictogram_picto_habit_bike,
    "picto-habit-breathe" to R.string.pictogram_picto_habit_breathe,
    "picto-habit-cook" to R.string.pictogram_picto_habit_cook,
    "picto-habit-disconnect" to R.string.pictogram_picto_habit_disconnect,
    "picto-habit-friends" to R.string.pictogram_picto_habit_friends,
    "picto-habit-fruit" to R.string.pictogram_picto_habit_fruit,
    "picto-habit-gratitude" to R.string.pictogram_picto_habit_gratitude,
    "picto-habit-hygiene" to R.string.pictogram_picto_habit_hygiene,
    "picto-habit-meditate" to R.string.pictogram_picto_habit_meditate,
    "picto-habit-morning" to R.string.pictogram_picto_habit_morning,
    "picto-habit-music" to R.string.pictogram_picto_habit_music,
    "picto-habit-nutrition" to R.string.pictogram_picto_habit_nutrition,
    "picto-habit-outing" to R.string.pictogram_picto_habit_outing,
    "picto-habit-phone" to R.string.pictogram_picto_habit_phone,
    "picto-habit-plan" to R.string.pictogram_picto_habit_plan,
    "picto-habit-plants" to R.string.pictogram_picto_habit_plants,
    "picto-habit-read" to R.string.pictogram_picto_habit_read,
    "picto-habit-run" to R.string.pictogram_picto_habit_run,
    "picto-habit-sleep" to R.string.pictogram_picto_habit_sleep,
    "picto-habit-strength" to R.string.pictogram_picto_habit_strength,
    "picto-habit-study" to R.string.pictogram_picto_habit_study,
    "picto-habit-sunscreen" to R.string.pictogram_picto_habit_sunscreen,
    "picto-habit-tasks" to R.string.pictogram_picto_habit_tasks,
    "picto-habit-tidy" to R.string.pictogram_picto_habit_tidy,
    "picto-habit-wake" to R.string.pictogram_picto_habit_wake,
    "picto-habit-walk" to R.string.pictogram_picto_habit_walk,
    "picto-habit-water" to R.string.pictogram_picto_habit_water,
    "picto-habit-write" to R.string.pictogram_picto_habit_write,
    "picto-habit-yoga" to R.string.pictogram_picto_habit_yoga,
    "picto-habits" to R.string.pictogram_picto_habits,
    "picto-journal" to R.string.pictogram_picto_journal,
    "picto-organization" to R.string.pictogram_picto_organization,
    "picto-plan-alarm" to R.string.pictogram_picto_plan_alarm,
    "picto-plan-check" to R.string.pictogram_picto_plan_check,
    "picto-plan-checklist" to R.string.pictogram_picto_plan_checklist,
    "picto-plan-deadline" to R.string.pictogram_picto_plan_deadline,
    "picto-plan-group" to R.string.pictogram_picto_plan_group,
    "picto-plan-important" to R.string.pictogram_picto_plan_important,
    "picto-plan-meeting" to R.string.pictogram_picto_plan_meeting,
    "picto-plan-note" to R.string.pictogram_picto_plan_note,
    "picto-plan-pin" to R.string.pictogram_picto_plan_pin,
    "picto-plan-presentation" to R.string.pictogram_picto_plan_presentation,
    "picto-plan-reminder" to R.string.pictogram_picto_plan_reminder,
    "picto-plan-repeat" to R.string.pictogram_picto_plan_repeat,
    "picto-plan-star" to R.string.pictogram_picto_plan_star,
    "picto-plan-talk" to R.string.pictogram_picto_plan_talk,
    "picto-plan-videocall" to R.string.pictogram_picto_plan_videocall,
    "picto-plan-write" to R.string.pictogram_picto_plan_write,
    "picto-pomodoro" to R.string.pictogram_picto_pomodoro,
    "picto-target" to R.string.pictogram_picto_target,
    "picto-timer" to R.string.pictogram_picto_timer,
    "viri-focused" to R.string.pictogram_viri_focused,
    "viri-studying" to R.string.pictogram_viri_studying,
    )
}
