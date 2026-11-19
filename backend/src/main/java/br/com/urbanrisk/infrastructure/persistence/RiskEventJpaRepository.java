package br.com.urbanrisk.infrastructure.persistence;

import br.com.urbanrisk.domain.EventStatus;
import br.com.urbanrisk.domain.EventType;
import br.com.urbanrisk.domain.PriorityLevel;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RiskEventJpaRepository extends JpaRepository<RiskEventEntity, UUID> {

    List<RiskEventEntity> findByTypeAndOccurredAtBetween(EventType type, Instant from, Instant to);

    @Query("""
            select e from RiskEventEntity e
            where (:type is null or e.type = :type)
              and (:level is null or e.priorityLevel = :level)
              and (:status is null or e.status = :status)""")
    Page<RiskEventEntity> search(EventType type, PriorityLevel level, EventStatus status, Pageable pageable);

    List<RiskEventEntity> findByStatusAndPriorityLevelInOrderByPriorityScoreDescOccurredAtDesc(
            EventStatus status, Collection<PriorityLevel> levels, Limit limit);

    @Query("select e.type, count(e) from RiskEventEntity e where e.status = :status group by e.type")
    List<Object[]> countByTypeForStatus(EventStatus status);

    @Query("select e.priorityLevel, count(e) from RiskEventEntity e where e.status = :status group by e.priorityLevel")
    List<Object[]> countByLevelForStatus(EventStatus status);

    @Query("select max(e.createdAt) from RiskEventEntity e")
    Instant maxCreatedAt();
}
