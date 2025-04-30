package com.ajaxjs.mcp.server.feature;

import org.junit.jupiter.api.Test;

class TestFeatureMgr {
    @Test
    void testInit() {
        FeatureMgr mgr = new FeatureMgr();
        mgr.init("com.ajaxjs.mcp.server.testcase");
    }
}
