package me.Domplanto.streamLabs.util;

import org.reflections.Reflections;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectUtil {
    public static <T> Set<? extends T> findClasses(Class<T> superType, Object... constructorArgs) {
        String packageName = superType.getPackageName();
        return new Reflections(packageName)
                .getSubTypesOf(superType)
                .stream()
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
