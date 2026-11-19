package br.com.urbanrisk.infrastructure.persistence;

import br.com.urbanrisk.application.EventFilter;
import br.com.urbanrisk.application.PageResult;
import br.com.urbanrisk.application.port.RiskEventRepository;
import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import br.com.urbanrisk.domain.RiskEvent;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class RiskEventRepositoryAdapter implements RiskEventRepository {

    private static final Sort PRIORITY_ORDER = Sort.by(Sort.Order.desc("priorityScore"), Sort.Order.desc("occurredAt"));

    private final RiskEventJpaRepository jpa;

    public RiskEventRepositoryAdapter(RiskEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public RiskEvent save(RiskEvent event) {
        return jpa.saveAndFlush(RiskEventEntity.fromDomain(event)).toDomain();
    }

    @Override
    public Optional<RiskEvent> findById(UUID id) {
        return jpa.findById(id).map(RiskEventEntity::toDomain);
    }

    @Override
    public List<RiskEvent> findSameTypeBetween(EventType type, Instant from, Instant to) {
        return jpa.findByTypeAndOccurredAtBetween(type, from, to).stream().map(RiskEventEntity::toDomain).toList();
    }

    @Override
    public PageResult<RiskEvent> search(EventFilter f, int page, int size) {
        Page<RiskEventEntity> p = jpa.search(f.type(), f.level(), f.status(), PageRequest.of(page, size, PRIORITY_ORDER));
        return new PageResult<>(p.getContent().stream().map(RiskEventEntity::toDomain).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Override
    public List<RiskEvent> findActiveAlerts(int limit) {
        return jpa.findByStatusAndPriorityLevelInOrderByPriorityScoreDescOccurredAtDesc(
                        EventStatus.ACTIVE, List.of(PriorityLevel.HIGH, PriorityLevel.CRITICAL), Limit.of(limit))
                .stream().map(RiskEventEntity::toDomain).toList();
    }

    @Override
    public Map<EventType, Long> countActiveByType() {
        Map<EventType, Long> m = new EnumMap<>(EventType.class);
        jpa.countByTypeForStatus(EventStatus.ACTIVE).forEach(r -> m.put((EventType) r[0], (Long) r[1]));
        return m;
    }

    @Override
    public Map<PriorityLevel, Long> countActiveByLevel() {
        Map<PriorityLevel, Long> m = new EnumMap<>(PriorityLevel.class);
        jpa.countByLevelForStatus(EventStatus.ACTIVE).forEach(r -> m.put((PriorityLevel) r[0], (Long) r[1]));
        return m;
    }

    /** ponytail: no updated_at column, so this is the latest insert; resolves don't bump it. Add updated_at if needed. */
    @Override
    public Optional<Instant> lastUpdatedAt() {
        return Optional.ofNullable(jpa.maxCreatedAt());
    }
}
