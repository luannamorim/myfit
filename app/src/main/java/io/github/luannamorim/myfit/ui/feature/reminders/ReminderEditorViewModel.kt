package io.github.luannamorim.myfit.ui.feature.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.prefs.ReminderRepository
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.reminder.ReminderSchedule
import io.github.luannamorim.myfit.platform.reminder.SportNames
import io.github.luannamorim.myfit.ui.navigation.ReminderEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class RoutineChoice(val id: Long, val name: String)

data class ReminderEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val routines: List<RoutineChoice> = emptyList(),
    /** Selected routine; null means "other activity" ([sport], with [activity] as the label). */
    val routineId: Long? = null,
    /** The other activity's sport (required there). */
    val sport: Sport? = null,
    /** The other activity's name: the sport's, or the one typed for [Sport.OTHER]. */
    val activity: String = "",
    val minuteOfDay: Int = 0,
    /** Repeat days; empty means one-off on [date]. */
    val days: Set<DayOfWeek> = emptySet(),
    val date: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    val sportError: Boolean = false,
    val activityError: Boolean = false,
    val pastError: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class ReminderEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminders: ReminderRepository,
    routines: RoutineRepository,
    private val time: TimeSource,
    private val sportNames: SportNames,
) : ViewModel() {
    private val id: Long? = savedStateHandle.toRoute<ReminderEditorRoute>().reminderId
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ReminderEditorUiState> = _state.asStateFlow()
    private var saving = false

    init {
        viewModelScope.launch {
            val choices = routines.observeOverviews().first().map { RoutineChoice(it.id, it.name) }
            val existing = id?.let { reminders.get(it) }
            _state.update { s ->
                if (existing == null) {
                    s.copy(loading = false, routines = choices, routineId = choices.firstOrNull()?.id)
                } else {
                    // A deleted routine falls back to "other activity" with its last known name.
                    val routineId = existing.routineId?.takeIf { rid -> choices.any { it.id == rid } }
                    s.copy(
                        loading = false,
                        routines = choices,
                        routineId = routineId,
                        sport = if (routineId == null) existing.sport ?: Sport.OTHER else null,
                        activity = if (routineId == null) existing.label else "",
                        minuteOfDay = existing.minuteOfDay,
                        days = existing.days,
                        date = existing.dateEpochDay?.let(LocalDate::ofEpochDay) ?: s.date,
                    )
                }
            }
        }
    }

    fun selectRoutine(routineId: Long?) = _state.update { it.copy(routineId = routineId, sportError = false, activityError = false) }

    /** Picks the other activity's sport; [label] is its name in the current language (the free sport keeps a typed name). */
    fun selectSport(sport: Sport, label: String) = _state.update { s ->
        val activity = when {
            sport != Sport.OTHER -> label
            s.sport == Sport.OTHER -> s.activity
            else -> ""
        }
        s.copy(sport = sport, activity = activity, sportError = false, activityError = false)
    }

    fun setActivity(text: String) = _state.update { it.copy(activity = text, activityError = false) }

    fun setTime(minuteOfDay: Int) = _state.update { it.copy(minuteOfDay = minuteOfDay, pastError = false) }

    fun toggleDay(day: DayOfWeek) = _state.update { s ->
        s.copy(days = if (day in s.days) s.days - day else s.days + day, pastError = false)
    }

    fun setDate(date: LocalDate) = _state.update { it.copy(date = date, pastError = false) }

    /** Flags a missing sport, a free activity without a name, or a one-off moment that is not in the future. */
    fun validate(): Boolean {
        val s = _state.value
        if (s.loading) return false
        val sportError = s.routineId == null && s.sport == null
        val activityError = s.routineId == null && s.sport == Sport.OTHER && s.activity.isBlank()
        val pastError = s.days.isEmpty() && !ZonedDateTime.of(s.date, ReminderSchedule.localTime(s.minuteOfDay), time.zone()).isAfter(now())
        _state.update { it.copy(sportError = sportError, activityError = activityError, pastError = pastError) }
        return !sportError && !activityError && !pastError
    }

    /** Saves (and turns on) the reminder when valid. */
    fun save() {
        if (saving || !validate()) return
        saving = true
        val s = _state.value
        val label = s.routines.firstOrNull { it.id == s.routineId }?.name ?: s.activity.trim()
        val reminder = Reminder(
            id = id ?: 0L,
            routineId = s.routineId,
            label = label,
            // A free name that is a sport ("Corrida") becomes that sport, so the runs logged count for it.
            sport = if (s.routineId == null) s.sport?.takeIf { it != Sport.OTHER } ?: sportNames.match(label) ?: Sport.OTHER else null,
            minuteOfDay = s.minuteOfDay,
            dateEpochDay = if (s.days.isEmpty()) s.date.toEpochDay() else null,
            days = s.days,
            enabled = true,
        )
        viewModelScope.launch {
            reminders.save(reminder)
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() {
        val existing = id ?: return
        viewModelScope.launch {
            reminders.delete(existing)
            _state.update { it.copy(done = true) }
        }
    }

    /** New reminders start at the next full hour, so a one-off is valid right away. */
    private fun initialState(): ReminderEditorUiState {
        val now = now()
        val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        return ReminderEditorUiState(isNew = id == null, minuteOfDay = nextHour.hour * 60, date = nextHour.toLocalDate(), today = now.toLocalDate())
    }

    private fun now(): ZonedDateTime = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone())
}
