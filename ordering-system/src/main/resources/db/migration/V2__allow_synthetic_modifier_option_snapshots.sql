-- Allow order items to persist synthetic modifier snapshots without a real modifier_options row.
-- This keeps staff/order dashboard rendering working when modifiers are generated from product metadata.

alter table order_item_modifier_options
    drop constraint if exists order_item_modifier_options_pkey;

alter table order_item_modifier_options
    drop constraint if exists order_item_modifier_options_modifier_option_id_fkey;

alter table order_item_modifier_options
    alter column modifier_option_id drop not null;

alter table order_item_modifier_options
    add column if not exists id bigserial;

create index if not exists idx_order_item_modifier_options_order_item
    on order_item_modifier_options(order_item_id);

create or replace function fn_check_order_item_modifier_option()
returns trigger
language plpgsql
as $$
declare
    v_order_tenant bigint;
    v_option_tenant bigint;
    v_group_name varchar(150);
    v_option_name varchar(150);
    v_price_delta numeric(10,2);
begin
    select o.tenant_id
      into v_order_tenant
    from orders o
    join order_items oi on oi.order_id = o.id
    where oi.id = new.order_item_id;

    if new.modifier_option_id is null then
        -- Synthetic snapshot: keep the values provided by the application.
        if tg_op = 'INSERT' then
            if new.modifier_group_name_snapshot is null or btrim(new.modifier_group_name_snapshot) = '' then
                raise exception 'modifier_group_name_snapshot mancante per snapshot sintetico';
            end if;
            if new.option_name_snapshot is null or btrim(new.option_name_snapshot) = '' then
                raise exception 'option_name_snapshot mancante per snapshot sintetico';
            end if;
            if new.price_delta_snapshot is null then
                new.price_delta_snapshot := 0;
            end if;
        end if;
        return new;
    end if;

    select mo.tenant_id, mg.name, mo.name, mo.price_delta
      into v_option_tenant, v_group_name, v_option_name, v_price_delta
    from modifier_options mo
    join modifier_groups mg on mg.id = mo.modifier_group_id
    where mo.id = new.modifier_option_id;

    if v_option_tenant is null then
        raise exception 'Modifier option % inesistente', new.modifier_option_id;
    end if;

    if v_order_tenant <> v_option_tenant then
        raise exception 'Modifier option e order item appartengono a tenant diversi';
    end if;

    if tg_op = 'INSERT' then
        new.modifier_group_name_snapshot := v_group_name;
        new.option_name_snapshot := v_option_name;
        new.price_delta_snapshot := v_price_delta;
    end if;

    return new;
end;
$$;

create or replace function fn_order_totals_trigger()
returns trigger
language plpgsql
as $$
declare
    v_order_id bigint;
begin
    if tg_table_name = 'order_item_modifier_options' then
        select oi.order_id
          into v_order_id
        from order_items oi
        where oi.id = coalesce(new.order_item_id, old.order_item_id);
    else
        v_order_id := coalesce(new.order_id, old.order_id);
    end if;

    if v_order_id is not null then
        perform fn_recalculate_order_totals(v_order_id);
    end if;

    return null;
end;
$$;