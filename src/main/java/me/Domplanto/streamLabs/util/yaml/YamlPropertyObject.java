package me.Domplanto.streamLabs.util.yaml;

import me.Domplanto.streamLabs.config.issue.ConfigIssue;
import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.PluginConfig.getSectionKeys;

public interface YamlPropertyObject {
    @Nullable
    default String getPrefix() {
        return null;
    }

    default void acceptYamlProperties(ConfigurationSection section, ConfigIssueHelper issueHelper) {
        try {
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
                if (YamlPropertyObject.class.isAssignableFrom(field.getType()) && section.isConfigurationSection(id))
                    //noinspection unchecked
                    sectionVal = YamlPropertyObject.createInstance((Class<? extends YamlPropertyObject>) field.getType(), section.getConfigurationSection(id), issueHelper);
                if (customDeserializer != null && (actuallySet || !customDeserializer.getAnnotation(YamlPropertyCustomDeserializer.class).onlyUseWhenActuallySet())) {
                    customDeserializer.setAccessible(true);
                    sectionVal = customDeserializer.invoke(this, sectionVal, issueHelper);
                }

                Object value = actuallySet ? sectionVal : field.get(this);
                try {
                    field.set(this, value);
                } catch (IllegalArgumentException e) {
                    issueHelper.appendAtPath(ConfigIssue.Level.WARNING, "Unexpected property type found, expected %s but got %s (now using default \"%s\")"
                            .formatted(field.getType().getSimpleName(), value != null ? value.getClass().getSimpleName() : "null", field.get(this)));
                    issueHelper.pop();
                    return;
                }

                for (Method assigner : issueAssigners) {
                    assigner.setAccessible(true);
                    assigner.invoke(this, issueHelper, actuallySet);
                }
                issueHelper.pop();
            }
        } catch (ReflectiveOperationException e) {
            issueHelper.appendAtPathAndLog(ConfigIssue.Level.ERROR, "Error in an internal configuration system, please report the exception found in the logs to the developers!", e);
            issueHelper.popIfProperty();
        }
    }

    private Set<Field> getYamlPropertyFields() {
        return Stream.concat(Arrays.stream(getClass().getDeclaredFields()), Arrays.stream(getClass().getSuperclass().getDeclaredFields()))
                .filter(field -> field.isAnnotationPresent(YamlProperty.class))
                .collect(Collectors.toSet());
    }

    private Set<Method> getIssueAssignerMethods(String propertyName) {
        return Stream.concat(Arrays.stream(getClass().getDeclaredMethods()), Arrays.stream(getClass().getSuperclass().getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(YamlPropertyIssueAssigner.class))
                .filter(method -> method.getAnnotation(YamlPropertyIssueAssigner.class).propertyName().equals(propertyName))
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getParameterTypes()[0] == ConfigIssueHelper.class && method.getParameterTypes()[1] == boolean.class)
                .collect(Collectors.toSet());
    }

    @Nullable
    private Method getCustomDeserializer(String propertyName, Class<?> propertyCls) {
        return Stream.concat(Arrays.stream(getClass().getDeclaredMethods()), Arrays.stream(getClass().getSuperclass().getDeclaredMethods()))
                .filter(method -> method.isAnnotationPresent(YamlPropertyCustomDeserializer.class))
                .filter(method -> method.getAnnotation(YamlPropertyCustomDeserializer.class).propertyName().equals(propertyName))
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getReturnType() == propertyCls && method.getParameterTypes()[1] == ConfigIssueHelper.class)
                .findAny().orElse(null);
    }

    @Nullable
    static <T extends YamlPropertyObject> T createInstance(Class<T> type, ConfigurationSection section, ConfigIssueHelper issueHelper) {
        try {
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
