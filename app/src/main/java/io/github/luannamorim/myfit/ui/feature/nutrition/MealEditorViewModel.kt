package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.repository.NutritionRepository
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.ui.navigation.MealEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class MealEditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val date: LocalDate = LocalDate.MIN,
    val description: String = "",
    val kcalText: String = "",
    /** Minutes after midnight. */
    val minuteOfDay: Int = 0,
    /** Saved or deleted: the screen closes. */
    val done: Boolean = false,
    /** A save or delete is running: further taps are ignored (no duplicate meals). */
    val busy: Boolean = false,
) {
    val kcal: Int? get() = kcalText.toIntOrNull()?.takeIf { it in Meal.MIN_KCAL..Meal.MAX_KCAL }
    val canSave: Boolean get() = !loading && !busy && kcal != null
    val canAsk: Boolean get() = description.isNotBlank()
}

/**
 * New or existing meal. The fields live in [SavedStateHandle] too: the user leaves for an AI app to
 * get the calories, and Android may kill the app meanwhile.
 */
@HiltViewModel
class MealEditorViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val repo: NutritionRepository,
    time: TimeSource,
) : ViewModel() {
    private val route = handle.toRoute<MealEditorRoute>()
    private val _state = MutableStateFlow(MealEditorState(isNew = route.mealId == 0L, date = LocalDate.ofEpochDay(route.dateEpochDay)))
    val state: StateFlow<MealEditorState> = _state.asStateFlow()

    init {
        val draft = handle.get<Boolean>(KEY_DRAFT) == true
        if (draft) {
            _state.update {
                it.copy(
                    loading = false,
                    description = handle[KEY_DESCRIPTION] ?: "",
                    kcalText = handle[KEY_KCAL] ?: "",
                    minuteOfDay = handle[KEY_MINUTE] ?: 0,
                )
            }
        } else if (route.mealId == 0L) {
            val now = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone()).toLocalTime()
            _state.update { it.copy(loading = false, minuteOfDay = now.hour * 60 + now.minute) }
        } else {
            viewModelScope.launch {
                val meal = repo.getMeal(route.mealId)
                _state.update {
                    if (meal == null) {
                        it.copy(loading = false, isNew = true)
                    } else {
                        it.copy(loading = false, date = meal.date, description = meal.description.orEmpty(), kcalText = meal.kcal.toString(), minuteOfDay = meal.minuteOfDay)
                    }
                }
            }
        }
    }

    fun setDescription(text: String) = edit { it.copy(description = text) }

    fun setKcal(text: String) = edit { it.copy(kcalText = text.filter(Char::isDigit).take(KCAL_DIGITS)) }

    fun setMinute(minuteOfDay: Int) = edit { it.copy(minuteOfDay = minuteOfDay) }

    fun save() {
        val s = _state.value
        val kcal = s.kcal ?: return
        if (s.loading || s.busy || s.done) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val id = if (s.isNew) 0L else route.mealId
            repo.saveMeal(Meal(id = id, date = s.date, minuteOfDay = s.minuteOfDay, description = s.description, kcal = kcal))
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val s = _state.value
        if (s.isNew || s.busy || s.done) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            repo.deleteMeal(route.mealId)
            _state.update { it.copy(done = true) }
        }
    }

    private fun edit(change: (MealEditorState) -> MealEditorState) {
        val next = change(_state.value)
        _state.value = next
        handle[KEY_DRAFT] = true
        handle[KEY_DESCRIPTION] = next.description
        handle[KEY_KCAL] = next.kcalText
        handle[KEY_MINUTE] = next.minuteOfDay
    }

    private companion object {
        const val KEY_DRAFT = "meal_draft"
        const val KEY_DESCRIPTION = "meal_description"
        const val KEY_KCAL = "meal_kcal"
        const val KEY_MINUTE = "meal_minute"
        const val KCAL_DIGITS = 4
    }
}
