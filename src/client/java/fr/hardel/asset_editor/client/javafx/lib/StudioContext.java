package fr.hardel.asset_editor.client.javafx.context;

import fr.hardel.asset_editor.client.javafx.data.mock.StudioMockRepository;
import fr.hardel.asset_editor.client.javafx.routes.StudioRouter;
import fr.hardel.asset_editor.client.javafx.store.StudioTabsState;
import fr.hardel.asset_editor.client.javafx.store.StudioUiState;

public final class StudioContext {

    private final StudioRouter router = new StudioRouter();
    private final StudioUiState uiState = new StudioUiState();
    private final StudioTabsState tabsState = new StudioTabsState();
    private final StudioMockRepository repository = new StudioMockRepository();

    public StudioRouter router() {
        return router;
    }

    public StudioUiState uiState() {
        return uiState;
    }

    public StudioTabsState tabsState() {
        return tabsState;
    }

    public StudioMockRepository repository() {
        return repository;
    }
}


