package com.kiteclass.core.common.constant;

/**
 * Type of link between a parent and a student.
 *
 * <p>Used to distinguish the principal contact ({@link #PRIMARY}) from additional
 * contacts ({@link #SECONDARY}) when a student has multiple linked parents.
 * Notifications that go to "one parent only" target PRIMARY by default.
 *
 * @author KiteClass Team
 * @since 2.14.0 (Wave 2 — GAP-052a)
 */
public enum ParentLinkType {
    PRIMARY,
    SECONDARY
}
