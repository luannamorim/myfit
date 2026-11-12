package io.github.luannamorim.myfit.data.db.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import io.github.luannamorim.myfit.core.format.SearchText
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MovementType.LEGS
import io.github.luannamorim.myfit.domain.model.MovementType.OTHER
import io.github.luannamorim.myfit.domain.model.MovementType.PULL
import io.github.luannamorim.myfit.domain.model.MovementType.PUSH
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.model.MuscleGroup.ABS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.BACK
import io.github.luannamorim.myfit.domain.model.MuscleGroup.BICEPS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.CALVES
import io.github.luannamorim.myfit.domain.model.MuscleGroup.CARDIO
import io.github.luannamorim.myfit.domain.model.MuscleGroup.CHEST
import io.github.luannamorim.myfit.domain.model.MuscleGroup.FOREARMS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.FULL_BODY
import io.github.luannamorim.myfit.domain.model.MuscleGroup.GLUTES
import io.github.luannamorim.myfit.domain.model.MuscleGroup.HAMSTRINGS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.QUADRICEPS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.SHOULDERS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.TRICEPS

data class SeedExercise(
    val namePt: String,
    val nameEn: String,
    val muscleGroup: MuscleGroup,
    val movementType: MovementType,
    /** Library version that introduced it (existing installs get it through a migration). */
    val since: Int = 1,
) {
    /** Name in the device language at first launch (Portuguese for pt-*, English otherwise). */
    fun name(locale: Locale = Locale.getDefault()): String = if (locale.language == "pt") namePt else nameEn
}

