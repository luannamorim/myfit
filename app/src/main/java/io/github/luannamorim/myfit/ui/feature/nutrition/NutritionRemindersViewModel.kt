package io.github.luannamorim.myfit.ui.feature.nutrition

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.domain.nutrition.MealReminder
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminderSchedule
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminders
import io.github.luannamorim.myfit.domain.nutrition.WaterReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NutritionRemindersUiState(
    val loading: Boolean = true,
    val reminders: NutritionReminders = NutritionReminders(),
    val notificationsEnabled: Boolean = true,
)

/** Meal times and the water interval; every change is saved at once (the alarms follow via NutritionReminderSync). */
@HiltViewModel
class NutritionRemindersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val notificationsEnabled = MutableStateFlow(readNotificationsEnabled())

    val state: StateFlow<NutritionRemindersUiState> = combine(settings.settings.map { it.nutritionReminders }, notificationsEnabled) { r, n ->
        NutritionRemindersUiState(loading = false, reminders = r, notificationsEnabled = n)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionRemindersUiState())

    /** Adds a daily meal time, named by the hour (breakfast, lunch, snack, dinner); a time already in the list is ignored. */
    fun addMeal(minuteOfDay: Int) {
        val name = context.getString(defaultMealName(minuteOfDay))
        updateMeals { meals ->
            if (meals.any { it.minuteOfDay == minuteOfDay }) meals else meals + MealReminder(NutritionReminderSchedule.nextMealId(meals), minuteOfDay, label = name)
        }
    }

    fun setMealLabel(id: Long, label: String) = updateMeals { meals -> meals.map { if (it.id == id) it.copy(label = label.trim().take(MAX_LABEL)) else it } }

    fun setMealEnabled(id: Long, enabled: Boolean) = updateMeals { meals -> meals.map { if (it.id == id) it.copy(enabled = enabled) else it } }

    fun setMealTime(id: Long, minuteOfDay: Int) = updateMeals { meals -> meals.map { if (it.id == id) it.copy(minuteOfDay = minuteOfDay) else it } }

    fun deleteMeal(id: Long) = updateMeals { meals -> meals.filterNot { it.id == id } }

    fun setWaterEnabled(enabled: Boolean) = updateWater { it.copy(enabled = enabled) }

    fun setWaterEvery(minutes: Int) = updateWater { it.copy(everyMinutes = minutes) }

    /** The window stays in order: a start after the end moves the end with it, and the other way round. */
    fun setWaterFrom(minuteOfDay: Int) = updateWater { it.copy(fromMinute = minuteOfDay, untilMinute = maxOf(it.untilMinute, minuteOfDay)) }

    fun setWaterUntil(minuteOfDay: Int) = updateWater { it.copy(untilMinute = minuteOfDay, fromMinute = minOf(it.fromMinute, minuteOfDay)) }

    /** Re-read on resume: the user may turn notifications on or off in system settings. */
    fun refreshSystem() {
        notificationsEnabled.value = readNotificationsEnabled()
    }

    private fun updateMeals(change: (List<MealReminder>) -> List<MealReminder>) = update { r ->
        r.copy(meals = change(r.meals).sortedWith(compareBy({ it.minuteOfDay }, { it.id })))
    }

    private fun updateWater(change: (WaterReminder) -> WaterReminder) = update { r -> r.copy(water = change(r.water)) }

    private fun update(change: (NutritionReminders) -> NutritionReminders) {
        viewModelScope.launch { settings.update { it.copy(nutritionReminders = change(it.nutritionReminders)) } }
    }

    private fun readNotificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private companion object {
        const val MAX_LABEL = 40
    }
}

/** Before 10:00 breakfast, before 15:00 lunch, before 18:00 snack, then dinner. */
@StringRes
fun defaultMealName(minuteOfDay: Int): Int = when {
    minuteOfDay < 10 * 60 -> R.string.nutrition_meal_breakfast
    minuteOfDay < 15 * 60 -> R.string.nutrition_meal_lunch
    minuteOfDay < 18 * 60 -> R.string.nutrition_meal_snack
    else -> R.string.nutrition_meal_dinner
}
