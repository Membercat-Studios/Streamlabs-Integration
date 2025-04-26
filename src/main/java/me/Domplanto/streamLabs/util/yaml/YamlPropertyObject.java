package me.Domplanto.streamLabs.util.yaml;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

public interface YamlPropertyObject {
    String SUPPRESS_KEY = "__suppress";

    @Nullable
    static String getString(ConfigurationSection section, String key) {
        return section.getKeys(true).contains(key) ? section.getString(key) : null;
    }

    @NotNull
    static Set<String> getSectionKeys(@Nullable ConfigurationSection section, boolean recursive) {
        if (section == null) return new HashSet<>();
        return section.getKeys(recursive);
    }

    private static <T extends YamlPropertyObject> Map<String, T> loadSection(ConfigurationSection section, Class<T> cls, ConfigIssueHelper issueHelper) {
        //noinspection unchecked
        return getSectionKeys(section, false).stream()
                .map(key -> {
                    assert section != null;
                    issueHelper.push(cls, key);
                    try {
                        ConfigurationSection subSection = section.getConfigurationSection(key);
                        if (subSection == null) {
                            issueHelper.pop();
                            return new Object[0];
                        }

                        T element = YamlPropertyObject.createInstance(cls, subSection, issueHelper);
                        issueHelper.pop();
                        return new Object[]{key, element};
                    } catch (Exception e) {
                        issueHelper.appendAtPathAndLog(EI0, e);
                    }
                    issueHelper.pop();
                    return new Object[0];
                })
                .filter(o -> o.length > 0)
                .collect(Collectors.toMap(o -> (String) o[0], o -> (T) o[1]));
    }

    private static void suppressForSection(ConfigurationSection section, ConfigIssueHelper issueHelper) {
        if (getSectionKeys(section, false).contains(SUPPRESS_KEY)) {
            issueHelper.process(SUPPRESS_KEY);
            issueHelper.suppress(section.getStringList(SUPPRESS_KEY));
        }
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

    @Nullable
    default String getPrefix() {
        return null;
    }

    default void acceptYamlProperties(ConfigurationSection section, ConfigIssueHelper issueHelper) {
        try {
            suppressForSection(section, issueHelper);
            for (Field field : this.getYamlPropertyFields()) {
                boolean isSection = field.isAnnotationPresent(YamlPropertySection.class);
                String value = isSection ? field.getAnnotation(YamlPropertySection.class).value()
                        : field.getAnnotation(YamlProperty.class).value();
                Class<? extends YamlPropertyObject> sectionCls = isSection ? field.getAnnotation(YamlPropertySection.class).elementClass() : null;
                String id = getPrefix() != null ? "%s.%s".formatted(getPrefix(), value) : value;
                Set<Method> issueAssigners = this.getIssueAssignerMethods(value);
                boolean actuallySet = getSectionKeys(section, true).contains(id);
                if (actuallySet) issueHelper.process(id);

                if (isSection)
                    issueHelper.pushSection(value);
                else
                    issueHelper.pushProperty(value);
                field.setAccessible(true);
                Method customDeserializer = this.getCustomDeserializer(value, field.getType());
                Object sectionVal = actuallySet ? section.get(id) : null;
                if (value.equalsIgnoreCase("!section") && field.getType() == String.class) {
                    sectionVal = section.getName();
                    actuallySet = true;
                }
                if (sectionCls != null && section.isConfigurationSection(id))
                    sectionVal = loadSection(section.getConfigurationSection(id), sectionCls, issueHelper);
                if (YamlPropertyObject.class.isAssignableFrom(field.getType()) && section.isConfigurationSection(id)) {
                    issueHelper.pop();
                    issueHelper.push(field.getType(), value);
                    ConfigurationSection subSection = section.getConfigurationSection(id);
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

                Object finalValue = actuallySet ? sectionVal : field.get(this);
                try {
                    field.set(this, finalValue);
                } catch (IllegalArgumentException e) {
                    issueHelper.appendAtPath(WI2(field, finalValue, this));
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

            getSectionKeys(section, false).stream()
                    .filter(key -> !issueHelper.getProcessed().contains(key))
                    .forEach(key -> issueHelper.appendAtPath(WY0.apply(key)));
        } catch (ReflectiveOperationException e) {
            issueHelper.appendAtPathAndLog(EI1, e);
            issueHelper.popIf(entry -> entry.isProperty() || entry.isSection());
        }
    }

    private Set<Field> getYamlPropertyFields() {
        return tracePropertySuperclasses(getClass())
                .flatMap(cls -> Arrays.stream(cls.getDeclaredFields()))
                .filter(field -> field.isAnnotationPresent(YamlProperty.class) || field.isAnnotationPresent(YamlPropertySection.class))
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
}
