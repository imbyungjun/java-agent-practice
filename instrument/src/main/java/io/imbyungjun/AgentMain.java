package io.imbyungjun;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AgentMain {
    public static void premain(String agentArgs, Instrumentation inst) {
        sayHello();
        transformClass("com.example.Person", inst);
    }

    private static void sayHello() {
        System.out.println("Hello, Java Agent!");
    }

    private static void transformClass(String className, Instrumentation instrumentation) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
        // see if we can get the class using forName
        try {
            targetCls = Class.forName(className);
            targetClassLoader = targetCls.getClassLoader();
            transform(targetCls, targetClassLoader, instrumentation);
            return;
        } catch (Exception ex) {
            System.err.println("Class [{}] not found with Class.forName");
        }
        // otherwise iterate all loaded classes and find what we want
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                transform(targetCls, targetClassLoader, instrumentation);
                return;
            }
        }
        throw new RuntimeException("Failed to find class [" + className + "]");
    }

    private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation) {
        MyClassTransformer dt = new MyClassTransformer(clazz.getName(), classLoader);
        instrumentation.addTransformer(dt, true);
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Transform failed for: [" + clazz.getName() + "]", ex);
        }
    }

    private static class MyClassTransformer implements ClassFileTransformer {
        private final String targetClassName;
        private final ClassLoader targetClassLoader;

        public MyClassTransformer(String targetClassName, ClassLoader targetClassLoader) {
            this.targetClassName = targetClassName;
            this.targetClassLoader = targetClassLoader;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            byte[] byteCode = classfileBuffer;
            String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/");

            if (!className.equals(finalTargetClassName)) {
                return byteCode;
            }

            if (loader.equals(targetClassLoader)) {
                try {
                    ClassPool cp = ClassPool.getDefault();
                    CtClass cc = cp.get(targetClassName);
                    CtMethod m = cc.getDeclaredMethod("sayHello");
                    m.insertBefore("System.out.println(\"Before Method\");");
                    m.insertAfter("System.out.println(\"After Method\");");

                    byteCode = cc.toBytecode();
                    cc.detach();
                } catch (NotFoundException | CannotCompileException | IOException e) {
                    System.err.println("Exception" + e);
                }
            }
            return byteCode;
        }
    }
}
