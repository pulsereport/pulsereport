package io.github.pulsereport.assertions;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * Advice inlined at the start and end of every instrumented assertion method.
 * Exceptions raised by the advice itself are suppressed so the original assertion
 * behaves exactly as before.
 */
final class AssertionAdvice {

    private AssertionAdvice() {
    }

    @Advice.OnMethodEnter(suppress = Throwable.class)
    static boolean enter() {
        return AssertionRecorder.enter();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    static void exit(@Advice.Enter boolean outermost,
                     @Advice.Origin("#t") String owner,
                     @Advice.Origin("#m") String method,
                     @Advice.Origin("#d") String descriptor,
                     @Advice.AllArguments Object[] args,
                     @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returned,
                     @Advice.Thrown Throwable thrown) {
        AssertionRecorder.exit(outermost, owner, method, descriptor, args, returned, thrown);
    }
}
