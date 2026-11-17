package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.MealReminder
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminders
import io.github.luannamorim.myfit.domain.nutrition.WaterReminder
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.TargetMissing
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class NutritionScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 17)

    @Test
    fun nutrition_withTargets() {
        val meals = listOf(
            Meal(1, today, 8 * 60 + 10, "Bread with eggs and coffee", 320),
            Meal(2, today, 12 * 60 + 30, "Rice, beans, grilled chicken and salad", 920),
        )
        compose.snapBothThemes("nutrition") {
            NutritionContent(
                NutritionUiState(
                    loading = false, date = today, today = today, day = NutritionDay(today, meals, 1500, 4),
                    targets = DailyTargets(2300, 2800, emptySet()), goal = NutritionGoal.MAINTAIN,
                ),
                NutritionActions(),
            )
        }
    }

    @Test
    fun nutrition_missingProfile() {
        compose.snapBothThemes("nutrition_missing") {
            NutritionContent(
                NutritionUiState(
                    loading = false, date = today.minusDays(3), today = today, day = NutritionDay(today.minusDays(3), emptyList(), 0, 0),
                    targets = DailyTargets(null, 2800, setOf(TargetMissing.HEIGHT, TargetMissing.SEX, TargetMissing.BIRTH_DATE)),
                ),
                NutritionActions(),
            )
        }
    }

    @Test
    fun mealEditor() {
        compose.snapBothThemes("meal_editor") {
            MealEditorContent(
                MealEditorState(loading = false, isNew = true, date = today, description = "Rice, beans, grilled chicken and salad", kcalText = "610", minuteOfDay = 12 * 60 + 30),
                MealEditorActions(),
            )
        }
    }

    @Test
    fun nutritionReminders() {
        compose.snapBothThemes("nutrition_reminders") {
            NutritionRemindersContent(
                NutritionRemindersUiState(
                    loading = false,
                    reminders = NutritionReminders(
                        meals = listOf(MealReminder(1, 7 * 60), MealReminder(2, 12 * 60 + 30), MealReminder(3, 20 * 60, enabled = false)),
                        water = WaterReminder(enabled = true, everyMinutes = 120, fromMinute = 8 * 60, untilMinute = 22 * 60),
                    ),
                ),
                NutritionRemindersActions(),
                is24Hour = false,
            )
        }
    }
}
