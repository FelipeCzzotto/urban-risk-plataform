package br.com.urbanrisk.application.port;

public interface ReporterHasher {

    /** Irreversible digest of a normalized contact; raw value must never be stored. */
    String hash(String normalizedContact);
}
