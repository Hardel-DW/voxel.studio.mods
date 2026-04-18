package fr.hardel.asset_editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DevFlagsTest {

    @Test
    void allDevFlagsMustStayDisabledInBuilds() {
        assertFalse(DevFlags.DISABLE_SINGLEPLAYER_ADMIN);
        assertFalse(DevFlags.CLEAR_COMPOSE_CACHE);
        assertFalse(DevFlags.SHOW_FPS_COUNTER);
        assertFalse(DevFlags.SHOW_HOVER_TRIANGLE);
        assertFalse(DevFlags.STAY_ON_SPLASH);
    }
}