/** Predefined exercise library (PT-BR and English), inserted once when the database is created. */
object ExerciseSeed {
    val items: List<SeedExercise> = listOf(
        // Peito
        SeedExercise("Supino reto com barra", "Barbell bench press", CHEST, PUSH),
        SeedExercise("Supino reto com halteres", "Dumbbell bench press", CHEST, PUSH),
        SeedExercise("Supino inclinado com barra", "Incline barbell bench press", CHEST, PUSH),
        SeedExercise("Supino inclinado com halteres", "Incline dumbbell press", CHEST, PUSH),
        SeedExercise("Supino declinado", "Decline bench press", CHEST, PUSH),
        SeedExercise("Supino na máquina", "Machine chest press", CHEST, PUSH),
        SeedExercise("Crucifixo com halteres", "Dumbbell fly", CHEST, PUSH),
        SeedExercise("Crucifixo na máquina (peck deck)", "Pec deck fly", CHEST, PUSH),
        SeedExercise("Crossover na polia", "Cable crossover", CHEST, PUSH),
        SeedExercise("Flexão de braço", "Push-up", CHEST, PUSH),
        SeedExercise("Mergulho nas paralelas", "Parallel bar dip", CHEST, PUSH),
        // Costas
        SeedExercise("Puxada frontal", "Lat pulldown", BACK, PULL),
        SeedExercise("Puxada com pegada neutra", "Neutral-grip pulldown", BACK, PULL),
        SeedExercise("Barra fixa", "Pull-up", BACK, PULL),
        SeedExercise("Remada curvada", "Bent-over row", BACK, PULL),
        SeedExercise("Remada unilateral", "One-arm dumbbell row", BACK, PULL),
        SeedExercise("Remada baixa na polia", "Seated cable row", BACK, PULL),
        SeedExercise("Remada cavalinho", "T-bar row", BACK, PULL),
        SeedExercise("Remada na máquina", "Machine row", BACK, PULL),
        SeedExercise("Pullover", "Pullover", BACK, PULL),
        SeedExercise("Pulldown com braços estendidos", "Straight-arm pulldown", BACK, PULL),
        SeedExercise("Levantamento terra", "Deadlift", BACK, PULL),
        SeedExercise("Encolhimento com halteres", "Dumbbell shrug", BACK, PULL),
        SeedExercise("Hiperextensão lombar", "Back extension", BACK, PULL),
        // Ombros
        SeedExercise("Desenvolvimento com halteres", "Dumbbell shoulder press", SHOULDERS, PUSH),
        SeedExercise("Desenvolvimento militar com barra", "Barbell overhead press", SHOULDERS, PUSH),
        SeedExercise("Desenvolvimento na máquina", "Machine shoulder press", SHOULDERS, PUSH),
        SeedExercise("Elevação lateral", "Lateral raise", SHOULDERS, PUSH),
        SeedExercise("Elevação lateral na polia", "Cable lateral raise", SHOULDERS, PUSH),
        SeedExercise("Elevação frontal", "Front raise", SHOULDERS, PUSH),
        SeedExercise("Crucifixo inverso", "Reverse fly", SHOULDERS, PULL),
        SeedExercise("Face pull", "Face pull", SHOULDERS, PULL),
        // Bíceps
        SeedExercise("Rosca direta com barra", "Barbell curl", BICEPS, PULL),
        SeedExercise("Rosca alternada com halteres", "Alternating dumbbell curl", BICEPS, PULL),
        SeedExercise("Rosca martelo", "Hammer curl", BICEPS, PULL),
        SeedExercise("Rosca Scott", "Preacher curl", BICEPS, PULL),
        SeedExercise("Rosca concentrada", "Concentration curl", BICEPS, PULL),
        SeedExercise("Rosca na polia", "Cable curl", BICEPS, PULL),
        // Tríceps
        SeedExercise("Tríceps corda", "Rope triceps pushdown", TRICEPS, PUSH),
        SeedExercise("Tríceps na polia com barra", "Bar triceps pushdown", TRICEPS, PUSH),
        SeedExercise("Tríceps testa", "Skull crusher", TRICEPS, PUSH),
        SeedExercise("Tríceps francês", "Overhead triceps extension", TRICEPS, PUSH),
        SeedExercise("Mergulho no banco", "Bench dip", TRICEPS, PUSH),
        SeedExercise("Supino fechado", "Close-grip bench press", TRICEPS, PUSH),
        // Antebraço
        SeedExercise("Rosca de punho", "Wrist curl", FOREARMS, PULL),
        SeedExercise("Rosca inversa", "Reverse curl", FOREARMS, PULL),
        // Abdômen
        SeedExercise("Prancha", "Plank", ABS, OTHER),
        SeedExercise("Abdominal supra", "Crunch", ABS, OTHER),
        SeedExercise("Elevação de pernas", "Leg raise", ABS, OTHER),
        SeedExercise("Abdominal oblíquo", "Oblique crunch", ABS, OTHER),
        SeedExercise("Abdominal na polia", "Cable crunch", ABS, OTHER),
        SeedExercise("Roda abdominal", "Ab wheel rollout", ABS, OTHER),
        // Quadríceps
        SeedExercise("Agachamento livre", "Barbell back squat", QUADRICEPS, LEGS),
        SeedExercise("Agachamento frontal", "Front squat", QUADRICEPS, LEGS),
        SeedExercise("Agachamento búlgaro", "Bulgarian split squat", QUADRICEPS, LEGS),
        SeedExercise("Agachamento hack", "Hack squat", QUADRICEPS, LEGS),
        SeedExercise("Leg press 45°", "45° leg press", QUADRICEPS, LEGS),
        SeedExercise("Cadeira extensora", "Leg extension", QUADRICEPS, LEGS),
        SeedExercise("Afundo com halteres", "Dumbbell lunge", QUADRICEPS, LEGS),
        // Posterior
        SeedExercise("Stiff", "Stiff-leg deadlift", HAMSTRINGS, LEGS),
        SeedExercise("Levantamento terra romeno", "Romanian deadlift", HAMSTRINGS, LEGS),
        SeedExercise("Mesa flexora", "Lying leg curl", HAMSTRINGS, LEGS),
        SeedExercise("Cadeira flexora", "Seated leg curl", HAMSTRINGS, LEGS),
        SeedExercise("Bom dia", "Good morning", HAMSTRINGS, LEGS),
        // Glúteos
        SeedExercise("Elevação pélvica", "Hip thrust", GLUTES, LEGS),
        SeedExercise("Glúteo na polia", "Cable kickback", GLUTES, LEGS),
        SeedExercise("Cadeira abdutora", "Hip abduction machine", GLUTES, LEGS),
        SeedExercise("Agachamento sumô", "Sumo squat", GLUTES, LEGS),
        SeedExercise("Cadeira adutora", "Hip adduction machine", MuscleGroup.OTHER, LEGS),
        // Panturrilha
        SeedExercise("Panturrilha em pé", "Standing calf raise", CALVES, LEGS),
        SeedExercise("Panturrilha sentado", "Seated calf raise", CALVES, LEGS),
        SeedExercise("Panturrilha no leg press", "Leg press calf raise", CALVES, LEGS),
        // Corpo inteiro
        SeedExercise("Burpee", "Burpee", FULL_BODY, OTHER),
        SeedExercise("Kettlebell swing", "Kettlebell swing", FULL_BODY, OTHER),
        SeedExercise("Caminhada do fazendeiro", "Farmer's walk", FULL_BODY, OTHER),
        // Cardio
        SeedExercise("Esteira", "Treadmill", CARDIO, OTHER),
        SeedExercise("Bicicleta ergométrica", "Stationary bike", CARDIO, OTHER),
        SeedExercise("Elíptico", "Elliptical", CARDIO, OTHER),
        SeedExercise("Remo ergômetro", "Rowing machine", CARDIO, OTHER),
        SeedExercise("Pular corda", "Jump rope", CARDIO, OTHER),
        SeedExercise("Escada", "Stair climber", CARDIO, OTHER),
        // Version 2: bodyweight and kettlebell additions.
        SeedExercise("Agachamento goblet", "Goblet squat", QUADRICEPS, LEGS, since = 2),
        SeedExercise("Afundo reverso com kettlebell", "Kettlebell reverse lunge", QUADRICEPS, LEGS, since = 2),
        SeedExercise("Levantamento terra romeno unilateral", "Single-leg Romanian deadlift", HAMSTRINGS, LEGS, since = 2),
        SeedExercise("Levantamento terra romeno com kettlebell", "Kettlebell Romanian deadlift", HAMSTRINGS, LEGS, since = 2),
        SeedExercise("Panturrilha em pé unilateral", "Single-leg standing calf raise", CALVES, LEGS, since = 2),
        SeedExercise("Remada unilateral com kettlebell", "Single-arm kettlebell row", BACK, PULL, since = 2),
        SeedExercise("Desenvolvimento unilateral com kettlebell", "Single-arm kettlebell shoulder press", SHOULDERS, PUSH, since = 2),
        SeedExercise("Supino no chão unilateral com kettlebell", "Single-arm kettlebell floor press", CHEST, PUSH, since = 2),
        SeedExercise("Flexão pike", "Pike push-up", SHOULDERS, PUSH, since = 2),
        SeedExercise("Rosca bíceps", "Biceps curl", BICEPS, PULL, since = 2),
        SeedExercise("Abdominal reverso", "Reverse crunch", ABS, OTHER, since = 2),
        SeedExercise("Prancha lateral", "Side plank", ABS, OTHER, since = 2),
    )

