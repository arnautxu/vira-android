package cat.vira.android.features.plan

import androidx.annotation.DrawableRes
import cat.vira.android.R

/// Pictogrames triables a Pla, pel nom que es desa (`PlanItem.symbolName`, `Habit.symbolName`).
///
/// Calca `ViraPictogram.Name.planIcons` i `habitIcons` de l'iPhone, en el mateix ordre. Viu aquí
/// fins que `core/design` tingui la cerca per nom per a tota l'app; llavors aquest fitxer marxa.
/// Un nom desconegut (una icona nova de l'iPhone que aquí encara no hi és) cau a la genèrica.
object PlanPictograms {

    val planIcons: List<Pair<String, Int>> = listOf(
        "picto-flag" to R.drawable.picto_flag,
        "picto-calendar" to R.drawable.picto_calendar,
        "picto-plan-deadline" to R.drawable.picto_plan_deadline,
        "picto-plan-alarm" to R.drawable.picto_plan_alarm,
        "picto-plan-reminder" to R.drawable.picto_plan_reminder,
        "picto-plan-repeat" to R.drawable.picto_plan_repeat,
        "picto-plan-checklist" to R.drawable.picto_plan_checklist,
        "picto-plan-write" to R.drawable.picto_plan_write,
        "picto-plan-note" to R.drawable.picto_plan_note,
        "picto-plan-pin" to R.drawable.picto_plan_pin,
        "picto-plan-star" to R.drawable.picto_plan_star,
        "picto-plan-important" to R.drawable.picto_plan_important,
        "picto-plan-check" to R.drawable.picto_plan_check,
        "picto-target" to R.drawable.picto_target,
        "picto-organization" to R.drawable.picto_organization,
        "picto-plan-presentation" to R.drawable.picto_plan_presentation,
        "picto-plan-meeting" to R.drawable.picto_plan_meeting,
        "picto-plan-group" to R.drawable.picto_plan_group,
        "picto-plan-videocall" to R.drawable.picto_plan_videocall,
        "picto-plan-talk" to R.drawable.picto_plan_talk,
        "picto-habit-study" to R.drawable.picto_habit_study,
        "picto-habit-read" to R.drawable.picto_habit_read,
        "picto-pomodoro" to R.drawable.picto_pomodoro,
        "picto-cal-today" to R.drawable.picto_cal_today,
        "picto-cal-week" to R.drawable.picto_cal_week,
        "picto-cal-month" to R.drawable.picto_cal_month,
        "picto-cal-reminder" to R.drawable.picto_cal_reminder,
        "picto-cal-deadline" to R.drawable.picto_cal_deadline,
        "picto-cal-break" to R.drawable.picto_cal_break,
        "picto-cal-task" to R.drawable.picto_cal_task,
        "picto-cal-event" to R.drawable.picto_cal_event,
        "picto-cal-class" to R.drawable.picto_cal_class,
        "picto-cal-exam" to R.drawable.picto_cal_exam,
        "picto-cal-workout" to R.drawable.picto_cal_workout,
        "picto-cal-meeting" to R.drawable.picto_cal_meeting,
        "picto-cal-call" to R.drawable.picto_cal_call,
        "picto-cal-videocall" to R.drawable.picto_cal_videocall,
        "picto-cal-birthday" to R.drawable.picto_cal_birthday,
        "picto-cal-party" to R.drawable.picto_cal_party,
        "picto-cal-coffee" to R.drawable.picto_cal_coffee,
        "picto-cal-meal" to R.drawable.picto_cal_meal,
        "picto-cal-shopping" to R.drawable.picto_cal_shopping,
        "picto-cal-bill" to R.drawable.picto_cal_bill,
        "picto-cal-home" to R.drawable.picto_cal_home,
        "picto-cal-cleaning" to R.drawable.picto_cal_cleaning,
        "picto-cal-dog" to R.drawable.picto_cal_dog,
        "picto-cal-doctor" to R.drawable.picto_cal_doctor,
        "picto-cal-trip" to R.drawable.picto_cal_trip,
        "picto-cal-flight" to R.drawable.picto_cal_flight,
        "picto-cal-freetime" to R.drawable.picto_cal_freetime,
        "picto-cal-sun" to R.drawable.picto_cal_sun,
        "picto-cal-rain" to R.drawable.picto_cal_rain,
    )

    val habitIcons: List<Pair<String, Int>> = listOf(
        "picto-habits" to R.drawable.picto_habits,
        "picto-habit-morning" to R.drawable.picto_habit_morning,
        "picto-habit-wake" to R.drawable.picto_habit_wake,
        "picto-habit-hygiene" to R.drawable.picto_habit_hygiene,
        "picto-habit-water" to R.drawable.picto_habit_water,
        "picto-habit-nutrition" to R.drawable.picto_habit_nutrition,
        "picto-habit-fruit" to R.drawable.picto_habit_fruit,
        "picto-habit-cook" to R.drawable.picto_habit_cook,
        "picto-habit-sleep" to R.drawable.picto_habit_sleep,
        "picto-habit-run" to R.drawable.picto_habit_run,
        "picto-habit-walk" to R.drawable.picto_habit_walk,
        "picto-habit-outing" to R.drawable.picto_habit_outing,
        "picto-habit-bike" to R.drawable.picto_habit_bike,
        "picto-habit-strength" to R.drawable.picto_habit_strength,
        "picto-habit-yoga" to R.drawable.picto_habit_yoga,
        "picto-habit-meditate" to R.drawable.picto_habit_meditate,
        "picto-habit-breathe" to R.drawable.picto_habit_breathe,
        "picto-habit-study" to R.drawable.picto_habit_study,
        "picto-habit-read" to R.drawable.picto_habit_read,
        "picto-habit-write" to R.drawable.picto_habit_write,
        "picto-habit-gratitude" to R.drawable.picto_habit_gratitude,
        "picto-habit-tasks" to R.drawable.picto_habit_tasks,
        "picto-habit-plan" to R.drawable.picto_habit_plan,
        "picto-habit-tidy" to R.drawable.picto_habit_tidy,
        "picto-habit-plants" to R.drawable.picto_habit_plants,
        "picto-habit-friends" to R.drawable.picto_habit_friends,
        "picto-habit-phone" to R.drawable.picto_habit_phone,
        "picto-habit-disconnect" to R.drawable.picto_habit_disconnect,
        "picto-habit-music" to R.drawable.picto_habit_music,
        "picto-habit-sunscreen" to R.drawable.picto_habit_sunscreen,
    )

    const val PLAN_DEFAULT = "picto-flag"
    const val HABIT_DEFAULT = "picto-habits"

    private val byName: Map<String, Int> = (planIcons + habitIcons).toMap()

    @DrawableRes
    fun forPlanItem(name: String): Int = byName[name] ?: R.drawable.picto_flag

    @DrawableRes
    fun forHabit(name: String): Int = byName[name] ?: R.drawable.picto_habits
}
