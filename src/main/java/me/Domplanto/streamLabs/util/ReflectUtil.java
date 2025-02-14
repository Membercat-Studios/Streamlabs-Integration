package me.Domplanto.streamLabs.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectUtil {
    public static <T> Set<? extends T> initializeClasses(Class<T> superType, Object... constructorArgs) {
        String packageName = superType.getPackageName();
        try (ScanResult result = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(packageName)
                .scan()) {
            return (superType.isInterface() ? result.getClassesImplementing(superType) : result.getSubclasses(superType)).stream()
                    .map(cls -> cls.loadClass(superType))
                    .map(cls -> {
                        try {
                            Class<?>[] argTypes = Arrays.stream(constructorArgs)
                                    .map(Object::getClass)
                                    .toArray(Class[]::new);
                            return cls.getConstructor(argTypes).newInstance(constructorArgs);
                        } catch (ReflectiveOperationException ignored) {
                            return null;
                        }
                    })
                    .collect(Collectors.toSet());
        }
    }

    public static boolean checkForPaper() {
        try {
            Class.forName("io.papermc.paper.plugin.loader.PluginLoader");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
