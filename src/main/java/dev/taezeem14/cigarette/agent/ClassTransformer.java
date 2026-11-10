package dev.taezeem14.cigarette.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class ClassTransformer implements ClassFileTransformer {

    public boolean isTarget(String className) {
        if (className == null) return false;
        return className.equals("net/minecraft/client/Minecraft")
                || className.equals("ave")
                || className.equals("net/minecraft/client/entity/EntityPlayerSP")
                || className.equals("bew")
                || className.equals("net/minecraft/client/renderer/EntityRenderer")
                || className.equals("bfk");
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null) {
            return classfileBuffer;
        }

        // Target verification and bytecode interception hook
        if (isTarget(className)) {
            // When running alongside Mixin or Forge, bytecode is transformed by Mixin subsystem
            return classfileBuffer;
        }

        return classfileBuffer;
    }
}
