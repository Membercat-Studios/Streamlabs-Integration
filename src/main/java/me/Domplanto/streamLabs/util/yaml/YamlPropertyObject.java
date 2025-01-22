package me.Domplanto.streamLabs.util.yaml;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.PluginConfig.getSectionKeys;
import static me.Domplanto.streamLabs.config.issue.Issues.*;

public interface YamlPropertyObject {
    @Nullable
    default String getPrefix() {
        return null;
    }

    default void acceptYamlProperties(ConfigurationSection section, ConfigIssueHelper issueHelper) {
        try {
            suppressForSection(section, issueHelper);
            for (Field field : this.getYamlPropertyFields()) {
                YamlProperty property = field.getAnnotation(YamlProperty.class);
                String id = getPrefix() != null ? "%s.%s".formatted(getPrefix(), property.value()) : property.value();
                Set<Method> issueAssigners = this.getIssueAssignerMethods(property.value());
                issueHelper.pushProperty(property.value());

                boolean actuallySet = getSectionKeys(section, true).contains(id);
                field.setAccessible(true);
                Method customDeserializer = this.getCustomDeserializer(property.value(), field.getType());
                Object sectionVal = actuallySet ? section.get(id) : null;
                if (property.value().equalsIgnoreCase("!section") && field.getType() == String.class) {
                    sectionVal = section.getName();
                    actuallySet = true;
                }
                if (YamlPropertyObject.class.isAssignableFrom(field.getType()) && section.isConfigurationSection(id)) {
                    ConfigurationSection subSection = section.getConfigurationSection(id);
                    suppressForSection(subSection, issueHelper);
                    //noinspection unchecked
                    sectionVal = YamlPropertyObject.createInstance((Class<? extends YamlPropertyObject>) field.getType(), subSection, issueHelper);
                }
                if (customDeserializer != null && (actuallySet || !customDeserializer.getAnnotation(YamlPropertyCustomDeserializer.class).onlyUseWhenActuallySet())) {
                    customDeserializer.setAccessible(true);
                    try {
                        sectionVal = customDeserializer.invoke(this, sectionVal, issueHelper);
                    } catch (Exception e) {
                        issueHelper.appendAtPathAndLog(WI0, e);
                    }
                }

                Object value = actuallySet ? sectionVal : field.get(this);
                try {
                    field.set(this, value);
                } catch (IllegalArgumentException e) {
                    issueHelper.appendAtPath(WI2(field, value, this));
                    issueHelper.pop();
                    return;
                }

                for (Method assigner : issueAssigners) {
                    assigner.setAccessible(true);
                    try {
                        assigner.invoke(this, issueHelper, actuallySet);
                    } catch (Exception e) {
                        issueHelper.appendAtPathAndLog(WI1, e);
                    }
                }
                issueHelper.pop();
            }
        } catch (ReflectiveOperationException e) {
            issueHelper.appendAtPathAndLog(EI1, e);
            issueHelper.popIfProperty();
        }
    }

    private static void suppressForSection(ConfigurationSection section, ConfigIssueHelper issueHelper) {
        if (getSectionKeys(section, false).contains("__suppress"))
            issueHelper.suppress(section.getStringList("__suppress"));
    }

    private Set<Field> getYamlPropertyFields() {
        return tracePropertySuperclasses(getClass())
                .flatMap(cls -> Arrays.stream(cls.getDeclaredFields()))
                .filter(field -> field.isAnnotationPresent(YamlProperty.class))
                .collect(Collectors.toSet());
    }

    private Set<Method> getIssueAssignerMethods(String propertyName) {
        return tracePropertySuperclasses(getClass())
                .flatMap(cls -> Arrays.stream(cls.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(YamlPropertyIssueAssigner.class))
                .filter(method -> method.getAnnotation(YamlPropertyIssueAssigner.class).propertyName().equals(propertyName))
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getParameterTypes()[0] == ConfigIssueHelper.class && method.getParameterTypes()[1] == boolean.class)
                .collect(Collectors.toSet());
    }

    @Nullable
    private Method getCustomDeserializer(String propertyName, Class<?> propertyCls) {
        return tracePropertySuperclasses(getClass())
                .flatMap(cls -> Arrays.stream(cls.getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(YamlPropertyCustomDeserializer.class))
                .filter(method -> method.getAnnotation(YamlPropertyCustomDeserializer.class).propertyName().equals(propertyName))
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getReturnType() == propertyCls && method.getParameterTypes()[1] == ConfigIssueHelper.class)
                .findAny().orElse(null);
    }

    private Stream<Class<? extends YamlPropertyObject>> tracePropertySuperclasses(Class<?> cls) {
        Set<Class<? extends YamlPropertyObject>> classes = tracePropertySuperclasses(cls, new HashSet<>());
        classes.add(getClass());
        return classes.stream();
    }

    private static Set<Class<? extends YamlPropertyObject>> tracePropertySuperclasses(Class<?> cls, Set<Class<? extends YamlPropertyObject>> collection) {
        if (!YamlPropertyObject.class.isAssignableFrom(cls.getSuperclass())) return collection;
        //noinspection unchecked
        collection.add((Class<? extends YamlPropertyObject>) cls.getSuperclass());
        return tracePropertySuperclasses(cls.getSuperclass(), collection);
    }

    @Nullable
    static <T extends YamlPropertyObject> T createInstance(Class<T> type,
                                                           ConfigurationSection section, ConfigIssueHelper issueHelper) {
        try {
            suppressForSection(section, issueHelper);
            Method staticDeserializer = YamlPropertyObject.getOtherCustomDeserializer(type);
            T instance;
            if (staticDeserializer != null) {
                staticDeserializer.setAccessible(true);
                //noinspection unchecked
                instance = (T) staticDeserializer.invoke(null, section, issueHelper);
            } else
                instance = type.getConstructor().newInstance();

            if (instance != null)
                instance.acceptYamlProperties(section, issueHelper);
            return instance;
        } catch (ReflectiveOperationException ignore) {
        }

        return null;
    }

    @Nullable
    private static Method getOtherCustomDeserializer(Class<?> otherPropertyCls) {
        return Arrays.stream(otherPropertyCls.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.isAnnotationPresent(YamlPropertyCustomDeserializer.class))
                .filter(method -> method.getReturnType() == otherPropertyCls)
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getParameterTypes()[0] == ConfigurationSection.class && method.getParameterTypes()[1] == ConfigIssueHelper.class)
                .findAny().orElse(null);
    }
}
