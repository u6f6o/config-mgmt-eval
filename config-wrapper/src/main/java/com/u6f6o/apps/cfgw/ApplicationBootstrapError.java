package com.u6f6o.apps.cfgw;


public class ApplicationBootstrapError extends Error {

    public ApplicationBootstrapError(String message) {
        super(message);
    }

    public ApplicationBootstrapError(String message, Throwable cause) {
        super(message, cause);
    }
}
