package com.kiteclass.core.module.instance.approval;

/**
 * Thrown when a rebrand trigger conflicts with another in-flight or stale request
 * (per GAP-070). Controllers should translate to HTTP 409.
 *
 * @since 3.21.0
 */
public class ConcurrentRebrandException extends RuntimeException {

    public ConcurrentRebrandException(String message) {
        super(message);
    }
}
