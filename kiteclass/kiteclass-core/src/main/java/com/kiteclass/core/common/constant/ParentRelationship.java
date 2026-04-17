package com.kiteclass.core.common.constant;

/**
 * Relationship of a parent/guardian to their linked student.
 *
 * <p>Used by {@link com.kiteclass.core.module.parent.entity.Parent#relationship}.
 * Values cover the common Vietnamese family roles plus a generic GUARDIAN catch-all
 * for cases where the parent is not a biological parent (e.g., grandparent, foster).
 *
 * @author KiteClass Team
 * @since 2.14.0 (Wave 2 — GAP-052a)
 */
public enum ParentRelationship {
    /** Biological or adoptive father. */
    FATHER,
    /** Biological or adoptive mother. */
    MOTHER,
    /** Generic guardian (grandparent, relative, foster parent, etc.). */
    GUARDIAN
}
