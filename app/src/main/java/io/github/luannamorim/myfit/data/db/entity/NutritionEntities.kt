package io.github.luannamorim.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One glass/bottle of water added on a day, since v8. Kept per entry so the last one can be undone. */
@Entity(tableName = "water_entry", indices = [Index("dateEpochDay")])
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val ml: Int,
    val createdAtMs: Long,
)

/** A logged meal, since v8: free-text description and the calories the user typed. */
@Entity(tableName = "meal", indices = [Index("dateEpochDay")])
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    /** Minutes after midnight. */
    val minuteOfDay: Int,
    val description: String? = null,
    val kcal: Int,
    val createdAtMs: Long,
)
