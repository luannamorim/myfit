package io.github.luannamorim.myfit.domain.reminder

import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Reminder

/**
 * Links free-text activity reminders (before 1.18.0, or from an old backup) to a sport: the one whose name
 * [match]es the label, else [Sport.OTHER] with the label as its name. Routine and linked reminders are kept.
 */
fun linkSports(reminders: List<Reminder>, match: (String) -> Sport?): List<Reminder> = reminders.map { r ->
    if (r.routineId == null && r.sport == null) r.copy(sport = match(r.label) ?: Sport.OTHER) else r
}
