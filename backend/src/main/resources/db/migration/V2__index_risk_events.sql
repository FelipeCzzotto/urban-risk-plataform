-- GET /events (status filter, ORDER BY priority_score desc, occurred_at desc)
create index risk_events_status_priority_idx
    on risk_events (status, priority_score desc, occurred_at desc);

-- GET /events?type=... / ?level=... ; level index also serves GET /alerts (ACTIVE + HIGH|CRITICAL)
create index risk_events_status_type_priority_idx
    on risk_events (status, type, priority_score desc, occurred_at desc);
create index risk_events_status_level_priority_idx
    on risk_events (status, priority_level, priority_score desc, occurred_at desc);

-- Corroboration/cluster lookup: same type within last 1h, lat/lng bbox prefilter (index-only).
-- ponytail: plain btree, no PostGIS; move to GiST/PostGIS if volume grows.
create index risk_events_type_occurred_idx
    on risk_events (type, occurred_at) include (latitude, longitude);
