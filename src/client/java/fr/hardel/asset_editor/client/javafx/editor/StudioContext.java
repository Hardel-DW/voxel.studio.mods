package fr.hardel.asset_editor.client.javafx.editor;

import fr.hardel.asset_editor.client.javafx.editor.model.StudioMockRepository;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioRouter;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioTabsState;
import fr.hardel.asset_editor.client.javafx.editor.state.StudioUiState;

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
