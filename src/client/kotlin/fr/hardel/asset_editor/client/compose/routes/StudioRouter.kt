package fr.hardel.asset_editor.client.compose.routes

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.hardel.asset_editor.permission.StudioPermissions

class StudioRouter {

    var currentRoute: StudioRoute by mutableStateOf(StudioRoute.NoPermission)
        private set

    val currentConcept: String by derivedStateOf { currentRoute.concept() }
    val isOverview: Boolean by derivedStateOf { currentRoute.isOverview() }

    var permissionSupplier: () -> StudioPermissions = { StudioPermissions.NONE }

    fun navigate(route: StudioRoute) {
        if (route == currentRoute) return

        if (route == StudioRoute.NoPermission) {
            currentRoute = route
            return
        }

        if (permissionSupplier().isNone) {
            redirectNoPermission()
            return
        }

        if (route == StudioRoute.Debug && !permissionSupplier().isAdmin) {
            redirectNoPermission()
            return
        }

        currentRoute = route
    }

    fun revalidate() {
        val saved = currentRoute
        currentRoute = StudioRoute.NoPermission
        navigate(saved)
    }

    private fun redirectNoPermission() {
        if (currentRoute != StudioRoute.NoPermission)
            currentRoute = StudioRoute.NoPermission
    }
}
