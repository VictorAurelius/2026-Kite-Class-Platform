package com.kiteclass.core.module.ai.workflow;

import com.kiteclass.core.common.outbox.OutboxEventWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlanExecutorTest {

    @Mock
    private OutboxEventWriter outbox;

    @InjectMocks
    private PlanExecutor executor;

    private StepContext ctx() {
        return new StepContext(42L, "t-1");
    }

    private Step namedStep(String name, Runnable body) {
        return new Step() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void execute(StepContext context) {
                body.run();
                context.put("ran:" + name, true);
            }
        };
    }

    @Test
    void happy_path_runs_all_steps_and_emits_lifecycle_events() {
        Plan plan = new Plan("desc", List.of(
                namedStep("a", () -> {}),
                namedStep("b", () -> {})));
        StepContext context = ctx();

        executor.execute(plan, context);

        assertThat(context.getExecutedSteps()).containsExactly("a", "b");
        verify(outbox).enqueue(eq("ai.plan.started"), anyString(), anyString(), anyString());
        verify(outbox, atLeastOnce()).enqueue(eq("ai.step.completed"), anyString(), anyString(), anyString());
        verify(outbox).enqueue(eq("ai.plan.completed"), anyString(), anyString(), anyString());
    }

    @Test
    void step_failure_without_fallback_emits_plan_failed_and_rethrows() {
        Step failing = new Step() {
            @Override
            public String name() {
                return "bad";
            }

            @Override
            public void execute(StepContext c) {
                throw new StepException("boom");
            }
        };

        assertThatThrownBy(() -> executor.execute(new Plan("x", List.of(failing)), ctx()))
                .isInstanceOf(StepException.class)
                .hasMessageContaining("boom");

        verify(outbox).enqueue(eq("ai.plan.failed"), anyString(), anyString(), anyString());
        verify(outbox, never()).enqueue(eq("ai.plan.completed"), anyString(), anyString(), anyString());
    }

    @Test
    void step_failure_with_fallback_continues_plan() {
        Step withFallback = new Step() {
            @Override
            public String name() {
                return "recoverable";
            }

            @Override
            public void execute(StepContext c) {
                throw new StepException("primary failed");
            }

            @Override
            public boolean hasFallback() {
                return true;
            }

            @Override
            public void fallback(StepContext c) {
                c.put("recovered", true);
            }
        };
        Step after = namedStep("after", () -> {});
        StepContext context = ctx();

        executor.execute(new Plan("x", List.of(withFallback, after)), context);

        assertThat(context.getExecutedSteps()).containsExactly("recoverable[fallback]", "after");
        assertThat((Boolean) context.get("recovered")).isTrue();
        verify(outbox).enqueue(eq("ai.step.fallback"), anyString(), anyString(), anyString());
        verify(outbox).enqueue(eq("ai.plan.completed"), anyString(), anyString(), anyString());
    }

    @Test
    void fallback_that_also_fails_aborts_plan() {
        Step doubleFail = new Step() {
            @Override
            public String name() {
                return "double-bad";
            }

            @Override
            public void execute(StepContext c) {
                throw new StepException("primary");
            }

            @Override
            public boolean hasFallback() {
                return true;
            }

            @Override
            public void fallback(StepContext c) {
                throw new RuntimeException("fallback blew up");
            }
        };

        assertThatThrownBy(() -> executor.execute(new Plan("x", List.of(doubleFail)), ctx()))
                .isInstanceOf(StepException.class)
                .hasMessageContaining("fallback failed");

        verify(outbox).enqueue(eq("ai.plan.failed"), anyString(), anyString(), anyString());
    }
}
