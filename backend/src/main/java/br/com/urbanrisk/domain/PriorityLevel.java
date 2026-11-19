package br.com.urbanrisk.domain;

public enum PriorityLevel {
    LOW, MEDIUM, HIGH, CRITICAL;

    public static PriorityLevel fromScore(double score) {
        if (score < 40) return LOW;
        if (score < 60) return MEDIUM;
        if (score < 80) return HIGH;
        return CRITICAL;
    }

    public boolean isAlert() {
        return this == HIGH || this == CRITICAL;
    }
}
