package io.github.luannamorim.myfit.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import io.github.luannamorim.myfit.ui.feature.library.ExerciseEditorContent
import io.github.luannamorim.myfit.ui.feature.library.ExerciseEditorUiState
import io.github.luannamorim.myfit.ui.feature.settings.CreditsScreen
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class ExerciseVisualScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun muscleMap_everyGroup() = compose.snapBothThemes("muscle_map") {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MuscleGroup.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { group ->
                        Column(Modifier.width(124.dp)) {
                            MuscleMap(group, Modifier.fillMaxWidth().height(120.dp))
                            Text(stringResource(group.labelRes), style = MyFitTheme.type.caption, color = MyFitTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun drawings_andThumbnails() = compose.snapBothThemes("exercise_drawings") {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("barbell-bench-press", "plank", "burpee", "lat-pulldown").forEach { key ->
                ExerciseVisual(null, key, MuscleGroup.CHEST, key, Modifier.fillMaxWidth().height(180.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("deadlift", "treadmill", "hip-thrust", "face-pull", null).forEach { key ->
                    ExerciseThumbnail(null, key, MuscleGroup.GLUTES, Modifier.size(44.dp))
                }
            }
        }
    }

    @Test
    fun editor_libraryExercise() = compose.snapBothThemes("exercise_editor_library") {
        ExerciseEditorContent(
            ExerciseEditorUiState(
                isLoading = false,
                isNew = false,
                name = "Barbell bench press",
                muscleGroup = MuscleGroup.CHEST,
                movementType = MovementType.PUSH,
                seedKey = "barbell-bench-press",
            ),
            {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }

    @Test
    @Config(qualifiers = PHONE)
    fun credits() = compose.snapBothThemes("credits") { CreditsScreen(onBack = {}) }
}
