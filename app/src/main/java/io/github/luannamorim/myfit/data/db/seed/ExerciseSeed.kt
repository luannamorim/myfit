package io.github.luannamorim.myfit.data.db.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import io.github.luannamorim.myfit.core.format.SearchText
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
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
    /** Drawing slug: `assets/exercises/<art>-1.webp` (start) and, when it moves, `-2.webp` (end). */
    val art: String? = null,
    /** Timed exercises (holds, cardio) count seconds instead of reps. */
    val measure: ExerciseMeasure = ExerciseMeasure.REPS,
    /** Stable identity stored in `exercise.seedKey`; never change it once released. */
    val key: String = keyOf(nameEn),
) {
    /** Name in the device language at first launch (Portuguese for pt-*, English otherwise). */
    fun name(locale: Locale = Locale.getDefault()): String = if (locale.language == "pt") namePt else nameEn

    companion object {
        fun keyOf(nameEn: String): String =
            SearchText.normalize(nameEn).replace("°", "").replace("'", "").replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }
}

/** Predefined exercise library (PT-BR and English), inserted once when the database is created. */
object ExerciseSeed {
    val items: List<SeedExercise> = listOf(
        // Peito
        SeedExercise("Supino reto com barra", "Barbell bench press", CHEST, PUSH, art = "bench-press"),
        SeedExercise("Supino reto com halteres", "Dumbbell bench press", CHEST, PUSH, art = "bench-press-dumbbell"),
        SeedExercise("Supino inclinado com barra", "Incline barbell bench press", CHEST, PUSH, art = "incline-bench-press"),
        SeedExercise("Supino inclinado com halteres", "Incline dumbbell press", CHEST, PUSH, art = "incline-dumbbell-press"),
        SeedExercise("Supino declinado", "Decline bench press", CHEST, PUSH, art = "decline-barbell-bench-press"),
        SeedExercise("Supino na máquina", "Machine chest press", CHEST, PUSH, art = "machine-bench-press"),
        SeedExercise("Crucifixo com halteres", "Dumbbell fly", CHEST, PUSH, art = "dumbbell-flys"),
        SeedExercise("Crucifixo na máquina (peck deck)", "Pec deck fly", CHEST, PUSH, art = "butterfly-machine"),
        SeedExercise("Crossover na polia", "Cable crossover", CHEST, PUSH, art = "cable-crossover"),
        SeedExercise("Flexão de braço", "Push-up", CHEST, PUSH, art = "push-ups"),
        SeedExercise("Mergulho nas paralelas", "Parallel bar dip", CHEST, PUSH, art = "chest-dips"),
        // Costas
        SeedExercise("Puxada frontal", "Lat pulldown", BACK, PULL, art = "wide-grip-lat-pull-down"),
        SeedExercise("Puxada com pegada neutra", "Neutral-grip pulldown", BACK, PULL, art = "v-bar-pull-down"),
        SeedExercise("Barra fixa", "Pull-up", BACK, PULL, art = "pull-ups"),
        SeedExercise("Remada curvada", "Bent-over row", BACK, PULL, art = "bent-over-row-with-barbell"),
        SeedExercise("Remada unilateral", "One-arm dumbbell row", BACK, PULL, art = "rear-deltoid-row-dumbbell"),
        SeedExercise("Remada baixa na polia", "Seated cable row", BACK, PULL, art = "seated-cable-rows"),
        SeedExercise("Remada cavalinho", "T-bar row", BACK, PULL, art = "t-bar-rows"),
        SeedExercise("Remada na máquina", "Machine row", BACK, PULL, art = "wg-machine-row"),
        SeedExercise("Pullover", "Pullover", BACK, PULL, art = "straight-arm-dumbbell-pullover"),
        SeedExercise("Pulldown com braços estendidos", "Straight-arm pulldown", BACK, PULL, art = "straight-arm-push-down"),
        SeedExercise("Levantamento terra", "Deadlift", BACK, PULL, art = "barbell-dead-lifts"),
        SeedExercise("Encolhimento com halteres", "Dumbbell shrug", BACK, PULL, art = "shoulder-shrugs"),
        SeedExercise("Hiperextensão lombar", "Back extension", BACK, PULL, art = "hyperextensions"),
        // Ombros
        SeedExercise("Desenvolvimento com halteres", "Dumbbell shoulder press", SHOULDERS, PUSH, art = "dumbbell-shoulder-press"),
        SeedExercise("Desenvolvimento militar com barra", "Barbell overhead press", SHOULDERS, PUSH, art = "barbell-shoulder-press"),
        SeedExercise("Desenvolvimento na máquina", "Machine shoulder press", SHOULDERS, PUSH, art = "seated-shoulder-press-machine"),
        SeedExercise("Elevação lateral", "Lateral raise", SHOULDERS, PUSH, art = "lateral-dumbbell-raises"),
        SeedExercise("Elevação lateral na polia", "Cable lateral raise", SHOULDERS, PUSH, art = "wg-cable-lateral-raise"),
        SeedExercise("Elevação frontal", "Front raise", SHOULDERS, PUSH, art = "front-dumbbell-raise"),
        SeedExercise("Crucifixo inverso", "Reverse fly", SHOULDERS, PULL, art = "bent-over-rear-deltoid-raise-with-head-on-bench"),
        SeedExercise("Face pull", "Face pull", SHOULDERS, PULL, art = "wg-face-pull"),
        // Bíceps
        SeedExercise("Rosca direta com barra", "Barbell curl", BICEPS, PULL, art = "biceps-curls-with-barbell"),
        SeedExercise("Rosca alternada com halteres", "Alternating dumbbell curl", BICEPS, PULL, art = "alternating-biceps-curl-with-dumbbell"),
        SeedExercise("Rosca martelo", "Hammer curl", BICEPS, PULL, art = "biceps-hammer-curl-with-dumbbell"),
        SeedExercise("Rosca Scott", "Preacher curl", BICEPS, PULL, art = "preacher-curl-with-barbell"),
        SeedExercise("Rosca concentrada", "Concentration curl", BICEPS, PULL, art = "concentration-curls-with-dumbbell"),
        SeedExercise("Rosca na polia", "Cable curl", BICEPS, PULL, art = "standing-biceps-curl-with-cable"),
        // Tríceps
        SeedExercise("Tríceps corda", "Rope triceps pushdown", TRICEPS, PUSH, art = "triceps-pushdown-with-rope-and-cable"),
        SeedExercise("Tríceps na polia com barra", "Bar triceps pushdown", TRICEPS, PUSH, art = "triceps-pushdown-with-cable"),
        SeedExercise("Tríceps testa", "Skull crusher", TRICEPS, PUSH, art = "lying-triceps-press-with-barbell"),
        SeedExercise("Tríceps francês", "Overhead triceps extension", TRICEPS, PUSH, art = "standing-triceps-extension-2"),
        SeedExercise("Mergulho no banco", "Bench dip", TRICEPS, PUSH, art = "bench-dips"),
        SeedExercise("Supino fechado", "Close-grip bench press", TRICEPS, PUSH, art = "close-grip-barbell-bench-press"),
        // Antebraço
        SeedExercise("Rosca de punho", "Wrist curl", FOREARMS, PULL, art = "wg-wrist-curl"),
        SeedExercise("Rosca inversa", "Reverse curl", FOREARMS, PULL, art = "biceps-curl-reverse-with-dumbbells"),
        // Abdômen
        SeedExercise("Prancha", "Plank", ABS, OTHER, art = "wg-plank", measure = ExerciseMeasure.TIME),
        SeedExercise("Abdominal supra", "Crunch", ABS, OTHER, art = "crunches"),
        SeedExercise("Elevação de pernas", "Leg raise", ABS, OTHER, art = "flat-bench-leg-raises"),
        SeedExercise("Abdominal oblíquo", "Oblique crunch", ABS, OTHER, art = "cross-body-crunch"),
        SeedExercise("Abdominal na polia", "Cable crunch", ABS, OTHER, art = "seated-ab-crunch-with-cable"),
        SeedExercise("Roda abdominal", "Ab wheel rollout", ABS, OTHER, art = "ab-rollout-on-knees-with-barbell"),
        // Quadríceps
        SeedExercise("Agachamento livre", "Barbell back squat", QUADRICEPS, LEGS, art = "barbell-squat"),
        SeedExercise("Agachamento frontal", "Front squat", QUADRICEPS, LEGS, art = "front-squat-with-barbell"),
        SeedExercise("Agachamento búlgaro", "Bulgarian split squat", QUADRICEPS, LEGS, art = "one-leg-squat-with-barbell"),
        SeedExercise("Agachamento hack", "Hack squat", QUADRICEPS, LEGS, art = "hack-squat-machine"),
        SeedExercise("Leg press 45°", "45° leg press", QUADRICEPS, LEGS, art = "leg-press"),
        SeedExercise("Cadeira extensora", "Leg extension", QUADRICEPS, LEGS, art = "leg-extensions"),
        SeedExercise("Afundo com halteres", "Dumbbell lunge", QUADRICEPS, LEGS, art = "dumbbell-lunges"),
        // Posterior
        SeedExercise("Stiff", "Stiff-leg deadlift", HAMSTRINGS, LEGS, art = "romanian-dead-lift"),
        SeedExercise("Levantamento terra romeno", "Romanian deadlift", HAMSTRINGS, LEGS, art = "romanian-dead-lift"),
        SeedExercise("Mesa flexora", "Lying leg curl", HAMSTRINGS, LEGS, art = "lying-leg-curl-machine"),
        SeedExercise("Cadeira flexora", "Seated leg curl", HAMSTRINGS, LEGS, art = "seated-leg-curl"),
        SeedExercise("Bom dia", "Good morning", HAMSTRINGS, LEGS, art = "barbell-good-mornings"),
        // Glúteos
        SeedExercise("Elevação pélvica", "Hip thrust", GLUTES, LEGS, art = "wg-hip-thrust"),
        SeedExercise("Glúteo na polia", "Cable kickback", GLUTES, LEGS, art = "one-legged-cable-kickback"),
        SeedExercise("Cadeira abdutora", "Hip abduction machine", GLUTES, LEGS, art = "thigh-abductor"),
        SeedExercise("Agachamento sumô", "Sumo squat", GLUTES, LEGS, art = "pile-squat-with-dumbbell"),
        SeedExercise("Cadeira adutora", "Hip adduction machine", MuscleGroup.OTHER, LEGS, art = "thigh-adductor"),
        // Panturrilha
        SeedExercise("Panturrilha em pé", "Standing calf raise", CALVES, LEGS, art = "standing-calf-raises-using-machine"),
        SeedExercise("Panturrilha sentado", "Seated calf raise", CALVES, LEGS, art = "seated-calf-raise-using-machine"),
        SeedExercise("Panturrilha no leg press", "Leg press calf raise", CALVES, LEGS, art = "calves-press-on-leg-machine"),
        // Corpo inteiro
        SeedExercise("Burpee", "Burpee", FULL_BODY, OTHER, art = "wg-burpee"),
        SeedExercise("Kettlebell swing", "Kettlebell swing", FULL_BODY, OTHER, art = "wg-kettlebell-swing"),
        SeedExercise("Caminhada do fazendeiro", "Farmer's walk", FULL_BODY, OTHER, art = "wg-farmer-carry"),
        // Cardio
        SeedExercise("Esteira", "Treadmill", CARDIO, OTHER, art = "wg-running", measure = ExerciseMeasure.TIME),
        SeedExercise("Bicicleta ergométrica", "Stationary bike", CARDIO, OTHER, art = "wg-cycling", measure = ExerciseMeasure.TIME),
        SeedExercise("Elíptico", "Elliptical", CARDIO, OTHER, art = "wg-elliptical", measure = ExerciseMeasure.TIME),
        SeedExercise("Remo ergômetro", "Rowing machine", CARDIO, OTHER, art = "wg-rowing", measure = ExerciseMeasure.TIME),
        SeedExercise("Pular corda", "Jump rope", CARDIO, OTHER, art = "wg-jump-rope", measure = ExerciseMeasure.TIME),
        SeedExercise("Escada", "Stair climber", CARDIO, OTHER, art = "wg-stair-climber", measure = ExerciseMeasure.TIME),
        // Version 2: bodyweight and kettlebell additions.
        SeedExercise("Agachamento goblet", "Goblet squat", QUADRICEPS, LEGS, since = 2, art = "wg-goblet-squat"),
        SeedExercise("Afundo reverso com kettlebell", "Kettlebell reverse lunge", QUADRICEPS, LEGS, since = 2, art = "rear-lunges-with-dumbbell"),
        SeedExercise("Levantamento terra romeno unilateral", "Single-leg Romanian deadlift", HAMSTRINGS, LEGS, since = 2, art = "wg-single-leg-romanian-deadlift"),
        SeedExercise("Levantamento terra romeno com kettlebell", "Kettlebell Romanian deadlift", HAMSTRINGS, LEGS, since = 2, art = "wg-kettlebell-romanian-deadlift"),
        SeedExercise("Panturrilha em pé unilateral", "Single-leg standing calf raise", CALVES, LEGS, since = 2, art = "wg-single-leg-calf-raise"),
        SeedExercise("Remada unilateral com kettlebell", "Single-arm kettlebell row", BACK, PULL, since = 2, art = "rear-deltoid-row-dumbbell"),
        SeedExercise("Desenvolvimento unilateral com kettlebell", "Single-arm kettlebell shoulder press", SHOULDERS, PUSH, since = 2, art = "one-arm-dumbbell-shoulder-press"),
        SeedExercise("Supino no chão unilateral com kettlebell", "Single-arm kettlebell floor press", CHEST, PUSH, since = 2, art = "one-arm-barbell-floor-press"),
        SeedExercise("Flexão pike", "Pike push-up", SHOULDERS, PUSH, since = 2, art = "wg-pike-push-up"),
        SeedExercise("Rosca bíceps", "Biceps curl", BICEPS, PULL, since = 2, art = "biceps-curl-with-dumbbell"),
        SeedExercise("Abdominal reverso", "Reverse crunch", ABS, OTHER, since = 2, art = "bent-knee-hip-raise"),
        SeedExercise("Prancha lateral", "Side plank", ABS, OTHER, since = 2, art = "side-plank", measure = ExerciseMeasure.TIME),
        // Version 3: exercises that come with an Everkinetic drawing.
        SeedExercise("Supino reto no Smith", "Smith machine bench press", CHEST, PUSH, since = 3, art = "smith-machine-bench-press"),
        SeedExercise("Supino inclinado no Smith", "Smith machine incline press", CHEST, PUSH, since = 3, art = "smith-machine-incline-bench-press"),
        SeedExercise("Supino declinado com halteres", "Decline dumbbell press", CHEST, PUSH, since = 3, art = "decline-dumbbell-bench-press"),
        SeedExercise("Crucifixo inclinado com halteres", "Incline dumbbell fly", CHEST, PUSH, since = 3, art = "incline-dumbbell-flys"),
        SeedExercise("Crucifixo na polia no banco reto", "Flat bench cable fly", CHEST, PUSH, since = 3, art = "flat-bench-cable-flys"),
        SeedExercise("Crucifixo na polia no banco inclinado", "Incline cable fly", CHEST, PUSH, since = 3, art = "incline-cable-flys"),
        SeedExercise("Flexão com pés elevados", "Decline push-up", CHEST, PUSH, since = 3, art = "push-up-feet-elevated"),
        SeedExercise("Barra fixa supinada", "Chin-up", BACK, PULL, since = 3, art = "chin-ups"),
        SeedExercise("Barra fixa aberta", "Wide-grip pull-up", BACK, PULL, since = 3, art = "wide-grip-chin-up"),
        SeedExercise("Barra fixa com pegada neutra", "Neutral-grip pull-up", BACK, PULL, since = 3, art = "narrow-parallel-grip-chin-ups"),
        SeedExercise("Puxada supinada", "Underhand pulldown", BACK, PULL, since = 3, art = "underhand-pull-down"),
        SeedExercise("Remada curvada supinada", "Reverse-grip bent-over row", BACK, PULL, since = 3, art = "reverse-grips-bent-over-barbell-rows"),
        SeedExercise("Remada invertida", "Inverted row", BACK, PULL, since = 3, art = "body-row"),
        SeedExercise("Levantamento terra com halteres", "Dumbbell deadlift", BACK, PULL, since = 3, art = "dumbbell-dead-lifts"),
        SeedExercise("Super-homem", "Superman", BACK, PULL, since = 3, art = "supermans"),
        SeedExercise("Encolhimento com barra", "Barbell shrug", BACK, PULL, since = 3, art = "barbell-shrugs"),
        SeedExercise("Encolhimento na polia", "Cable shrug", BACK, PULL, since = 3, art = "cable-shoulder-shrugs"),
        SeedExercise("Desenvolvimento Arnold", "Arnold press", SHOULDERS, PUSH, since = 3, art = "arnold-press"),
        SeedExercise("Remada alta com barra", "Barbell upright row", SHOULDERS, PULL, since = 3, art = "upright-barbell-rows"),
        SeedExercise("Remada alta com halteres", "Dumbbell upright row", SHOULDERS, PULL, since = 3, art = "dumbbell-upright-rows"),
        SeedExercise("Elevação frontal na polia", "Cable front raise", SHOULDERS, PUSH, since = 3, art = "front-cable-raises"),
        SeedExercise("Crucifixo inverso na polia", "Cable reverse fly", SHOULDERS, PULL, since = 3, art = "bent-over-lateral-cable-raises"),
        SeedExercise("Rosca com barra W", "EZ-bar curl", BICEPS, PULL, since = 3, art = "ez-bar-curl-with-barbell"),
        SeedExercise("Rosca spider", "Spider curl", BICEPS, PULL, since = 3, art = "spider-curl-with-barbell"),
        SeedExercise("Rosca Zottman", "Zottman curl", BICEPS, PULL, since = 3, art = "zottman-curl-with-dumbbells"),
        SeedExercise("Rosca martelo na polia com corda", "Rope hammer curl", BICEPS, PULL, since = 3, art = "hammer-curls-with-rope-and-cable"),
        SeedExercise("Rosca martelo cruzada", "Cross-body hammer curl", BICEPS, PULL, since = 3, art = "cross-body-hammer-curl-with-dumbbell"),
        SeedExercise("Rosca Scott na máquina", "Machine preacher curl", BICEPS, PULL, since = 3, art = "preacher-curl-with-machine"),
        SeedExercise("Rosca na máquina", "Machine biceps curl", BICEPS, PULL, since = 3, art = "biceps-curl-with-machine"),
        SeedExercise("Rosca na polia alta", "High cable curl", BICEPS, PULL, since = 3, art = "high-cable-curls"),
        SeedExercise("Tríceps coice", "Triceps kickback", TRICEPS, PUSH, since = 3, art = "triceps-kickback-with-dumbbell"),
        SeedExercise("Tate press", "Tate press", TRICEPS, PUSH, since = 3, art = "tate-press-with-dumbbell"),
        SeedExercise("Tríceps na polia com barra V", "V-bar triceps pushdown", TRICEPS, PUSH, since = 3, art = "triceps-pushdown-with-v-bar-and-cable"),
        SeedExercise("Tríceps na polia com pegada invertida", "Reverse-grip triceps pushdown", TRICEPS, PUSH, since = 3, art = "reverse-grip-triceps-pushdown"),
        SeedExercise("Tríceps testa com halteres", "Lying dumbbell triceps extension", TRICEPS, PUSH, since = 3, art = "lying-triceps-extension-with-dumbbells"),
        SeedExercise("Mergulho na máquina", "Machine dip", TRICEPS, PUSH, since = 3, art = "tricep-dips"),
        SeedExercise("Tríceps na máquina", "Machine triceps extension", TRICEPS, PUSH, since = 3, art = "triceps-extensions-using-machine"),
        SeedExercise("Flexão diamante", "Diamond push-up", TRICEPS, PUSH, since = 3, art = "close-triceps-pushup"),
        SeedExercise("Abdominal declinado", "Decline crunch", ABS, OTHER, since = 3, art = "decline-crunch"),
        SeedExercise("Abdominal bicicleta", "Bicycle crunch", ABS, OTHER, since = 3, art = "air-bike"),
        SeedExercise("Flexão lateral com halter", "Dumbbell side bend", ABS, OTHER, since = 3, art = "side-bend-with-dumbbell"),
        SeedExercise("Abdominal tesoura", "Flutter kicks", ABS, OTHER, since = 3, art = "flutter-kicks"),
        SeedExercise("Agachamento no Smith", "Smith machine squat", QUADRICEPS, LEGS, since = 3, art = "smith-machine-squats"),
        SeedExercise("Agachamento com halteres", "Dumbbell squat", QUADRICEPS, LEGS, since = 3, art = "squats-using-dumbbells"),
        SeedExercise("Agachamento no banco", "Box squat", QUADRICEPS, LEGS, since = 3, art = "squat-to-bench-with-barbell"),
        SeedExercise("Agachamento overhead", "Overhead squat", QUADRICEPS, LEGS, since = 3, art = "overhead-squat-with-barbell"),
        SeedExercise("Agachamento sissy", "Sissy squat", QUADRICEPS, LEGS, since = 3, art = "weighted-sissy-squat-with-weight-plate"),
        SeedExercise("Agachamento Zercher", "Zercher squat", QUADRICEPS, LEGS, since = 3, art = "zecher-squats"),
        SeedExercise("Leg press com pés fechados", "Narrow-stance leg press", QUADRICEPS, LEGS, since = 3, art = "narrow-stance-leg-press"),
        SeedExercise("Afundo com barra", "Barbell lunge", QUADRICEPS, LEGS, since = 3, art = "barbell-lunges"),
        SeedExercise("Afundo caminhando", "Walking lunge", QUADRICEPS, LEGS, since = 3, art = "walking-lunges"),
        SeedExercise("Afundo reverso com barra", "Barbell reverse lunge", QUADRICEPS, LEGS, since = 3, art = "rear-lunges-with-barbell"),
        SeedExercise("Subida no banco com halteres", "Dumbbell step-up", QUADRICEPS, LEGS, since = 3, art = "step-ups-with-dumbbells"),
        SeedExercise("Subida no banco com barra", "Barbell step-up", QUADRICEPS, LEGS, since = 3, art = "step-ups-with-barbell"),
        SeedExercise("Agachamento lateral com barra", "Barbell lateral squat", QUADRICEPS, LEGS, since = 3, art = "side-squats-with-barbell"),
        SeedExercise("Flexora em pé", "Standing leg curl", HAMSTRINGS, LEGS, since = 3, art = "standing-leg-curls"),
        SeedExercise("Ponte de glúteo", "Glute bridge", GLUTES, LEGS, since = 3, art = "bridging"),
        SeedExercise("Panturrilha burrinho", "Donkey calf raise", CALVES, LEGS, since = 3, art = "donkey-calf-raises"),
        SeedExercise("Panturrilha em pé com barra", "Standing barbell calf raise", CALVES, LEGS, since = 3, art = "standing-barbell-calf-raise"),
    )

