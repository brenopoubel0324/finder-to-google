package br.com.findertogoogle.routing

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import java.util.UUID

sealed interface CalibrationCapture {
    data object Ignore : CalibrationCapture
    data object Unsupported : CalibrationCapture
    data class Candidate(val client: String) : CalibrationCapture
}

/** Sessão efêmera iniciada somente na UI. Sem serviço, timer, persistência ou extras de controle. */
class FinderCalibration(private val clock: () -> Long = { SystemClock.elapsedRealtime() }) {
    data class Pending(val phrase: String, val started: Long)
    private var pending: Pending? = null

    @Synchronized fun begin(phrase: String = newPhrase()): Pending {
        require(Regex("teste finder [a-f0-9]{8}").matches(phrase))
        return Pending(phrase, clock()).also { pending = it }
    }

    @Synchronized fun current(): Pending? {
        val value = pending ?: return null
        val age = clock() - value.started
        if (age < 0 || age >= VALIDITY_MS) { pending = null; return null }
        return value
    }

    @Synchronized fun cancel() { pending = null }

    @Synchronized fun inspect(action: String?, uri: Uri?, origin: LinkOrigin): CalibrationCapture {
        val value = current() ?: return CalibrationCapture.Ignore
        if (action != Intent.ACTION_VIEW || uri == null || origin.kind == OriginKind.OTHER ||
            GoogleSearchUrlParser.extractQuery(uri) != value.phrase) return CalibrationCapture.Ignore
        pending = null // Uma só captura; confirmar nunca acontece automaticamente.
        val client = GoogleSearchUrlParser.finderClient(uri) ?: return CalibrationCapture.Unsupported
        val uniqueClient = try { uri.getQueryParameters("client").size == 1 } catch (_: RuntimeException) { false }
        if (!uniqueClient) return CalibrationCapture.Unsupported
        return CalibrationCapture.Candidate(client)
    }

    companion object {
        const val VALIDITY_MS = 5 * 60 * 1000L
        fun newPhrase(): String = "teste finder " + UUID.randomUUID().toString().take(8)
        val process = FinderCalibration()
    }
}
