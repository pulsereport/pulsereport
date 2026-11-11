package io.github.pulsereport.assertions;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * Advice inlined into instrumented fluent assertion methods (AssertJ checks,
 * REST Assured response checks). Like {@link AssertionAdvice}, but also passes
 * the object the check runs on, which holds the actual value. Exceptions raised
 * by the advice itself are suppressed so the assertion behaves exactly as before.
 */
final class InstanceAssertionAdvice {

    private InstanceAssertionAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    static boolean enter() {
        return AssertionRecorder.enter();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    static void exit(@Advice.Enter boolean outermost,
                     @Advice.This Object self,
                     @Advice.Origin("#t") String owner,
                     @Advice.Origin("#m") String method,
                     @Advice.Origin("#d") String descriptor,
                     @Advice.AllArguments Object[] args,
                     @Advice.Thrown Throwable thrown) {
        AssertionRecorder.exitFluent(outermost, owner, method, descriptor, self, args, thrown);
    }
}
