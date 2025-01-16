package me.Domplanto.streamLabs.util.yaml;

import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public interface YamlPropertyObject {
    default void acceptYamlProperties(ConfigurationSection section) throws ReflectiveOperationException {
        for (Field field : this.getYamlPropertyFields()) {
            YamlProperty property = field.getAnnotation(YamlProperty.class);
            Object value = section.getKeys(true).contains(property.value()) ? section.get(property.value()) : field.get(this);

            field.setAccessible(true);
            field.set(this, value);
        }
    }

    default void serializeYamlProperties(ConfigurationSection section) throws ReflectiveOperationException {
        for (Field field : this.getYamlPropertyFields()) {
            YamlProperty property = field.getAnnotation(YamlProperty.class);

            field.setAccessible(true);
            section.set(property.value(), field.get(this));
        }
    }

    private Set<Field> getYamlPropertyFields() {
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(YamlProperty.class))
                .collect(Collectors.toSet());
    }
}
