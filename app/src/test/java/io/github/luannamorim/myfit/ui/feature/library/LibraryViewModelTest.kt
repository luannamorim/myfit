package io.github.luannamorim.myfit.ui.feature.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.images.ImageKind
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.ExerciseRepository
import io.github.luannamorim.myfit.domain.model.Exercise
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
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
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class LibraryViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = inMemoryDb(seed = true)
    private val repo = ExerciseRepository(db.exerciseDao(), FakeTimeSource(), BackupTrigger { })
    private val images = ImageStore(context, Dispatchers.Unconfined)

    @After fun tearDown() = db.close()

    private fun image(name: String): Uri {
        val file = File(context.cacheDir, "$name.jpg")
        file.outputStream().use {
            Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
                .compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        return Uri.fromFile(file)
    }

    @Test
    fun filterByGroup_andQuery_combine() = runBlocking {
        val vm = LibraryViewModel(repo)
        vm.state.awaitUntil { !it.isLoading && it.exercises.isNotEmpty() }
        vm.toggleGroup(MuscleGroup.BACK)
        vm.setQuery("remada")
        val state = vm.state.awaitUntil { s -> s.query == "remada" && s.exercises.all { it.muscleGroup == MuscleGroup.BACK } }
        assertTrue(state.exercises.isNotEmpty())
        assertTrue(state.exercises.all { it.name.lowercase().contains("remada") })
        vm.toggleSelected(state.exercises[1].id)
        vm.toggleSelected(state.exercises[0].id)
        assertEquals(listOf(state.exercises[1].id, state.exercises[0].id), vm.state.awaitUntil { it.selected.size == 2 }.selected)
    }

    @Test
    fun editor_blankName_cannotSave() = runBlocking {
        val vm = ExerciseEditorViewModel(SavedStateHandle(route = ExerciseEditorRoute()), repo, images, CoroutineScope(Dispatchers.Unconfined))
        vm.state.awaitUntil { !it.isLoading }
        vm.setName("   ")
        assertFalse(vm.save())
        assertTrue(vm.state.value.nameError)
    }

    @Test
    fun editor_replaceImage_deletesOldFile() = runBlocking {
        val oldPath = images.import(image("old"), ImageKind.EXERCISE)
        val id = repo.save(Exercise(name = "Remada Pendlay", muscleGroup = MuscleGroup.BACK, movementType = MovementType.PULL, imagePath = oldPath))
        val vm = ExerciseEditorViewModel(SavedStateHandle(route = ExerciseEditorRoute(id)), repo, images, CoroutineScope(Dispatchers.Unconfined))
        vm.state.awaitUntil { !it.isLoading }
        vm.pickImage(image("new"))
        val newPath = vm.state.awaitUntil { it.imagePath != oldPath && !it.isImporting }.imagePath!!
        assertTrue(vm.save())
        vm.state.awaitUntil { it.done }
        assertFalse(images.file(oldPath).exists())
        assertTrue(images.file(newPath).exists())
        assertEquals(newPath, repo.get(id)!!.imagePath)
    }
}
