package io.github.luannamorim.myfit.testutil

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale

/** Sets the JVM default locale for plain (non-Robolectric) tests. */
class LocaleRule(private val locale: Locale = Locale.forLanguageTag("pt-BR")) : TestWatcher() {
    private var previous: Locale = Locale.getDefault()

    override fun starting(description: Description) {
        previous = Locale.getDefault()
        Locale.setDefault(locale)
    }

    override fun finished(description: Description) = Locale.setDefault(previous)
}
