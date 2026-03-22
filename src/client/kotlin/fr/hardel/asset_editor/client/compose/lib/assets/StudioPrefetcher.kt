package fr.hardel.asset_editor.client.compose.lib.assets

import fr.hardel.asset_editor.client.navigation.StudioDestination

interface StudioPrefetcher {
    fun prefetch(destination: StudioDestination)
}
