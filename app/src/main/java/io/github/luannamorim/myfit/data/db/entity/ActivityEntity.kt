package io.github.luannamorim.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity

/** A logged sport (swimming, bike, soccer...), since v4. */
@Entity(tableName = "activity", indices = [Index("dateEpochDay")])
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sport: Sport,
    val name: String? = null,
    val dateEpochDay: Long,
    val durationMinutes: Int,
    val distanceKm: Double? = null,
    val intensity: Intensity? = null,
    val notes: String? = null,
    /** Relative to the images dir. */
    val imagePath: String? = null,
    val createdAtMs: Long,
)
