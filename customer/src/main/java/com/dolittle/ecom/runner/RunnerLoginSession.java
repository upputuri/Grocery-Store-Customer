package com.dolittle.ecom.runner;

import lombok.Data;

public @Data class RunnerLoginSession {
    private final String sessionId;
    private final Runner runner;

    public RunnerLoginSession(String sessionId, Runner runner)
    {
        this.sessionId = sessionId;
        this.runner = runner;
    }
}
