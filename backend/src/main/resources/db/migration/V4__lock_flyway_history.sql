-- Supabase grants anon/authenticated on every new public table; flyway's history table must not be
-- readable/writable through PostgREST (schema recon / tampering). Backend role bypasses RLS.
alter table flyway_schema_history enable row level security;

do $$
begin
    if exists (select 1 from pg_roles where rolname = 'anon') then
        execute 'revoke all on table flyway_schema_history from anon';
    end if;
    if exists (select 1 from pg_roles where rolname = 'authenticated') then
        execute 'revoke all on table flyway_schema_history from authenticated';
    end if;
end
$$;
