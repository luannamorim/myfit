package io.github.luannamorim.myfit.ui.feature.library

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * A library exercise shows only its drawing, name, video link and star; a custom one keeps every field.
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
    fun libraryExercise_hasNoEditsNotesOrDelete_butCanBeStarred() {
        var state by mutableStateOf(library)
        compose.setContent {
            MyFitTheme(ThemeMode.LIGHT) {
                ExerciseEditorContent(state, onToggleFavorite = { state = state.copy(favorite = !state.favorite) })
            }
        }
        compose.onNodeWithText("Exercício").assertExists()
        compose.onNodeWithText("Supino reto com barra").assertExists()
        compose.onNodeWithText("Ver execução").assertExists()
        listOf("GRUPO MUSCULAR", "TIPO DE MOVIMENTO", "CADA SÉRIE CONTA", "Escolher da galeria", "Observações", "Excluir", "Salvar")
            .forEach { compose.onNodeWithText(it).assertDoesNotExist() }

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
