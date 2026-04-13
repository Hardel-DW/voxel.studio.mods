package fr.hardel.asset_editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DevFlagsTest {

    @Test
    void disableSingleplayerAdminMustStayDisabledInBuilds() {
        assertFalse(DevFlags.DISABLE_SINGLEPLAYER_ADMIN);
    }

    @Test
    void clearComposeCacheMustStayDisabledInBuilds() {
        assertFalse(DevFlags.CLEAR_COMPOSE_CACHE);
    }
}