    /** Latest library version (the database version that last added exercises). */
    const val VERSION = 2

    /**
     * Inserts [candidates] whose name (in either language, ignoring case and accents) is not in the
     * table yet, so user-created exercises with the same name are never duplicated. Returns how many.
     */
    fun insertMissing(db: SupportSQLiteDatabase, candidates: List<SeedExercise>, locale: Locale, nowMs: Long): Int {
        val existing = mutableSetOf<String>()
        db.query("SELECT name FROM exercise").use { c -> while (c.moveToNext()) existing += SearchText.normalize(c.getString(0)) }
        var inserted = 0
        candidates.forEach { item ->
            if (SearchText.normalize(item.namePt) in existing || SearchText.normalize(item.nameEn) in existing) return@forEach
            val values = ContentValues().apply {
                put("name", item.name(locale))
                put("muscleGroup", item.muscleGroup.name)
                put("movementType", item.movementType.name)
                put("isCustom", 0)
                put("archived", 0)
                put("createdAtMs", nowMs)
            }
            db.insert("exercise", SQLiteDatabase.CONFLICT_ABORT, values)
            existing += SearchText.normalize(item.name(locale))
            inserted++
        }
        return inserted
    }

    /** 1 → 2: adds the version-2 exercises to existing installs (schema unchanged). */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            insertMissing(db, items.filter { it.since == 2 }, Locale.getDefault(), System.currentTimeMillis())
        }
    }

    /** Room callback that inserts the whole library when the database file is first created. */
    class Callback(
        private val nowMs: () -> Long = System::currentTimeMillis,
        private val locale: () -> Locale = Locale::getDefault,
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.beginTransaction()
            try {
                insertMissing(db, items, locale(), nowMs())
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
