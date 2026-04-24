/**
 * Document generation foundation — Wave 5 GAP-047.
 *
 * <p>Facade ({@link com.kiteclass.core.module.document.DocumentGenerationService}) fronts a set of
 * {@link com.kiteclass.core.module.document.Generator} strategies, one per
 * {@link com.kiteclass.core.module.document.DocumentFormat}. Concrete generators (PDF, XLSX, DOCX)
 * ship in Sub-PRs 5.1–5.3.
 *
 * @see documents/02-architecture/adr/ADR-019-document-generation-architecture.md
 * @see documents/03-planning/waves/wave-05-document-generation.md
 */
package com.kiteclass.core.module.document;
