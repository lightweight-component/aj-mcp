package com.ajaxjs.mcp.server.model;

/**
 * Represents session state.
 */
public enum SessionState {
    /**
     * The session is allocated but has not initialized.
     */
    NEW,

    /**
     * The session has received initialization but is not ready yet.
     */
    INITIALIZING,

    /**
     * The session completed initialization and can handle requests.
     */
    READY
}