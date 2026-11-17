package io.github.luannamorim.myfit.ui.feature.nutrition

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutrition: NutritionRepository,
    body: BodyRepository,
    history: HistoryRepository,
    activities: ActivityRepository,
    settings: SettingsRepository,
    time: TimeSource,
) : ViewModel() {
    private val today = time.today()
    private val date = MutableStateFlow(today)

    val state: StateFlow<NutritionUiState> = date.flatMapLatest { d ->
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
                today = today,
                day = day,
                targets = NutritionTargets.forDay(d, assessments, activeDays, s.sex, birth, s.nutritionGoal),
                goal = s.nutritionGoal,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())

    fun previousDay() = date.update { it.minusDays(1) }

    fun nextDay() = date.update { if (it < today) it.plusDays(1) else it }

    fun addWater(ml: Int) {
        val d = date.value
        viewModelScope.launch { nutrition.addWater(d, ml) }
    }

    fun undoWater() {
        val d = date.value
        viewModelScope.launch { nutrition.undoLastWater(d) }
    }
}
