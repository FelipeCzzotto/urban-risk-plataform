package br.com.urbanrisk.application;

import br.com.urbanrisk.application.port.RiskEventRepository;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;

public class GetDashboardSummary {

    private final RiskEventRepository repository;
    private final Clock clock;

    public GetDashboardSummary(RiskEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public DashboardSummary execute() {
        Map<EventType, Long> byType = zeroFilled(EventType.class, repository.countActiveByType());
        Map<PriorityLevel, Long> byLevel = zeroFilled(PriorityLevel.class, repository.countActiveByLevel());
        long total = byType.values().stream().mapToLong(Long::longValue).sum();
        return new DashboardSummary(total, byType, byLevel, repository.lastUpdatedAt().orElse(clock.instant()));
    }

    private static <E extends Enum<E>> Map<E, Long> zeroFilled(Class<E> type, Map<E, Long> counts) {
        Map<E, Long> result = new EnumMap<>(type);
        for (E key : type.getEnumConstants()) {
            result.put(key, counts.getOrDefault(key, 0L));
        }
        return result;
    }
}
