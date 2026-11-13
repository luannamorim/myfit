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
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.testutil.LocaleRule
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.ui.navigation.ExerciseEditorRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Library exercises open read-only (official name, no edits, no delete); custom ones (and rows whose key
 * the app no longer knows) stay editable. Any existing exercise can be starred.
 */
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

    private suspend fun insert(name: String, seedKey: String?, notes: String? = null) = db.exerciseDao().insert(
        ExerciseEntity(
            name = name, muscleGroup = MuscleGroup.CHEST, movementType = MovementType.PUSH, isCustom = seedKey == null, createdAtMs = 0,
            seedKey = seedKey, notes = notes,
        ),
    )

    private fun editor(id: Long?) =
        ExerciseEditorViewModel(SavedStateHandle(route = ExerciseEditorRoute(id)), repo, images, CoroutineScope(Dispatchers.Unconfined))

    private fun favoriteOf(id: Long) = db.exerciseDao().observeActive().map { list -> list.single { it.id == id }.favorite }

    @Test
    fun libraryExercise_isReadOnly_customNot() = runBlocking {
        val library = editor(insert("Barbell bench press", "barbell-bench-press")).state.awaitUntil { !it.isLoading }
        assertTrue(library.isLibrary)
        assertEquals("Supino reto com barra", library.name)

        assertFalse(editor(insert("Meu exercício", null)).state.awaitUntil { !it.isLoading }.isLibrary)
        assertFalse(editor(insert("Antigo", "no-such-key")).state.awaitUntil { !it.isLoading }.isLibrary)
    }

    @Test
    fun libraryExercise_cannotBeSavedOrDeleted() = runBlocking {
        val id = insert("Barbell bench press", "barbell-bench-press")
        val before = db.exerciseDao().get(id)
        val vm = editor(id)
        vm.state.awaitUntil { !it.isLoading }
        vm.setGroup(MuscleGroup.BACK)
        assertFalse(vm.save())
        vm.delete()
        assertFalse(vm.state.value.done)
        assertEquals(before, db.exerciseDao().get(id))
    }

    @Test
    fun favorite_isSavedRightAway_forLibraryAndCustom() = runBlocking {
        val library = insert("Barbell bench press", "barbell-bench-press")
        val vm = editor(library)
        assertFalse(vm.state.awaitUntil { !it.isLoading }.favorite)
        vm.toggleFavorite()
        favoriteOf(library).awaitUntil { it }
        assertTrue(editor(library).state.awaitUntil { !it.isLoading }.favorite)
        vm.toggleFavorite()
        favoriteOf(library).awaitUntil { !it }

        val custom = insert("Meu exercício", null)
        val customVm = editor(custom)
        customVm.state.awaitUntil { !it.isLoading }
        customVm.toggleFavorite()
        customVm.setName("Meu exercício novo")
        assertTrue(customVm.save())
        customVm.state.awaitUntil { it.done }
        val saved = db.exerciseDao().get(custom)!!
        assertTrue(saved.favorite)
        assertEquals("Meu exercício novo", saved.name)
    }

    @Test
    fun libraryMeasure_isSavedRightAway_customWaitsForSave() = runBlocking {
        val library = insert("Barbell bench press", "barbell-bench-press")
        val vm = editor(library)
        vm.state.awaitUntil { !it.isLoading }
        vm.setMeasure(ExerciseMeasure.TIME)
        db.exerciseDao().observeActive().map { list -> list.single { it.id == library }.measure }.awaitUntil { it == ExerciseMeasure.TIME }
        assertEquals(ExerciseMeasure.TIME, editor(library).state.awaitUntil { !it.isLoading }.measure)
        assertEquals("Barbell bench press", db.exerciseDao().get(library)!!.name)

        val custom = insert("Meu exercício", null)
        val customVm = editor(custom)
        customVm.state.awaitUntil { !it.isLoading }
        customVm.setMeasure(ExerciseMeasure.TIME)
        assertEquals(ExerciseMeasure.REPS, db.exerciseDao().get(custom)!!.measure)
        assertTrue(customVm.save())
        customVm.state.awaitUntil { it.done }
        assertEquals(ExerciseMeasure.TIME, db.exerciseDao().get(custom)!!.measure)
    }

    @Test
    fun savingCustom_keepsNotesWrittenBefore() = runBlocking {
        val id = insert("Meu exercício", null, notes = "pegada aberta")
        val vm = editor(id)
        vm.state.awaitUntil { !it.isLoading }
        vm.setGroup(MuscleGroup.BACK)
        assertTrue(vm.save())
        vm.state.awaitUntil { it.done }
        val saved = db.exerciseDao().get(id)!!
        assertEquals(MuscleGroup.BACK, saved.muscleGroup)
        assertEquals("pegada aberta", saved.notes)
    }
}
