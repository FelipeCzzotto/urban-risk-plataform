-- RiskEvent (docs/CONTRACT.md). Enums as text + CHECK: easier to evolve than native enum types.
create table risk_events (
    id             uuid             primary key default gen_random_uuid(),
    type           text             not null check (type in ('FLOOD', 'HEAT_ISLAND', 'SERVICE_OUTAGE')),
    title          text             not null check (char_length(title) between 1 and 120),
    description    text             not null check (char_length(description) <= 1000),
    latitude       double precision not null check (latitude between -90 and 90),
    longitude      double precision not null check (longitude between -180 and 180),
    neighborhood   text             not null check (char_length(neighborhood) <= 80),
    severity       smallint         not null check (severity between 1 and 5),
    source         text             not null check (source in ('SENSOR', 'PUBLIC_DATA', 'COMMUNITY')),
    source_ref     text             not null check (char_length(source_ref) <= 120),
    confidence     double precision not null check (confidence between 0 and 1),
    priority_score double precision not null check (priority_score between 0 and 100),
    priority_level text             not null check (priority_level in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status         text             not null default 'ACTIVE' check (status in ('ACTIVE', 'RESOLVED')),
    -- SHA-256(pepper+contact) hex; never raw PII. Not exposed via API.
    reporter_hash  text             check (char_length(reporter_hash) <= 128),
    occurred_at    timestamptz      not null,
    created_at     timestamptz      not null default now()
);
