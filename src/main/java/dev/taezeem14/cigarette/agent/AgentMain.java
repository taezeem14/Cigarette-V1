package dev.taezeem14.cigarette.agent;

import java.lang.instrument.Instrumentation;

public class AgentMain {

    private static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst);
    }

    private static void initialize(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
        System.out.println("[Cigarette Agent] Attaching JVM Bytecode Instrumentation Agent...");

        ClassTransformer transformer = new ClassTransformer();
        inst.addTransformer(transformer, true);

        if (inst.isRetransformClassesSupported()) {
            for (Class<?> clazz : inst.getAllLoadedClasses()) {
                if (transformer.isTarget(clazz.getName().replace('.', '/'))) {
                    try {
                        inst.retransformClasses(clazz);
                    } catch (Exception e) {
                        System.err.println("[Cigarette Agent] Failed to retransform class: " + clazz.getName());
                    }
                }
            }
        }

        System.out.println("[Cigarette Agent] Instrumentation successfully registered.");
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
