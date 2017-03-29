package com.nickrobison.trestle.metrics;

import com.codahale.metrics.annotation.Timed;
import com.nickrobison.trestle.annotations.metrics.CounterIncrement;
import com.nickrobison.trestle.annotations.metrics.Metriced;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nrobison on 3/27/17.
 */
public class ByteBuddyTest {

    private static final Map<String, Integer> values = new ConcurrentHashMap<>();

    @BeforeEach
    public void setup() {
        ByteBuddyAgent.install();
    }

    @Test
    public void test() {
        final TestClass testClass = new TestClass();
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.isAnnotatedWith(Metriced.class))
                .transform((builder, typeDescription, classLoader, module) -> {
                    return builder
//                            .implement(ITest.class)
                            .visit(Advice.to(TimingAdvice.class).on(ElementMatchers.isAnnotatedWith(Timed.class)))
                            .visit(Advice.to(CountingAdvice.class).on(ElementMatchers.isAnnotatedWith(CounterIncrement.class)));
//                    builder.visit(Advice.to(CountingAdvice.class).on(ElementMatchers.isAnnotatedWith(CounterIncrement.class)));
//                    return builder;
                })
                .installOnByteBuddyAgent();

        assertEquals("Hello world", testClass.getString(), "Should have string");
        assertEquals(1, testClass.testCount(), "Should be one");
    }

    @Metriced
    private class TestClass {

        public TestClass() {
        }

        @Timed
        public String getString() {
            return "Hello world";
        }

        @CounterIncrement(name = "test-annotation")
        public int testCount() {
            return 1;
        }
    }

    static class CountingAdvice {

        @Advice.OnMethodEnter
        static void enter(@Advice.Origin Method method, @Advice.This Object test) {
            System.out.println(method.getName());
            System.out.println(test.getClass().getName());
            values.put(method.getName(), 42);
            System.out.println(String.format("Counted %s", method.getAnnotation(CounterIncrement.class).name()));
        }

        @Advice.OnMethodExit
        static void exit(@Advice.Origin Method method) {
            System.out.println(String.format("Value: %s", values.get(method.getName())));
            System.out.println("Done counting");
        }
    }

    static class TimingAdvice {
        @Advice.OnMethodEnter
        public static long enter() {
            System.out.println("Enter advice");
            return System.nanoTime();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Enter long time, @Advice.Return String value) {
            System.out.println(String.format("Execution took %s ns", System.nanoTime() - time));
            System.out.println(String.format("Method value: %s", value));
        }
    }

    interface ITest {
        Map<String, Integer> values = new ConcurrentHashMap<>();
    }
}