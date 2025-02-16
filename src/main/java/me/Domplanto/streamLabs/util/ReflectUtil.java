package me.Domplanto.streamLabs.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import me.Domplanto.streamLabs.util.components.Translations;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReflectUtil {
    private static final Logger BASE_LOGGER = Logger.getLogger("StreamLabs Reflection Utils");

    public static <T> Set<? extends T> initializeClasses(Class<T> superType, Object... constructorArgs) {
        return loadClasses(superType).stream()
                .map(cls -> instantiate(cls, superType, constructorArgs))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Nullable
    public static <T extends S, S> T instantiate(Class<T> cls, Class<S> superType, Object... constructorArgs) {
        try {
            Class<?>[] argTypes = Arrays.stream(constructorArgs)
                    .map(Object::getClass)
                    .toArray(Class[]::new);
            return cls.getConstructor(argTypes).newInstance(constructorArgs);
        } catch (Exception e) {
            BASE_LOGGER.log(Level.SEVERE, "Failed to instantiate class %s (subtype of %s), please report this error to the developers at %s"
                    .formatted(cls.getName(), superType.getName(), Translations.ISSUES_URL), e);
            return null;
        }
    }

    public static <T> Map<String, Class<T>> loadClassesWithIds(Class<T> superType) {
        return loadClasses(superType)
                .stream().filter(cls -> {
                    boolean hasAnnotation = cls.isAnnotationPresent(ClassId.class);
                    if (!hasAnnotation)
                        BASE_LOGGER.log(Level.SEVERE, "Failed to load class %s (subtype of %s) because of missing ID annotation, please report this error to the developers at %s"
                                .formatted(cls.getName(), superType.getName(), Translations.ISSUES_URL));
                    return hasAnnotation;
                })
                .collect(Collectors.toMap(cls -> cls.getAnnotation(ClassId.class).value(), cls -> cls));
    }

    public static <T> List<Class<T>> loadClasses(Class<T> superType) {
        String packageName = superType.getPackageName();
        try (ScanResult result = new ClassGraph()
                .enableAllInfo()
                .acceptPackages(packageName)
                .scan()) {
            return (superType.isInterface() ? result.getClassesImplementing(superType) : result.getSubclasses(superType))
                    .loadClasses(superType, false);
        } catch (Exception e) {
            BASE_LOGGER.log(Level.SEVERE, "Failed to load classes of supertype %s, please report this error to the developers at %s"
                    .formatted(superType.getName(), Translations.ISSUES_URL), e);
            return List.of();
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

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ClassId {
        String value();
    }
}
