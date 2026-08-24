CREATE TABLE warehouses (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    capacity INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_warehouses_capacity CHECK (capacity BETWEEN 1 AND 1000000),
    CONSTRAINT ck_warehouses_version CHECK (version >= 0)
);

CREATE INDEX idx_warehouses_search ON warehouses (city, name, code);

CREATE TABLE logistics_hubs (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    name VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_logistics_hubs_warehouse_name UNIQUE (warehouse_id, name),
    CONSTRAINT ck_logistics_hubs_version CHECK (version >= 0)
);

CREATE INDEX idx_logistics_hubs_warehouse ON logistics_hubs (warehouse_id, name);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    type VARCHAR(60) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_incidents_status CHECK (status IN ('OPEN', 'RESOLVED')),
    CONSTRAINT ck_incidents_resolution CHECK (
        (status = 'OPEN' AND resolved_at IS NULL)
        OR (status = 'RESOLVED' AND resolved_at >= created_at)
    ),
    CONSTRAINT ck_incidents_version CHECK (version >= 0)
);

CREATE INDEX idx_incidents_status_created ON incidents (status, created_at DESC);
CREATE INDEX idx_incidents_shipment ON incidents (shipment_id, created_at DESC);

CREATE TABLE feature_flags (
    id UUID PRIMARY KEY,
    flag_key VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(300) NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_feature_flags_version CHECK (version >= 0)
);

CREATE TABLE audit_records (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    details VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_records_entity ON audit_records (entity_id, occurred_at DESC);
CREATE INDEX idx_audit_records_actor ON audit_records (actor_id, occurred_at DESC);

REVOKE UPDATE, DELETE ON audit_records FROM PUBLIC;
