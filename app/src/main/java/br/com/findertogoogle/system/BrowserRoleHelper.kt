package br.com.findertogoogle.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BrowserRoleHelper(context: Context) {
    private val roles = context.getSystemService(RoleManager::class.java)
    fun isAvailable(): Boolean = try {
        roles?.isRoleAvailable(RoleManager.ROLE_BROWSER) == true
    } catch (_: RuntimeException) { false }

    fun isHeld(): Boolean = try {
        roles?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
    } catch (_: RuntimeException) { false }

    fun requestIntent(): Intent? = try {
        if (isAvailable()) roles?.createRequestRoleIntent(RoleManager.ROLE_BROWSER) else null
    } catch (_: RuntimeException) { null }

    fun defaultAppsIntent() = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
}
