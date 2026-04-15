/**
 * Legal / IP protection module (Wave 4 Sub-PR 4.3, GAP-042).
 *
 * <p>Two-track workflow per ADR-012:
 * <ul>
 *   <li><b>Track 1 (proactive)</b> — {@link com.kiteclass.core.module.legal.service.TrademarkCheckService}
 *       scaffold; meant to be wired into {@code GenerateLogoStep} (Wave 3 Sub-PR 3.5) once the
 *       async pipeline is live.</li>
 *   <li><b>Track 2 (reactive)</b> — {@link com.kiteclass.core.module.legal.service.DmcaService}
 *       DMCA workflow with state machine + shared audit log.</li>
 * </ul>
 *
 * @since 3.24.0
 */
package com.kiteclass.core.module.legal;
