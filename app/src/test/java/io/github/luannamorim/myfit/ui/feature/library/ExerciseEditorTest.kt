package io.github.luannamorim.myfit.ui.feature.library

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.ExerciseRepository
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.testutil.LocaleRule
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.ui.navigation.ExerciseEditorRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Library exercises can't be renamed; custom ones (and rows whose key the app no longer knows) can. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ExerciseEditorTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val locale = LocaleRule() // pt-BR
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = inMemoryDb()
    private val repo = ExerciseRepository(db.exerciseDao(), FakeTimeSource(), BackupTrigger { })
    private val images = ImageStore(context, Dispatchers.Unconfined)

    @After fun tearDown() = db.close()

    private suspend fun insert(name: String, seedKey: String?) = db.exerciseDao().insert(
        ExerciseEntity(
            name = name, muscleGroup = MuscleGroup.CHEST, movementType = MovementType.PUSH, isCustom = seedKey == null, createdAtMs = 0,
            seedKey = seedKey,
        ),
    )

    private fun editor(id: Long) =
        ExerciseEditorViewModel(SavedStateHandle(route = ExerciseEditorRoute(id)), repo, images, CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun libraryExercise_nameLocked_customNot() = runBlocking {
        val library = editor(insert("Barbell bench press", "barbell-bench-press")).state.awaitUntil { !it.isLoading }
        assertTrue(library.nameLocked)
        assertEquals("Supino reto com barra", library.name)

        assertFalse(editor(insert("Meu exercício", null)).state.awaitUntil { !it.isLoading }.nameLocked)
        assertFalse(editor(insert("Antigo", "no-such-key")).state.awaitUntil { !it.isLoading }.nameLocked)
    }
}
