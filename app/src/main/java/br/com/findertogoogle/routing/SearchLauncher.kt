package br.com.findertogoogle.routing

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

fun interface ActivityStarter { fun start(intent: Intent) }
enum class SearchFailure { UNAVAILABLE, ACCESS_DENIED, INVALID_REQUEST }
sealed interface SearchLaunchResult {
    data object Launched : SearchLaunchResult
    data class Failed(val reason: SearchFailure) : SearchLaunchResult
}
fun interface SearchOpener { fun open(query: String): SearchLaunchResult }

class SearchLauncher(private val starter: ActivityStarter) : SearchOpener {
    constructor(context: Context) : this(ActivityStarter { context.startActivity(it) })
    override fun open(query: String): SearchLaunchResult = try {
        starter.start(Intent(Intent.ACTION_WEB_SEARCH).apply {
            setPackage(GOOGLE_PACKAGE)
            putExtra(SearchManager.QUERY, query)
        })
        SearchLaunchResult.Launched
    } catch (_: ActivityNotFoundException) { SearchLaunchResult.Failed(SearchFailure.UNAVAILABLE)
    } catch (_: SecurityException) { SearchLaunchResult.Failed(SearchFailure.ACCESS_DENIED)
    } catch (_: IllegalArgumentException) { SearchLaunchResult.Failed(SearchFailure.INVALID_REQUEST) }
    companion object { const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox" }
}
