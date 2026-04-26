package com.kitehub.admin.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * In-process Spring event signalling that subscription/instance domain data
 * has changed and admin caches should be evicted.
 *
 * <p>Closes part of <strong>GAP-126</strong> — admin dashboard cache invalidation.
 * The longer-term shape is a RabbitMQ Outbox listener for {@code instance.*} /
 * {@code subscription.*} routing keys; until {@code kitehub-admin} pulls in
 * {@code spring-boot-starter-amqp} (out of scope for this PR) we route eviction
 * via Spring application events. Producers in the same JVM (e.g. mutation
 * endpoints in {@link com.kitehub.admin.controller.AdminController}) can publish
 * this event after a successful write; cross-service producers will be wired in
 * a follow-up gap.</p>
 *
 * @since Wave 7-Perf (GAP-126)
 */
public class SubscriptionDataChangedEvent extends ApplicationEvent {

    /**
     * Routing-key style descriptor of the underlying change, e.g.
     * {@code subscription.created}, {@code instance.suspended}.
     */
    private final String changeType;

    /**
     * Aggregate id (instanceId / subscriptionId) — informational, may be null
     * when the change is bulk (e.g. import).
     */
    private final UUID aggregateId;

    public SubscriptionDataChangedEvent(Object source, String changeType, UUID aggregateId) {
        super(source);
        this.changeType = changeType;
        this.aggregateId = aggregateId;
    }

    public String getChangeType() {
        return changeType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }
}
