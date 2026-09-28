-- Tablerista: tablas y funciones para publicar tableros con QR.
-- Pegalo entero en Supabase → SQL Editor → New query → Run.

create extension if not exists pgcrypto with schema extensions;

create table if not exists public.tableros (
  id          text primary key check (id ~ '^[a-z0-9]{6,16}$'),
  name        text not null default 'Tablero',
  data        jsonb not null,
  key_hash    text not null,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

alter table public.tableros enable row level security;

-- Cualquiera puede VER un tablero publicado (es lo que abre el QR), pero no la clave.
drop policy if exists "ver tableros publicados" on public.tableros;
create policy "ver tableros publicados" on public.tableros for select using (true);
revoke all on public.tableros from anon, authenticated;
grant select (id, name, data, updated_at) on public.tableros to anon, authenticated;

-- Publicar o actualizar: solo con la clave que generó el equipo que lo publicó primero.
create or replace function public.publish_board(p_id text, p_key text, p_name text, p_data jsonb)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  h   text := encode(extensions.digest(p_key, 'sha256'), 'hex');
  cur text;
begin
  if p_id !~ '^[a-z0-9]{6,16}$' or length(coalesce(p_key, '')) < 24 then
    raise exception 'datos invalidos';
  end if;
  if octet_length(p_data::text) > 1000000 then
    raise exception 'tablero demasiado grande';
  end if;
  select key_hash into cur from tableros where id = p_id;
  if cur is null then
    insert into tableros (id, name, data, key_hash)
    values (p_id, left(coalesce(p_name, 'Tablero'), 120), p_data, h);
  elsif cur = h then
    update tableros set name = left(coalesce(p_name, 'Tablero'), 120), data = p_data, updated_at = now()
    where id = p_id;
  else
    raise exception 'sin permiso';
  end if;
end;
$$;

-- Quitar un tablero publicado (el QR deja de mostrarlo).
create or replace function public.delete_board(p_id text, p_key text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from tableros
  where id = p_id and key_hash = encode(extensions.digest(p_key, 'sha256'), 'hex');
  if not found then raise exception 'sin permiso'; end if;
end;
$$;

revoke all on function public.publish_board(text, text, text, jsonb) from public;
revoke all on function public.delete_board(text, text) from public;
grant execute on function public.publish_board(text, text, text, jsonb) to anon, authenticated;
grant execute on function public.delete_board(text, text) to anon, authenticated;
