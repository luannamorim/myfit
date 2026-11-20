package io.github.luannamorim.myfit.platform.reminder

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.ui.common.labelRes
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Sport names in both app languages, to recognize the sport in an old free-text reminder. */
@Singleton
class SportNames @Inject constructor(@ApplicationContext context: Context) {
    private val byName: Map<String, Sport> by lazy {
        LOCALES.flatMap { locale ->
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })
            Sport.entries.filter { it != Sport.OTHER }.map { key(localized.getString(it.labelRes)) to it }
        }.toMap()
    }

    /** The sport named [text] (Portuguese or English, case and spaces ignored); null for [Sport.OTHER]'s own name or any other text. */
    fun match(text: String): Sport? = byName[key(text)]

    private fun key(text: String) = text.trim().lowercase(Locale.ROOT)

    private companion object {
        val LOCALES = listOf(Locale.forLanguageTag("pt-BR"), Locale.ENGLISH)
    }
}
