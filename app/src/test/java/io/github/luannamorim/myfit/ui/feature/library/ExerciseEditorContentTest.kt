package io.github.luannamorim.myfit.ui.feature.library

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A library exercise shows its name, group (body map), movement type, drawing, video link and star, group
 * and movement with only their own value; how sets count stays a choice. A custom one keeps every field.
 * Section headers are shown in capitals.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR")
class ExerciseEditorContentTest {
    @get:Rule val compose = createComposeRule()

    private val library = ExerciseEditorUiState(
        isLoading = false,
        isNew = false,
        name = "Supino reto com barra",
        muscleGroup = MuscleGroup.CHEST,
        movementType = MovementType.PUSH,
        seedKey = "barbell-bench-press",
        isLibrary = true,
    )

    @Test
    fun libraryExercise_onlyMeasureAndStarChange_noNotesOrDelete() {
        var state by mutableStateOf(library)
        compose.setContent {
            MyFitTheme(ThemeMode.LIGHT) {
                ExerciseEditorContent(
                    state,
                    onToggleFavorite = { state = state.copy(favorite = !state.favorite) },
                    onMeasure = { state = state.copy(measure = it) },
                )
            }
        }
        compose.onNodeWithText("Exercício").assertExists()
        compose.onNodeWithText("Supino reto com barra").assertExists()
        compose.onNodeWithText("Ver execução").assertExists()
        // Group and movement type show only this exercise's value, not every option.
        listOf("GRUPO MUSCULAR", "Peito", "TIPO DE MOVIMENTO", "Empurrar", "IMAGEM DEMONSTRATIVA")
            .forEach { compose.onNodeWithText(it).assertExists() }
        listOf("Costas", "Puxar", "Escolher da galeria", "Observações", "Excluir", "Salvar")
            .forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        compose.onNodeWithText("Peito").assertHasNoClickAction()

        // How sets count is still a choice.
        compose.onNodeWithText("CADA SÉRIE CONTA").assertExists()
        compose.onNodeWithText("Tempo").performScrollTo().performClick()
        compose.onNodeWithText("Tempo").assertIsSelected()
        compose.onNodeWithText("Repetições").assertIsNotSelected()

        val star = compose.onNodeWithContentDescription("Favorito")
        star.assertIsOff()
        star.performClick()
        star.assertIsOn()
    }

    @Test
    fun customExercise_keepsFields_withoutNotes() {
        compose.setContent {
            MyFitTheme(ThemeMode.LIGHT) {
                ExerciseEditorContent(library.copy(name = "Meu exercício", seedKey = null, isLibrary = false))
            }
        }
        listOf("Editar exercício", "GRUPO MUSCULAR", "TIPO DE MOVIMENTO", "CADA SÉRIE CONTA", "Escolher da galeria", "Excluir", "Salvar")
            .forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Observações").assertDoesNotExist()
        compose.onNodeWithContentDescription("Favorito").assertExists()
    }

    @Test
    fun newExercise_hasNoStarYet() {
        compose.setContent {
            MyFitTheme(ThemeMode.LIGHT) { ExerciseEditorContent(ExerciseEditorUiState(isLoading = false)) }
        }
        compose.onNodeWithText("Novo exercício").assertExists()
        compose.onNodeWithContentDescription("Favorito").assertDoesNotExist()
        compose.onNodeWithText("Excluir").assertDoesNotExist()
    }
}
