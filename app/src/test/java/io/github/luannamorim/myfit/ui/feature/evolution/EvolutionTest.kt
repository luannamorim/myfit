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
import io.github.luannamorim.myfit.domain.body.BodyStatus
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.model.PhotoPose
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.testSettings
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
    private val settings = testSettings()

    @After fun tearDown() = db.close()

    private val jan = LocalDate.of(2026, 1, 10)
    private val apr = LocalDate.of(2026, 4, 10)

    private suspend fun seed(): Pair<Long, Long> {
        val a = body.save(BodyAssessment(date = jan, values = mapOf(Measure.WEIGHT to 71.0, Measure.WAIST to 78.0)))
        val b = body.save(BodyAssessment(date = apr, values = mapOf(Measure.WEIGHT to 73.0, Measure.WAIST to 79.0, Measure.CHEST to 96.0, Measure.HEIGHT to 178.0)))
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
        val all = Measure.entries.filterNot { it.derived }.associateWith { it.ordinal + 10.5 } // BMI is never stored
        val id = body.save(BodyAssessment(date = LocalDate.of(2026, 6, 1), values = all, notes = " ok "))
        val list = body.observeAssessments().first()
        assertEquals(listOf(LocalDate.of(2026, 6, 1), apr, jan), list.map { it.date })
        val loaded = body.get(id)!!
        assertEquals(all, loaded.values)
        assertEquals("ok", loaded.notes)
    }

    @Test
    fun editor_requiresAtLeastOneValue() = runBlocking {
        val vm = AssessmentEditorViewModel(SavedStateHandle(route = AssessmentEditorRoute()), body, settings, time)
        vm.state.awaitUntil { !it.loading }
        assertFalse(vm.save())
        assertTrue(vm.state.value.emptyError)
        assertEquals(time.today(), vm.state.value.date)
    }

    @Test
    fun editor_rejectsInvalidDecimal_keepsPrevious() = runBlocking {
        val (_, b) = seed()
        val vm = AssessmentEditorViewModel(SavedStateHandle(route = AssessmentEditorRoute(b)), body, settings, time)
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
    fun editor_showsBmiAndEstimate_live() = runBlocking {
        body.save(BodyAssessment(date = jan, values = mapOf(Measure.WEIGHT to 71.0, Measure.HEIGHT to 178.0)))
        settings.update { it.copy(sex = Sex.MALE) }
        val vm = AssessmentEditorViewModel(SavedStateHandle(route = AssessmentEditorRoute()), body, settings, time)
        vm.state.awaitUntil { !it.loading }
        vm.setField(Measure.WEIGHT, "73,25")
        assertEquals(23.1, vm.state.awaitUntil { it.bmi != null }.bmi!!, 0.0) // height from the other assessment
        vm.setField(Measure.NECK, "38")
        vm.setField(Measure.ABDOMEN, "85")
        assertEquals(16.4, vm.state.awaitUntil { it.estimatedBodyFat != null }.estimatedBodyFat!!, 0.0)
        vm.setField(Measure.BODY_FAT, "20") // typed value wins: no estimate shown
        assertNull(vm.state.awaitUntil { it.estimatedBodyFat == null }.estimatedBodyFat)
    }

    @Test
    fun editor_implausibleHeight_hidesBmi_andBlocksSave() = runBlocking {
        val vm = AssessmentEditorViewModel(SavedStateHandle(route = AssessmentEditorRoute()), body, settings, time)
        vm.state.awaitUntil { !it.loading }
        vm.setField(Measure.WEIGHT, "73")
        vm.setField(Measure.HEIGHT, "1") // first digit of "178": not 1 m
        assertNull(vm.state.value.bmi)
        vm.setField(Measure.HEIGHT, "17")
        assertNull(vm.state.value.bmi)
        assertFalse(vm.save())
        assertTrue(Measure.HEIGHT in vm.state.value.invalid)
        vm.setField(Measure.HEIGHT, "178")
        assertEquals(23.0, vm.state.value.bmi!!, 0.0)
        assertFalse(Measure.HEIGHT in vm.state.value.invalid)
    }

    @Test
    fun editor_heightHint_isTheLastRecordedHeight() = runBlocking {
        body.save(BodyAssessment(date = jan, values = mapOf(Measure.WEIGHT to 71.0, Measure.HEIGHT to 178.0)))
        body.save(BodyAssessment(date = apr, values = mapOf(Measure.WEIGHT to 73.0)))
        val vm = AssessmentEditorViewModel(SavedStateHandle(route = AssessmentEditorRoute()), body, settings, time)
        val state = vm.state.awaitUntil { !it.loading }
        assertEquals(73.0, state.lastValues[Measure.WEIGHT]!!, 0.0)
        assertEquals(178.0, state.lastValues[Measure.HEIGHT]!!, 0.0)
    }

    @Test
    fun editor_heightInMeters_savedAsCm() = runBlocking {
        val vm = AssessmentEditorViewModel(SavedStateHandle(route = AssessmentEditorRoute()), body, settings, time)
        vm.state.awaitUntil { !it.loading }
        vm.setField(Measure.WEIGHT, "73")
        vm.setField(Measure.HEIGHT, "1,78")
        assertEquals(23.0, vm.state.awaitUntil { it.bmi != null }.bmi!!, 0.0)
        assertTrue(vm.save())
        vm.state.awaitUntil { it.done }
        assertEquals(178.0, body.observeAssessments().first().single().values[Measure.HEIGHT]!!, 0.0)
    }

    @Test
    fun compare_defaultsToFirstAndLast() = runBlocking {
        val (a, b) = seed()
        val vm = CompareViewModel(body, settings)
        val state = vm.state.awaitUntil { !it.loading && it.rows.isNotEmpty() }
        assertEquals(a, state.from!!.id)
        assertEquals(b, state.to!!.id)
        val weight = state.rows.single { it.measure == Measure.WEIGHT }
        assertEquals(2.0, weight.diff!!, 0.0)
        assertNull(state.rows.single { it.measure == Measure.CHEST }.diff)
        assertTrue(state.rows.none { it.measure == Measure.NECK })
    }

    private suspend fun seedForEstimate() {
        body.save(BodyAssessment(date = jan, values = mapOf(Measure.WEIGHT to 71.0)))
        body.save(BodyAssessment(date = apr, values = mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 85.0)))
    }

    @Test
    fun metrics_followSexSetting() = runBlocking {
        seedForEstimate()
        val vm = EvolutionViewModel(body, images, settings, time)
        val unset = vm.state.awaitUntil { !it.loading && it.snapshot != null }
        assertEquals(listOf(Measure.WEIGHT, Measure.BMI), unset.snapshot!!.highlights.map { it.measure })
        assertEquals(BodyStatus.GOOD, unset.snapshot!!.highlights.single { it.measure == Measure.BMI }.status) // BMI 23.1
        assertTrue(unset.charts.none { it.measure == Measure.BODY_FAT })

        settings.update { it.copy(sex = Sex.MALE) }
        val male = vm.state.awaitUntil { s -> s.charts.any { it.measure == Measure.BODY_FAT } }
        assertEquals(listOf(Measure.WEIGHT, Measure.BODY_FAT, Measure.BMI), male.snapshot!!.highlights.map { it.measure })
        val fat = male.snapshot!!.highlights.single { it.measure == Measure.BODY_FAT }
        assertEquals(16.4, fat.to!!, 0.0)
        assertTrue(fat.estimated)
        assertEquals(BodyStatus.GOOD, fat.status) // 16.4 % for a man
        assertTrue(male.charts.single { it.measure == Measure.BODY_FAT }.hasEstimates)
        assertFalse(male.charts.single { it.measure == Measure.BMI }.hasEstimates)
        // The list counts only what was typed: weight, height, neck, abdomen.
        assertEquals(4, male.assessments.first { it.date == apr }.measuredCount)
    }

    @Test
    fun compare_includesHeightAndBmi_marksEstimates() = runBlocking {
        seedForEstimate()
        settings.update { it.copy(sex = Sex.MALE) }
        val vm = CompareViewModel(body, settings)
        val rows = vm.state.awaitUntil { s -> s.rows.any { it.measure == Measure.BODY_FAT } }.rows
        assertTrue(rows.any { it.measure == Measure.HEIGHT })
        assertEquals(22.4, rows.single { it.measure == Measure.BMI }.from!!, 0.0) // jan uses apr's height
        val fat = rows.single { it.measure == Measure.BODY_FAT }
        assertEquals(16.4, fat.to!!, 0.0)
        assertTrue(fat.estimated)
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
    fun repository_savesHeight_neverDerivedValues() = runBlocking {
        val derived = BodyAssessment(
            date = apr,
            values = mapOf(Measure.WEIGHT to 73.0, Measure.HEIGHT to 178.0, Measure.BODY_FAT to 16.4, Measure.BMI to 23.0),
            estimated = setOf(Measure.BODY_FAT, Measure.BMI),
        )
        val id = body.save(derived)
        assertEquals(mapOf(Measure.WEIGHT to 73.0, Measure.HEIGHT to 178.0), body.get(id)!!.values)
    }

    @Test
    fun chartRange_fitsData() {
        assertEquals(70.0 to 75.0, yRange(listOf(71.0, 73.25)))
        assertEquals(0.0 to 2.0, yRange(listOf(0.5)))
    }

    @Test
    fun charts_selectionKeptInViewModel_andPhotosFlow() = runBlocking {
        seed()
        val vm = EvolutionViewModel(body, images, settings, time)
        val initial = vm.state.awaitUntil { !it.loading }
        // Every chart measure starts selected (height has no chart); only the measured ones get a chart.
        assertEquals(CHART_MEASURES.toSet(), initial.selectedMeasures)
        assertFalse(Measure.HEIGHT in CHART_MEASURES)
        assertEquals(listOf(Measure.WEIGHT, Measure.BMI, Measure.CHEST, Measure.WAIST), initial.charts.map { it.measure })

        vm.toggleMeasure(Measure.WAIST)
        val noWaist = vm.state.awaitUntil { Measure.WAIST !in it.selectedMeasures }
        assertEquals(listOf(Measure.WEIGHT, Measure.BMI, Measure.CHEST), noWaist.charts.map { it.measure })

        // "All" selects every measure again, and a second tap clears the selection.
        vm.toggleAllMeasures()
        val all = vm.state.awaitUntil { it.selectedMeasures.size == CHART_MEASURES.size }
        assertEquals(listOf(Measure.WEIGHT, Measure.BMI, Measure.CHEST, Measure.WAIST), all.charts.map { it.measure })
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
