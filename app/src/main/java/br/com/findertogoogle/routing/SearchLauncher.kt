package br.com.findertogoogle.routing

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

fun interface ActivityStarter { fun start(intent: Intent) }
fun interface SearchOpener { fun open(query: String): Boolean }

class SearchLauncher(private val starter: ActivityStarter) : SearchOpener {
    constructor(context: Context) : this(ActivityStarter { context.startActivity(it) })

    override fun open(query: String): Boolean = try {
        // Não depender de resolveActivity: a tentativa real e as exceções são a autoridade.
        starter.start(Intent(Intent.ACTION_WEB_SEARCH).apply {
            setPackage(GOOGLE_PACKAGE)
            putExtra(SearchManager.QUERY, query)
        })
        true
    } catch (_: ActivityNotFoundException) { false
    } catch (_: SecurityException) { false
    } catch (_: IllegalArgumentException) { false }

    companion object { const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox" }
}
