package io.github.luannamorim.myfit.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.db.entity.BodyAssessmentEntity
import io.github.luannamorim.myfit.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BodyDaoTest {
    private val db = inMemoryDb()

    @After fun tearDown() = db.close()

    @Test
    fun assessments_orderedByDateDesc() = runTest {
        listOf(20_000L, 20_060L, 20_030L).forEach { day ->
            db.bodyDao().insertAssessment(BodyAssessmentEntity(dateEpochDay = day, weightKg = 70.0, createdAtMs = 0, updatedAtMs = 0))
        }
        assertEquals(listOf(20_060L, 20_030L, 20_000L), db.bodyDao().observeAssessments().first().map { it.dateEpochDay })
    }
}
