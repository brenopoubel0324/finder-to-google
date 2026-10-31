package br.com.findertogoogle.ui

import org.junit.Assert.*
import org.junit.Test

class SettingsStateTest {
    private fun state(browser: String? = "Navegador", selected: Boolean = true, available: Boolean = true,
        held: Boolean = true, pending: String? = null) = SettingsSnapshot(browser, selected, available, held, false,
        pending, 2, 2, false, null)
    @Test fun `navegador ausente prevalece sobre papel e aprendizado`() {
        assertEquals(HomeAction.CHOOSE_BROWSER, state(null, false, pending = "teste").homeAction)
        assertEquals(HomeAction.REPLACE_BROWSER, state(null, true, pending = "teste").homeAction)
    }
    @Test fun `permissao real precede sessao e papel indisponivel orienta sistema`() {
        assertEquals(HomeAction.ACTIVATE, state(held = false, pending = "teste").homeAction)
        assertEquals(HomeAction.SYSTEM_SETTINGS, state(available = false, held = false).homeAction)
        assertEquals(HomeAction.CONTINUE_CALIBRATION, state(pending = "teste").homeAction)
        assertEquals(HomeAction.TEST_FINDER, state().homeAction)
    }
    @Test fun `guia impede avancar com requisito perdido sem forcar passos anteriores validos`() {
        assertEquals(SetupStep.BROWSER, state(null).validStep(SetupStep.FINDER))
        assertEquals(SetupStep.ANDROID, state(held = false).validStep(SetupStep.FINDER))
        assertEquals(SetupStep.BROWSER, state().validStep(SetupStep.BROWSER))
        assertEquals(SetupStep.FINDER, state().firstSetupStep)
        assertEquals(SetupStep.ANDROID, state(held = false).firstSetupStep)
    }
}
