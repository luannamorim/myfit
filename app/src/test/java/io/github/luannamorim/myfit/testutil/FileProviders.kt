package io.github.luannamorim.myfit.testutil

import androidx.core.content.FileProvider

/**
 * FileProvider caches each authority's roots in a static map, resolved against the first test's
 * Robolectric data dir. Later tests get a new data dir, so getUriForFile fails with "Failed to find
 * configured root". Call this before any test that makes a FileProvider Uri.
 */
fun clearFileProviderCache() {
    val field = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
    synchronized(field.get(null)!!) { (field.get(null) as MutableMap<*, *>).clear() }
}
