/**
 * School MIS (Management Information System) integration — neutral roster
 * import surface that adapts to vendor-specific systems (VNEDU, SMAS, Base.vn,
 * Microsoft School Data Sync, Google Classroom, OneRoster CSV).
 *
 * <p>Design: Adapter pattern (one implementation of {@link
 * com.kiteclass.core.integration.mis.MisRosterSource} per vendor) wrapped
 * behind a shared neutral DTO ({@link com.kiteclass.core.integration.mis.RosterImport}).
 * Vendor SDK types never leak out of the {@code adapters/} subpackage — enforces
 * the leaky-abstraction rule in {@code .claude/rules/design-patterns.md} §3.10.
 *
 * <p>Phase 1 (wave 9 — GAP-200) ships only the interface, DTO, and a
 * {@code VneduAdapter} skeleton. Phase 2 will wire live VNEDU API calls, add
 * SMAS / Base.vn / OneRoster adapters, an orchestration service, and a
 * conflict-resolution UI.
 *
 * @see <a href="../../../../../../../../documents/02-architecture/adr/ADR-017-mis-sync-strategy.md">ADR-017</a>
 * @since 2.20.0 (Phase 1, 2026-04-21)
 */
package com.kiteclass.core.integration.mis;