    /** Latest library version (the database version that last added exercises). */
    const val VERSION = 3

    private val byKey: Map<String, SeedExercise> by lazy { items.associateBy { it.key } }

    /** The library entry behind an exercise row, or null for exercises the user created. */
    fun forKey(key: String?): SeedExercise? = key?.let(byKey::get)

    /**
     * Inserts [candidates] whose name (in either language, ignoring case and accents) is not in the
     * table yet, so user-created exercises with the same name are never duplicated. Returns how many.
     * [withKeys] is false only for migrations that run before the `seedKey` column exists.
     */
    fun insertMissing(
        db: SupportSQLiteDatabase,
        candidates: List<SeedExercise>,
        locale: Locale,
        nowMs: Long,
        withKeys: Boolean = true,
        /** False before schema v5, when the `measure` column doesn't exist yet. */
        withMeasure: Boolean = true,
    ): Int {
        val existing = mutableSetOf<String>()
        db.query("SELECT name FROM exercise").use { c -> while (c.moveToNext()) existing += SearchText.normalize(c.getString(0)) }
        val usedKeys = if (withKeys) existingKeys(db) else mutableSetOf()
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
                if (withKeys && usedKeys.add(item.key)) put("seedKey", item.key)
                if (withMeasure) put("measure", item.measure.name)
            }
            db.insert("exercise", SQLiteDatabase.CONFLICT_ABORT, values)
            existing += SearchText.normalize(item.name(locale))
            inserted++
        }
        return inserted
    }

    /**
     * Gives rows without a `seedKey` the key of the library entry with the same name (either
     * language, ignoring case and accents), library rows first. A renamed exercise stays unlinked.
     * Session snapshots inherit the key of their exercise. Returns how many exercises were linked.
     */
    fun linkKeys(db: SupportSQLiteDatabase): Int {
        val used = existingKeys(db)
        val byName = HashMap<String, SeedExercise>()
        items.forEach { item ->
            byName.putIfAbsent(SearchText.normalize(item.namePt), item)
            byName.putIfAbsent(SearchText.normalize(item.nameEn), item)
        }
        var linked = 0
        db.query("SELECT id, name FROM exercise WHERE seedKey IS NULL ORDER BY isCustom, archived, id").use { c ->
            while (c.moveToNext()) {
                val item = byName[SearchText.normalize(c.getString(1))] ?: continue
                if (!used.add(item.key)) continue
                db.execSQL("UPDATE exercise SET seedKey = ? WHERE id = ?", arrayOf<Any>(item.key, c.getLong(0)))
                linked++
            }
        }
        db.execSQL(
            "UPDATE session_exercise SET seedKey = (SELECT e.seedKey FROM exercise e WHERE e.id = session_exercise.exerciseId) " +
                "WHERE seedKey IS NULL AND exerciseId IS NOT NULL",
        )
        return linked
    }

    private fun existingKeys(db: SupportSQLiteDatabase): MutableSet<String> {
        val keys = mutableSetOf<String>()
        db.query("SELECT seedKey FROM exercise WHERE seedKey IS NOT NULL").use { c -> while (c.moveToNext()) keys += c.getString(0) }
        return keys
    }

    /** 1 → 2: adds the version-2 exercises to existing installs (schema unchanged). */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            insertMissing(db, items.filter { it.since == 2 }, Locale.getDefault(), System.currentTimeMillis(), withKeys = false, withMeasure = false)
        }
    }

    /**
     * 2 → 3: adds `seedKey` to exercises and session snapshots (so library exercises keep their
     * drawing after a rename), links the existing library rows by name and adds the version-3 exercises.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE exercise ADD COLUMN seedKey TEXT")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_exercise_seedKey ON exercise (seedKey)")
            db.execSQL("ALTER TABLE session_exercise ADD COLUMN seedKey TEXT")
            linkKeys(db)
            insertMissing(db, items.filter { it.since == 3 }, Locale.getDefault(), System.currentTimeMillis(), withMeasure = false)
        }
    }

    val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

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
