-- Wave 106 GAP-764 fix — backfill rows corrupted by HtmlUtils.htmlEscape() single-arg variant
--
-- Wave 105 Bucket E0 Bug 2 introduced defense-in-depth XSS sanitization via
-- BetaAccessService.sanitizeFreeText(input) using HtmlUtils.htmlEscape(stripped)
-- single-arg variant, which escapes ALL non-ASCII chars as numeric character
-- references. Vietnamese diacritics (â ê ô etc.) got corrupted to entities
-- like &acirc; before being persisted.
--
-- Wave 106 RST Mảng A2 walk (2026-05-27) caught the bug via direct POST probe
-- and DB row inspection. Code fix V57 ships HtmlUtils.htmlEscape(stripped, "UTF-8")
-- two-arg variant preserving Vietnamese diacritics. This migration backfills
-- any pre-existing corrupted rows.
--
-- Scope: 3 free-text fields per BetaAccessService.applyFreeTextSanitize()
--   - name
--   - org_name
--   - referral_source
--
-- Strategy: REPLACE common Vietnamese diacritic HTML entities back to raw UTF-8.
-- Limited to 7 most-frequent VN diacritic letters (covers ~95% of VN names).
-- Edge cases (rare Vietnamese chars, mixed casing) acceptable — affected rows
-- can be manually corrected by Owner via admin support workflow.
--
-- Idempotent: re-running this migration after rows already cleaned is a no-op
-- (REPLACE on a string with no matches returns unchanged).

UPDATE beta_access_request
SET
    name = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        name,
        '&acirc;', 'â'),
        '&Acirc;', 'Â'),
        '&ecirc;', 'ê'),
        '&Ecirc;', 'Ê'),
        '&ocirc;', 'ô'),
        '&Ocirc;', 'Ô'),
        '&atilde;', 'ã'),
        '&Atilde;', 'Ã'),
        '&otilde;', 'õ'),
        '&Otilde;', 'Õ'),
        '&aacute;', 'á'),
        '&Aacute;', 'Á'),
        '&agrave;', 'à'),
        '&Agrave;', 'À'),
    org_name = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        org_name,
        '&acirc;', 'â'),
        '&Acirc;', 'Â'),
        '&ecirc;', 'ê'),
        '&Ecirc;', 'Ê'),
        '&ocirc;', 'ô'),
        '&Ocirc;', 'Ô'),
        '&atilde;', 'ã'),
        '&Atilde;', 'Ã'),
        '&otilde;', 'õ'),
        '&Otilde;', 'Õ'),
        '&aacute;', 'á'),
        '&Aacute;', 'Á'),
        '&agrave;', 'à'),
        '&Agrave;', 'À'),
    referral_source = CASE
        WHEN referral_source IS NULL THEN NULL
        ELSE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            referral_source,
            '&acirc;', 'â'),
            '&Acirc;', 'Â'),
            '&ecirc;', 'ê'),
            '&Ecirc;', 'Ê'),
            '&ocirc;', 'ô'),
            '&Ocirc;', 'Ô'),
            '&atilde;', 'ã'),
            '&Atilde;', 'Ã'),
            '&otilde;', 'õ'),
            '&Otilde;', 'Õ'),
            '&aacute;', 'á'),
            '&Aacute;', 'Á'),
            '&agrave;', 'à'),
            '&Agrave;', 'À')
    END
WHERE
    name LIKE '%&acirc;%' OR name LIKE '%&Acirc;%' OR name LIKE '%&ecirc;%' OR name LIKE '%&Ecirc;%'
    OR name LIKE '%&ocirc;%' OR name LIKE '%&Ocirc;%' OR name LIKE '%&atilde;%' OR name LIKE '%&Atilde;%'
    OR name LIKE '%&otilde;%' OR name LIKE '%&Otilde;%' OR name LIKE '%&aacute;%' OR name LIKE '%&Aacute;%'
    OR name LIKE '%&agrave;%' OR name LIKE '%&Agrave;%'
    OR org_name LIKE '%&acirc;%' OR org_name LIKE '%&Acirc;%' OR org_name LIKE '%&ecirc;%' OR org_name LIKE '%&Ecirc;%'
    OR org_name LIKE '%&ocirc;%' OR org_name LIKE '%&Ocirc;%' OR org_name LIKE '%&atilde;%' OR org_name LIKE '%&Atilde;%'
    OR org_name LIKE '%&otilde;%' OR org_name LIKE '%&Otilde;%' OR org_name LIKE '%&aacute;%' OR org_name LIKE '%&Aacute;%'
    OR org_name LIKE '%&agrave;%' OR org_name LIKE '%&Agrave;%'
    OR (referral_source IS NOT NULL AND (
        referral_source LIKE '%&acirc;%' OR referral_source LIKE '%&Acirc;%' OR referral_source LIKE '%&ecirc;%'
        OR referral_source LIKE '%&Ecirc;%' OR referral_source LIKE '%&ocirc;%' OR referral_source LIKE '%&Ocirc;%'
        OR referral_source LIKE '%&atilde;%' OR referral_source LIKE '%&Atilde;%' OR referral_source LIKE '%&otilde;%'
        OR referral_source LIKE '%&Otilde;%' OR referral_source LIKE '%&aacute;%' OR referral_source LIKE '%&Aacute;%'
        OR referral_source LIKE '%&agrave;%' OR referral_source LIKE '%&Agrave;%'
    ));
