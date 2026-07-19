package com.ajaxjs.mcp.server.feature;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestFeatureMgr {
    @Test
    void testInit() {
        FeatureMgr mgr = new FeatureMgr();
        mgr.init("com.ajaxjs.mcp.server.testcase");

        assertFalse(mgr.getToolStore().isEmpty());
        assertFalse(mgr.getResourceStore().isEmpty());
        assertFalse(mgr.getPromptStore().isEmpty());
    }

    @Test
    void featureStoresAreIsolatedPerManager() {
        FeatureMgr populated = new FeatureMgr();
        populated.init("com.ajaxjs.mcp.server.testcase");
        FeatureMgr empty = new FeatureMgr();

        assertFalse(populated.getToolStore().isEmpty());
        assertTrue(empty.getToolStore().isEmpty());
        assertTrue(empty.getResourceStore().isEmpty());
        assertTrue(empty.getPromptStore().isEmpty());
    }
}
