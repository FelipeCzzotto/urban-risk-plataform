package br.com.urbanrisk.application;

import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;

/** Null field = no filter. */
public record EventFilter(EventType type, PriorityLevel level, EventStatus status) {
}
