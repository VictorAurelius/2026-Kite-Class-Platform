-- ============================================================================
-- Dev-only seed: KC-8 Parent portal walk fixture (GAP-1457)
-- ============================================================================
-- Seeds 1 PARENT (loginable via /api/v1/tenant-auth/login) linked to a real
-- student in the TRIAL instance sky-education-074901, with PDPL consent granted
-- for all 5 facets so the parent portal renders real data end-to-end.
--
-- DEV-ONLY — never run against production. Idempotent: re-running replaces the
-- seeded parent + credential + link cleanly. Run against `kiteclass_shared`:
--   docker exec -e PGPASSWORD=<pw> kite-postgres \
--     psql -U kitehub -d kiteclass_shared \
--     -f /path/to/dev-seed-parent-kc8.sql
--
-- Credential: hong.tran+074901@gmail.com / Parent@123  (bcrypt $2a$10$)
-- Linked child: student id 167 (Phạm Thị Mai), instance 5b3ef1ae-…0
-- ============================================================================

DO $$
DECLARE
    v_instance  UUID  := '5b3ef1ae-39e7-4088-888f-941fca67f410';  -- sky-education-074901
    v_email     TEXT  := 'hong.tran+074901@gmail.com';
    v_student   BIGINT := 167;                                     -- Phạm Thị Mai
    -- bcrypt('Parent@123', cost 10) — Spring BCryptPasswordEncoder compatible
    v_pwhash    TEXT  := '$2a$10$RCvaUFArMVs1BP2bp/MmfOJq49qKOickjj5/OdYIQxDfL.FSdF7Di';
    v_parent_id BIGINT;
BEGIN
    -- Idempotent cleanup of any prior seed for this email -------------------
    DELETE FROM parent_student_links psl
     USING parents p
     WHERE psl.parent_id = p.id AND p.email = v_email AND p.instance_id = v_instance;
    DELETE FROM auth_credentials WHERE email = v_email AND entity_type = 'PARENT';
    DELETE FROM parents WHERE email = v_email AND instance_id = v_instance;

    -- 1. Parent row --------------------------------------------------------
    INSERT INTO parents (instance_id, email, phone_number, full_name, relationship, status)
    VALUES (v_instance, v_email, '0901234567', 'Trần Thị Hồng', 'MOTHER', 'ACTIVE')
    RETURNING id INTO v_parent_id;

    -- 2. Auth credential (PARENT, entity_id = parents.id → JWT referenceId) -
    INSERT INTO auth_credentials (user_uuid, entity_type, entity_id, email, password_hash, instance_id, enabled)
    VALUES (gen_random_uuid(), 'PARENT', v_parent_id, v_email, v_pwhash, v_instance, TRUE);

    -- 3. Parent ↔ student link + PDPL consent granted (all 5 facets) --------
    INSERT INTO parent_student_links (instance_id, parent_id, student_id, link_type, parental_consent)
    VALUES (
        v_instance, v_parent_id, v_student, 'PRIMARY',
        jsonb_build_object(
            'fields', jsonb_build_object(
                'fees', true, 'attendance', true, 'conduct', true,
                'transcript', true, 'notifications', true),
            'version', 1,
            'updatedAt', to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
        )::jsonb);

    RAISE NOTICE 'Seeded PARENT id=% email=% linked to student=% in instance=%',
        v_parent_id, v_email, v_student, v_instance;
END $$;

-- Verification --------------------------------------------------------------
SELECT 'parent'  AS kind, id::text, full_name AS detail FROM parents WHERE email = 'hong.tran+074901@gmail.com'
UNION ALL
SELECT 'cred',  entity_id::text, entity_type FROM auth_credentials WHERE email = 'hong.tran+074901@gmail.com'
UNION ALL
SELECT 'link',  student_id::text, parental_consent->>'version' FROM parent_student_links psl
  JOIN parents p ON p.id = psl.parent_id WHERE p.email = 'hong.tran+074901@gmail.com';
