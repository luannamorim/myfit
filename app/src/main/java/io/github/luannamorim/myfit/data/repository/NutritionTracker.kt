package io.github.luannamorim.myfit.data.repository

import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.domain.nutrition.NutritionTargets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A day's nutrition log with the targets as they were on that day (Nutrition tab and Home). */
@Singleton
class NutritionTracker @Inject constructor(
    private val nutrition: NutritionRepository,
    private val body: BodyRepository,
    private val history: HistoryRepository,
    private val activities: ActivityRepository,
    private val settings: SettingsRepository,
) {
    fun observe(date: LocalDate): Flow<NutritionStatus> {
        val window = NutritionTargets.activityWindow(date)
        return combine(
            nutrition.observeDay(date),
            body.observeAssessments(),
            combine(history.observeSummaries(window), activities.observe(window)) { s, a -> HistoryCalculator.activeDays(s, a) },
            settings.settings,
        ) { day, assessments, activeDays, s ->
            val birth = s.birthEpochDay?.let(LocalDate::ofEpochDay)
            NutritionStatus(day, NutritionTargets.forDay(date, assessments, activeDays, s.sex, birth, s.nutritionGoal), s.nutritionGoal)
        }
    }
}
