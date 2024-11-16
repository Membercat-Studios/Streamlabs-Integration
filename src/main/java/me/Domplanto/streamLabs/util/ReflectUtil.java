package me.Domplanto.streamLabs.util;

import org.reflections.Reflections;

import java.util.Set;
import java.util.stream.Collectors;

public class ReflectUtil {
    public static <T> Set<? extends T> findClasses(Class<T> superType) {
        String packageName = superType.getPackageName();
        return new Reflections(packageName)
                .getSubTypesOf(superType)
                .stream()
                .map(cls -> {
                    try {
                        return cls.getConstructor().newInstance();
                    } catch (ReflectiveOperationException ignored) {
                        return null;
                    }
                })
                .collect(Collectors.toSet());
    }
}
