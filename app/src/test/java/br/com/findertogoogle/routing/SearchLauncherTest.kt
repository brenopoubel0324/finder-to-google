package br.com.findertogoogle.routing

import android.content.ActivityNotFoundException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchLauncherTest {
    @Test fun `falhas sao tipadas sem depender de estado anterior`() {
        val cases = listOf(ActivityNotFoundException("URL_CANARY") to SearchFailure.UNAVAILABLE,
            SecurityException("QUERY_CANARY") to SearchFailure.ACCESS_DENIED,
            IllegalArgumentException("URL_CANARY") to SearchFailure.INVALID_REQUEST)
        cases.forEach { (error, reason) ->
            var failing = true
            val launcher = SearchLauncher(ActivityStarter { if (failing) throw error })
            assertEquals(SearchLaunchResult.Failed(reason), launcher.open("QUERY_CANARY"))
            failing = false
            assertEquals(SearchLaunchResult.Launched, launcher.open("teste"))
        }
    }
}
