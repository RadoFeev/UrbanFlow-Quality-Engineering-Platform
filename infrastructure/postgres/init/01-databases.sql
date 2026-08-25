-- The official PostgreSQL image creates POSTGRES_DB (urbanflow) before
-- running files in /docker-entrypoint-initdb.d.
CREATE DATABASE urbanflow_tracking;
CREATE DATABASE urbanflow_identity;
CREATE DATABASE urbanflow_routing;
CREATE DATABASE urbanflow_notification;
CREATE DATABASE urbanflow_warehouse;
CREATE DATABASE pact_broker;
