#!/bin/bash
# Create kiteclass_shared database for KiteClass Core (shared multi-tenant)
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE kiteclass_shared OWNER $POSTGRES_USER'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'kiteclass_shared')\gexec
EOSQL

echo "kiteclass_shared database ready"
