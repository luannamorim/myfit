package io.github.luannamorim.myfit.ui.feature.routines

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.Exercise
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.testutil.snapBothThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE)
class RoutinesScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val today = 20_400L

    @Test
    fun routines_empty() = compose.snapBothThemes("routines_empty") {
        RoutinesContent(RoutinesUiState(isLoading = false), {}, {}, {}, {}, {}, {})
    }

    @Test
    fun routines_list() = compose.snapBothThemes("routines_list") {
        RoutinesContent(
            RoutinesUiState(
                routines = listOf(
                    RoutineOverview(1, "Treino Costas", null, 6, today - 3),
                    RoutineOverview(2, "Treino Peito e Tríceps", null, 7, today - 1),
                    RoutineOverview(3, "Pernas", null, 5, today),
                    RoutineOverview(4, "Novo treino", null, 0, null),
                ),
                todayEpochDay = today,
                isLoading = false,
            ),
            {}, {}, {}, {}, {}, {},
        )
    }

    @Test
    fun routineEditor() = compose.snapBothThemes("routine_editor") {
        fun ex(id: Long, name: String) = Exercise(id, name, MuscleGroup.BACK, MovementType.PULL)
        RoutineEditorContent(
            state = RoutineEditorUiState(
                isLoading = false,
                isNew = false,
                name = "Treino Costas",
                description = "Foco em espessura",
                items = listOf(
                    DraftItem(1, ex(1, "Remada curvada"), 3, 12, 90, 40.0, null),
                    DraftItem(2, ex(2, "Remada unilateral"), 3, 10, 60, 22.5, null),
                    DraftItem(3, ex(3, "Pullover"), 3, 12, 60, null, null),
                ),
            ),
            onNameChange = {}, onDescriptionChange = {}, onAddExercises = {}, onMove = { _, _ -> },
            onUpdateItem = { _, _ -> }, onRemoveItem = {}, onSave = {}, onClose = {},
        )
    }
}
