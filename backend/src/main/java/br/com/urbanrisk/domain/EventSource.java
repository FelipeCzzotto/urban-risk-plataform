package br.com.urbanrisk.domain;

public enum EventSource {
    SENSOR(0.9), PUBLIC_DATA(0.8), COMMUNITY(0.5);

    private final double baseConfidence;

    EventSource(double baseConfidence) {
        this.baseConfidence = baseConfidence;
    }

    public double baseConfidence() {
        return baseConfidence;
    }
}
