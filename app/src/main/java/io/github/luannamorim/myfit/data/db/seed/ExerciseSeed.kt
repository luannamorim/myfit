package io.github.luannamorim.myfit.data.db.seed

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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

data class SeedExercise(val name: String, val muscleGroup: MuscleGroup, val movementType: MovementType)

/** Predefined exercise library (PT-BR), inserted once when the database is created. */
object ExerciseSeed {
    val items: List<SeedExercise> = listOf(
        // Peito
        SeedExercise("Supino reto com barra", CHEST, PUSH),
        SeedExercise("Supino reto com halteres", CHEST, PUSH),
        SeedExercise("Supino inclinado com barra", CHEST, PUSH),
        SeedExercise("Supino inclinado com halteres", CHEST, PUSH),
        SeedExercise("Supino declinado", CHEST, PUSH),
        SeedExercise("Supino na máquina", CHEST, PUSH),
        SeedExercise("Crucifixo com halteres", CHEST, PUSH),
        SeedExercise("Crucifixo na máquina (peck deck)", CHEST, PUSH),
        SeedExercise("Crossover na polia", CHEST, PUSH),
        SeedExercise("Flexão de braço", CHEST, PUSH),
        SeedExercise("Mergulho nas paralelas", CHEST, PUSH),
        // Costas
        SeedExercise("Puxada frontal", BACK, PULL),
        SeedExercise("Puxada com pegada neutra", BACK, PULL),
        SeedExercise("Barra fixa", BACK, PULL),
        SeedExercise("Remada curvada", BACK, PULL),
        SeedExercise("Remada unilateral", BACK, PULL),
        SeedExercise("Remada baixa na polia", BACK, PULL),
        SeedExercise("Remada cavalinho", BACK, PULL),
        SeedExercise("Remada na máquina", BACK, PULL),
        SeedExercise("Pullover", BACK, PULL),
        SeedExercise("Pulldown com braços estendidos", BACK, PULL),
        SeedExercise("Levantamento terra", BACK, PULL),
        SeedExercise("Encolhimento com halteres", BACK, PULL),
        SeedExercise("Hiperextensão lombar", BACK, PULL),
        // Ombros
        SeedExercise("Desenvolvimento com halteres", SHOULDERS, PUSH),
        SeedExercise("Desenvolvimento militar com barra", SHOULDERS, PUSH),
        SeedExercise("Desenvolvimento na máquina", SHOULDERS, PUSH),
        SeedExercise("Elevação lateral", SHOULDERS, PUSH),
        SeedExercise("Elevação lateral na polia", SHOULDERS, PUSH),
        SeedExercise("Elevação frontal", SHOULDERS, PUSH),
        SeedExercise("Crucifixo inverso", SHOULDERS, PULL),
        SeedExercise("Face pull", SHOULDERS, PULL),
        // Bíceps
        SeedExercise("Rosca direta com barra", BICEPS, PULL),
        SeedExercise("Rosca alternada com halteres", BICEPS, PULL),
        SeedExercise("Rosca martelo", BICEPS, PULL),
        SeedExercise("Rosca Scott", BICEPS, PULL),
        SeedExercise("Rosca concentrada", BICEPS, PULL),
        SeedExercise("Rosca na polia", BICEPS, PULL),
        // Tríceps
        SeedExercise("Tríceps corda", TRICEPS, PUSH),
        SeedExercise("Tríceps na polia com barra", TRICEPS, PUSH),
        SeedExercise("Tríceps testa", TRICEPS, PUSH),
        SeedExercise("Tríceps francês", TRICEPS, PUSH),
        SeedExercise("Mergulho no banco", TRICEPS, PUSH),
        SeedExercise("Supino fechado", TRICEPS, PUSH),
        // Antebraço
        SeedExercise("Rosca de punho", FOREARMS, PULL),
        SeedExercise("Rosca inversa", FOREARMS, PULL),
        // Abdômen
        SeedExercise("Prancha", ABS, OTHER),
        SeedExercise("Abdominal supra", ABS, OTHER),
        SeedExercise("Elevação de pernas", ABS, OTHER),
        SeedExercise("Abdominal oblíquo", ABS, OTHER),
        SeedExercise("Abdominal na polia", ABS, OTHER),
        SeedExercise("Roda abdominal", ABS, OTHER),
        // Quadríceps
        SeedExercise("Agachamento livre", QUADRICEPS, LEGS),
        SeedExercise("Agachamento frontal", QUADRICEPS, LEGS),
        SeedExercise("Agachamento búlgaro", QUADRICEPS, LEGS),
        SeedExercise("Agachamento hack", QUADRICEPS, LEGS),
        SeedExercise("Leg press 45°", QUADRICEPS, LEGS),
        SeedExercise("Cadeira extensora", QUADRICEPS, LEGS),
        SeedExercise("Afundo com halteres", QUADRICEPS, LEGS),
        // Posterior
        SeedExercise("Stiff", HAMSTRINGS, LEGS),
        SeedExercise("Levantamento terra romeno", HAMSTRINGS, LEGS),
        SeedExercise("Mesa flexora", HAMSTRINGS, LEGS),
        SeedExercise("Cadeira flexora", HAMSTRINGS, LEGS),
        SeedExercise("Bom dia", HAMSTRINGS, LEGS),
        // Glúteos
        SeedExercise("Elevação pélvica", GLUTES, LEGS),
        SeedExercise("Glúteo na polia", GLUTES, LEGS),
        SeedExercise("Cadeira abdutora", GLUTES, LEGS),
        SeedExercise("Agachamento sumô", GLUTES, LEGS),
        SeedExercise("Cadeira adutora", MuscleGroup.OTHER, LEGS),
        // Panturrilha
        SeedExercise("Panturrilha em pé", CALVES, LEGS),
        SeedExercise("Panturrilha sentado", CALVES, LEGS),
        SeedExercise("Panturrilha no leg press", CALVES, LEGS),
        // Corpo inteiro
        SeedExercise("Burpee", FULL_BODY, OTHER),
        SeedExercise("Kettlebell swing", FULL_BODY, OTHER),
        SeedExercise("Caminhada do fazendeiro", FULL_BODY, OTHER),
        // Cardio
        SeedExercise("Esteira", CARDIO, OTHER),
        SeedExercise("Bicicleta ergométrica", CARDIO, OTHER),
        SeedExercise("Elíptico", CARDIO, OTHER),
        SeedExercise("Remo ergômetro", CARDIO, OTHER),
        SeedExercise("Pular corda", CARDIO, OTHER),
        SeedExercise("Escada", CARDIO, OTHER),
    )

    /** Room callback that inserts [items] when the database file is first created. */
    class Callback(private val nowMs: () -> Long = System::currentTimeMillis) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val now = nowMs()
            db.beginTransaction()
            try {
                items.forEach { item ->
                    val values = ContentValues().apply {
                        put("name", item.name)
                        put("muscleGroup", item.muscleGroup.name)
                        put("movementType", item.movementType.name)
                        put("isCustom", 0)
                        put("archived", 0)
                        put("createdAtMs", now)
                    }
                    db.insert("exercise", SQLiteDatabase.CONFLICT_ABORT, values)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
