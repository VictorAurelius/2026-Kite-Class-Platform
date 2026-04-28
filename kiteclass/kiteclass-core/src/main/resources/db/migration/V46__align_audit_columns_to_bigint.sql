-- GAP-244: Align created_by / updated_by audit columns to BIGINT to match BaseEntity.
--
-- BaseEntity (com.kiteclass.core.common.entity.BaseEntity) declares:
--     @CreatedBy private Long createdBy;
--     @LastModifiedBy private Long updatedBy;
--
-- AuditorAware<Long> in JpaConfig returns the numeric user ID from UserContext.
-- However, V28..V44 migrations declared these columns as VARCHAR(100) (or VARCHAR(255)
-- in V44), so a fresh Flyway-migrated DB fails Hibernate's `ddl-auto: validate` check:
--     wrong column type encountered in column [created_by]; found [varchar], but expecting [bigint]
--
-- Fix: ALTER each affected column to BIGINT. `USING NULLIF(col,'')::BIGINT` handles any
-- legacy VARCHAR-numeric values written by Spring's @CreatedBy (which always writes a Long
-- via Long.toString() coercion) and treats empty strings as NULL.
--
-- Idempotent via `ALTER TABLE IF EXISTS` + DO blocks that detect current type before
-- altering — re-running this migration on an already-aligned DB is a no-op.

DO $$
DECLARE
    target RECORD;
BEGIN
    FOR target IN
        SELECT t.table_name, c.column_name
        FROM information_schema.tables t
        JOIN information_schema.columns c
          ON c.table_schema = t.table_schema AND c.table_name = t.table_name
        WHERE t.table_schema = 'public'
          AND c.column_name IN ('created_by', 'updated_by')
          AND c.data_type IN ('character varying', 'varchar', 'text')
          AND t.table_name IN (
              'academic_years', 'semesters', 'holidays',
              'homeroom_classes', 'subject_sections', 'curricula', 'subject_grades',
              'permissions', 'roles', 'user_roles',
              'frontend_instances', 'branding_resources', 'outbox_events',
              'rebrand_approvals', 'audit_log', 'moderation_queue',
              'dmca_takedown_requests', 'quality_reports', 'class_schedule_slots'
          )
          -- role_permissions intentionally excluded — pure junction table, no audit columns
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE BIGINT USING NULLIF(%I, '''')::BIGINT',
            target.table_name, target.column_name, target.column_name
        );
    END LOOP;
END $$;
