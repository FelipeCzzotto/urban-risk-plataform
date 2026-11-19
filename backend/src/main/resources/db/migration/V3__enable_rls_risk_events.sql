-- Backend connects as postgres/service role (bypasses RLS). No policies = deny all for
-- PostgREST roles; revokes are belt-and-braces so reporter_hash is never exposed.
alter table risk_events enable row level security;

-- Guarded: anon/authenticated exist only on Supabase, not plain Postgres (Testcontainers).
do $$
begin
    if exists (select 1 from pg_roles where rolname = 'anon') then
        execute 'revoke all on table risk_events from anon';
    end if;
    if exists (select 1 from pg_roles where rolname = 'authenticated') then
        execute 'revoke all on table risk_events from authenticated';
    end if;
end
$$;
