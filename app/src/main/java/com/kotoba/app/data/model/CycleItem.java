package com.kotoba.app.data.model;

import java.io.Serializable;

public class CycleItem implements Serializable {
    public enum Response {
        PENDING,
        INGAT,
        LUPA
    }

    public enum PoolType {
        NORMAL,
        RECOVERY
    }

    private final String cycleId;
    private final String objectId;
    private final PoolType poolType;
    private final int cycleIteration;
    private Response response;
    private int responseTimeMs;
    private int attemptCount;

    public CycleItem(String cycleId, String objectId, PoolType poolType, int cycleIteration) {
        this(cycleId, objectId, poolType, cycleIteration, Response.PENDING, 0, 0);
    }

    public CycleItem(String cycleId, String objectId, PoolType poolType, int cycleIteration,
                     Response response, int responseTimeMs, int attemptCount) {
        this.cycleId = cycleId;
        this.objectId = objectId;
        this.poolType = poolType;
        this.cycleIteration = cycleIteration;
        this.response = response != null ? response : Response.PENDING;
        this.responseTimeMs = responseTimeMs;
        this.attemptCount = attemptCount;
    }

    public String getCycleId() { return cycleId; }
    public String getObjectId() { return objectId; }
    public PoolType getPoolType() { return poolType; }
    public int getCycleIteration() { return cycleIteration; }
    public Response getResponse() { return response; }
    public void setResponse(Response response) { this.response = response; }
    public int getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(int responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public int getAttemptCount() { return attemptCount; }
    public void incrementAttemptCount() { this.attemptCount++; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
}
