package io.github.luannamorim.myfit.ui.feature.evolution

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.model.PhotoPose
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.ui.navigation.AssessmentEditorRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class EvolutionTest {
    @get:Rule val main = MainDispatcherRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private val body = BodyRepository(db.bodyDao(), time, BackupTrigger { })
    private val images = ImageStore(context, Dispatchers.Unconfined)

    @After fun tearDown() = db.close()

    private val jan = LocalDate.of(2026, 1, 10)
    private val apr = LocalDate.of(2026, 4, 10)

    private suspend fun seed(): Pair<Long, Long> {
        val a = body.save(BodyAssessment(date = jan, values = mapOf(Measure.WEIGHT to 71.0, Measure.WAIST to 78.0)))
        val b = body.save(BodyAssessment(date = apr, values = mapOf(Measure.WEIGHT to 73.0, Measure.WAIST to 79.0, Measure.CHEST to 96.0)))
        return a to b
    }

    private fun image(name: String): Uri {
        val file = File(context.cacheDir, "$name.jpg")
        file.outputStream().use { Bitmap.createBitmap(30, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun repository_save_thenObserve_newestFirst_andRoundTripsAllMeasures() = runBlocking {
        seed()
        val all = Measure.entries.associateWith { it.ordinal + 10.5 }
        val id = body.save(BodyAssessment(date = LocalDate.of(2026, 6, 1), values = all, notes = " ok "))
        val list = body.observeAssessments().first()
        assertEquals(listOf(LocalDate.of(2026, 6, 1), apr, jan), list.map { it.date })
        val loaded = body.get(id)!!
        assertEquals(all, loaded.values)
        assertEquals("ok", loaded.notes)
    }

    @Test
    fun editor_requiresAtLeastOneValue() = runBlocking {
        val vm = AssessmentEditorViewModel(SavedStateHandle(route = AssessmentEditorRoute()), body, time)
        vm.state.awaitUntil { !it.loading }
        assertFalse(vm.save())
        assertTrue(vm.state.value.emptyError)
        assertEquals(time.today(), vm.state.value.date)
    }

    @Test
    fun editor_rejectsInvalidDecimal_keepsPrevious() = runBlocking {
        val (_, b) = seed()
        val vm = AssessmentEditorViewModel(SavedStateHandle(route = AssessmentEditorRoute(b)), body, time)
        val loaded = vm.state.awaitUntil { !it.loading }
        assertEquals("73", loaded.fields[Measure.WEIGHT])
        assertEquals(73.0 - 2, loaded.lastValues[Measure.WEIGHT]!!, 0.0) // hint from the other assessment
        vm.setField(Measure.WEIGHT, "7a")
        assertFalse(vm.save())
        assertTrue(Measure.WEIGHT in vm.state.value.invalid)
        assertEquals(73.0, body.get(b)!!.values[Measure.WEIGHT]!!, 0.0)
        vm.setField(Measure.WEIGHT, "73,4")
        assertTrue(vm.save())
        vm.state.awaitUntil { it.done }
        assertEquals(73.4, body.get(b)!!.values[Measure.WEIGHT]!!, 0.0)
    }

    @Test
    fun compare_defaultsToFirstAndLast() = runBlocking {
        val (a, b) = seed()
        val vm = CompareViewModel(body)
        val state = vm.state.awaitUntil { !it.loading && it.rows.isNotEmpty() }
        assertEquals(a, state.from!!.id)
        assertEquals(b, state.to!!.id)
        val weight = state.rows.single { it.measure == Measure.WEIGHT }
        assertEquals(2.0, weight.diff!!, 0.0)
        assertNull(state.rows.single { it.measure == Measure.CHEST }.diff)
        assertTrue(state.rows.none { it.measure == Measure.NECK })
    }

    @Test
    fun charts_periodFilter_trimsSeries() {
        val today = LocalDate.of(2026, 11, 12)
        val items = listOf(
            BodyAssessment(1, LocalDate.of(2025, 6, 1), mapOf(Measure.WEIGHT to 70.0)),
            BodyAssessment(2, LocalDate.of(2026, 3, 1), mapOf(Measure.WEIGHT to 72.0)),
            BodyAssessment(3, LocalDate.of(2026, 10, 1), mapOf(Measure.WEIGHT to 73.5)),
        )
        val all = buildCharts(items, setOf(Measure.WEIGHT), ChartPeriod.ALL, today).single()
        assertEquals(3, all.points.size)
        assertEquals(3.5, all.change!!, 0.0)
        val year = buildCharts(items, setOf(Measure.WEIGHT), ChartPeriod.YEAR, today).single()
        assertEquals(2, year.points.size)
        val half = buildCharts(items, setOf(Measure.WEIGHT), ChartPeriod.SIX_MONTHS, today).single()
        assertEquals(1, half.points.size)
        assertNull(half.change)
    }

    @Test
    fun chartRange_fitsData() {
        assertEquals(70.0 to 75.0, yRange(listOf(71.0, 73.25)))
        assertEquals(0.0 to 2.0, yRange(listOf(0.5)))
    }

    @Test
    fun charts_selectionKeptInViewModel_andPhotosFlow() = runBlocking {
        seed()
        val vm = EvolutionViewModel(body, images, time)
        vm.state.awaitUntil { !it.loading }
        vm.toggleMeasure(Measure.WAIST)
        vm.toggleMeasure(Measure.CHEST)
        val state = vm.state.awaitUntil { Measure.CHEST in it.selectedMeasures }
        // Right arm is selected by default but was never measured: no empty card for it.
        assertEquals(listOf(Measure.WEIGHT, Measure.CHEST), state.charts.map { it.measure })

        // "All" selects every measure (charts only for measured ones), and again clears the selection.
        vm.toggleAllMeasures()
        val all = vm.state.awaitUntil { it.selectedMeasures.size == Measure.entries.size }
        assertEquals(listOf(Measure.WEIGHT, Measure.CHEST, Measure.WAIST), all.charts.map { it.measure }.sortedBy { listOf(Measure.WEIGHT, Measure.CHEST, Measure.WAIST).indexOf(it) })
        vm.toggleAllMeasures()
        assertTrue(vm.state.awaitUntil { it.selectedMeasures.isEmpty() }.charts.isEmpty())

        // Photos default to today and can be back-dated.
        assertEquals(time.today(), vm.state.value.photoDate)
        vm.setPhotoDate(jan)
        assertEquals(jan, vm.state.awaitUntil { it.photoDate == jan }.photoDate)

        // Photos: grouped newest first, compare keeps exactly two, delete removes file.
        vm.addPhoto(image("p1"), jan, PhotoPose.FRONT)
        vm.addPhoto(image("p2"), apr, PhotoPose.FRONT)
        vm.addPhoto(image("p3"), apr, PhotoPose.SIDE)
        val photos = vm.state.awaitUntil { it.photos.size == 3 }.photos
        assertEquals(listOf(apr, apr, jan), photos.map { it.date })
        photos.forEach { vm.togglePhoto(it.id) }
        val selected = vm.state.awaitUntil { it.selectedPhotos.size == 2 }.selectedPhotos
        assertEquals(listOf(photos[1].id, photos[2].id), selected)

        val toDelete = photos.first { it.id == selected[0] }
        vm.togglePhoto(selected[1]) // keep only one selected
        vm.state.awaitUntil { it.selectedPhotos.size == 1 }
        vm.deleteSelectedPhotos()
        vm.state.awaitUntil { it.photos.size == 2 }
        assertFalse(images.file(toDelete.imagePath).exists())
    }
}
