package io.github.luannamorim.myfit.ui.feature.activity

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.images.ImageKind
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.DistanceUnit
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.ui.navigation.ActivityEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

data class ActivityEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val sport: Sport = Sport.entries.first(),
    val name: String = "",
    val date: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    /** Minutes, as typed. */
    val duration: String = "",
    /** In the sport's unit (km, or m for swimming), as typed. */
    val distance: String = "",
    val intensity: Intensity? = null,
    val notes: String = "",
    val imagePath: String? = null,
    val nameError: Boolean = false,
    val durationError: Boolean = false,
    val distanceError: Boolean = false,
    val photoError: Boolean = false,
    val done: Boolean = false,
)

/** Logs a sport after the fact (new, or editing one from history). */
@HiltViewModel
class ActivityEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activities: ActivityRepository,
    private val images: ImageStore,
    time: TimeSource,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ActivityEditorRoute>()
    private val today = time.today()
    private val _state = MutableStateFlow(
        ActivityEditorUiState(date = route.dateEpochDay?.let(LocalDate::ofEpochDay)?.coerceAtMost(today) ?: today, today = today),
    )
    val state: StateFlow<ActivityEditorUiState> = _state.asStateFlow()

    /** The photo saved in the database, deleted only once a different one is saved. */
    private var savedImagePath: String? = null

    init {
        viewModelScope.launch {
            val existing = route.activityId.takeIf { it != 0L }?.let { activities.get(it) }
            savedImagePath = existing?.imagePath
            _state.update { s ->
                if (existing == null) {
                    s.copy(loading = false)
                } else {
                    s.copy(
                        loading = false,
                        isNew = false,
                        sport = existing.sport,
                        name = existing.name.orEmpty(),
                        date = existing.date,
                        duration = existing.durationMinutes.toString(),
                        distance = formatDistance(existing.sport, existing.distanceKm),
                        intensity = existing.intensity,
                        notes = existing.notes.orEmpty(),
                        imagePath = existing.imagePath,
                    )
                }
            }
        }
    }

    fun setSport(sport: Sport) = _state.update { s ->
        // Keep a typed distance only when the unit stays the same.
        val distance = if (s.sport.distance == sport.distance) s.distance else ""
        s.copy(sport = sport, distance = distance, nameError = false, distanceError = false)
    }

    fun setName(text: String) = _state.update { it.copy(name = text.take(MAX_NAME), nameError = false) }
    fun setDate(date: LocalDate) = _state.update { it.copy(date = date.coerceAtMost(today)) }
    fun setDuration(text: String) = _state.update { it.copy(duration = text.filter(Char::isDigit).take(4), durationError = false) }
    fun setDistance(text: String) = _state.update { it.copy(distance = text.take(8), distanceError = false) }
    fun setIntensity(value: Intensity?) = _state.update { it.copy(intensity = value) }
    fun setNotes(text: String) = _state.update { it.copy(notes = text) }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val path = runCatching { images.import(uri, ImageKind.SESSION) }.getOrNull()
            val previous = _state.value.imagePath
            _state.update { if (path == null) it.copy(photoError = true) else it.copy(imagePath = path, photoError = false) }
            // A photo picked in this editor and replaced before saving is not referenced anywhere.
            if (path != null && previous != savedImagePath) images.delete(previous)
        }
    }

    fun removePhoto() {
        val previous = _state.value.imagePath
        _state.update { it.copy(imagePath = null, photoError = false) }
        if (previous != savedImagePath) viewModelScope.launch { images.delete(previous) }
    }

    /** Validates and saves. Returns false (with errors flagged) when something is missing. */
    fun save(): Boolean {
        val s = _state.value
        val minutes = s.duration.toIntOrNull()?.takeIf { it in 1..MAX_MINUTES }
        val nameMissing = s.sport == Sport.OTHER && s.name.isBlank()
        val distanceKm = parseDistance(s.sport, s.distance)
        val distanceInvalid = s.distance.isNotBlank() && distanceKm == null
        if (minutes == null || nameMissing || distanceInvalid) {
            _state.update { it.copy(durationError = minutes == null, nameError = nameMissing, distanceError = distanceInvalid) }
            return false
        }
        viewModelScope.launch {
            activities.save(
                Activity(
                    id = route.activityId,
                    sport = s.sport,
                    name = s.name,
                    date = s.date,
                    durationMinutes = minutes,
                    distanceKm = distanceKm,
                    intensity = s.intensity,
                    notes = s.notes,
                    imagePath = s.imagePath,
                ),
            )
            if (savedImagePath != s.imagePath) images.delete(savedImagePath)
            _state.update { it.copy(done = true) }
        }
        return true
    }

    fun delete() {
        viewModelScope.launch {
            val stored = activities.delete(route.activityId)
            images.delete(stored)
            val current = _state.value.imagePath
            if (current != stored) images.delete(current)
            _state.update { it.copy(done = true) }
        }
    }

    companion object {
        private const val MAX_NAME = 40
        private const val MAX_MINUTES = 24 * 60

        fun formatDistance(sport: Sport, km: Double?): String = when {
            km == null -> ""
            sport.distance == DistanceUnit.METERS -> (km * 1000).roundToInt().toString()
            else -> DecimalInput.format(km)
        }

        /** Distance in km from the typed text (meters for swimming); null when blank or invalid. */
        fun parseDistance(sport: Sport, text: String): Double? {
            val t = text.trim()
            if (t.isEmpty() || sport.distance == null) return null
            return when (sport.distance) {
                DistanceUnit.METERS -> t.toIntOrNull()?.takeIf { it in 1..99_999 }?.let { it / 1000.0 }
                DistanceUnit.KM -> DecimalInput.parse(t)?.takeIf { it > 0 }
            }
        }
    }
}
