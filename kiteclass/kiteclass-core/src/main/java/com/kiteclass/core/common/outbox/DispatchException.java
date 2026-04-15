package com.kiteclass.core.common.outbox;

/**
 * Checked exception — transient dispatch failure that should be retried.
 *
 * @since 3.17.0
 */
public class DispatchException extends Exception {

    public DispatchException(String message) {
        super(message);
    }

    public DispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
