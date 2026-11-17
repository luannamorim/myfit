package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.data.repository.NutritionRepository
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionTargets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class NutritionUiState(
    val loading: Boolean = true,
    /** The day shown. */
    val date: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    val day: NutritionDay = NutritionDay(LocalDate.MIN, emptyList(), 0, 0),
    val targets: DailyTargets = DailyTargets(null, null, emptySet()),
    val goal: NutritionGoal = NutritionGoal.MAINTAIN,
) {
    /** No future days. */
    val canGoForward: Boolean get() = date < today
}

/**
 * One day of the nutrition log. The shown day lives in [SavedStateHandle] (the user leaves for an AI
 * app and Android may kill MyFit meanwhile), and "today" is re-read on resume and before writing, so
 * a tab kept alive overnight never logs to yesterday by mistake.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val nutrition: NutritionRepository,
    body: BodyRepository,
    history: HistoryRepository,
    activities: ActivityRepository,
    settings: SettingsRepository,
    private val time: TimeSource,
) : ViewModel() {
    private val today = MutableStateFlow(time.today())
    private val shownDay: StateFlow<Long> = handle.getStateFlow(KEY_DAY, today.value.toEpochDay())

    val state: StateFlow<NutritionUiState> = combine(shownDay, today) { day, t -> minOf(LocalDate.ofEpochDay(day), t) to t }
        .distinctUntilChanged()
        .flatMapLatest { (d, t) ->
            val window = NutritionTargets.activityWindow(d)
            combine(
                nutrition.observeDay(d),
                body.observeAssessments(),
                combine(history.observeSummaries(window), activities.observe(window)) { s, a -> HistoryCalculator.activeDays(s, a) },
                settings.settings,
            ) { day, assessments, activeDays, s ->
                val birth = s.birthEpochDay?.let(LocalDate::ofEpochDay)
                NutritionUiState(
                    loading = false,
                    date = d,
                    today = t,
                    day = day,
                    targets = NutritionTargets.forDay(d, assessments, activeDays, s.sex, birth, s.nutritionGoal),
                    goal = s.nutritionGoal,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

    /** The day the screen shows and writes to (never after today). */
    private fun shown(): LocalDate = minOf(LocalDate.ofEpochDay(shownDay.value), today.value)

    fun previousDay() {
        handle[KEY_DAY] = shown().minusDays(1).toEpochDay()
    }

    fun nextDay() {
        val d = shown()
        if (d < today.value) handle[KEY_DAY] = d.plusDays(1).toEpochDay()
    }

    /** Re-reads the date: if the screen was on "today", it moves to the new today. */
    fun refreshToday() {
        val old = today.value
        val now = time.today()
        if (now == old) return
        if (shown() == old) handle[KEY_DAY] = now.toEpochDay()
        today.value = now
    }

    fun addWater(ml: Int) {
        refreshToday()
        val d = shown()
        viewModelScope.launch { nutrition.addWater(d, ml) }
    }

    fun undoWater() {
        refreshToday()
        val d = shown()
        viewModelScope.launch { nutrition.undoLastWater(d) }
    }

    private companion object {
        const val KEY_DAY = "nutrition_day"
    }
}
