package me.Domplanto.streamLabs.util.yaml;

import me.Domplanto.streamLabs.config.issue.ConfigIssueHelper;
import me.Domplanto.streamLabs.config.issue.ConfigPathStack;
import me.Domplanto.streamLabs.util.ReflectUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.Domplanto.streamLabs.config.issue.Issues.*;

@SuppressWarnings("rawtypes")
public class PropertyBasedClassInitializer<T extends PropertyLoadable> {
    private final Class<T> baseClass;
    private final Map<String, Class<? extends T>> classMap;
    private final String nameTranslationKey;
    private final Set<CustomSerializer<T>> customSerializers;

    public PropertyBasedClassInitializer(Class<T> baseClass, String nameId, @Nullable Set<CustomSerializer<T>> customSerializers) {
        this.baseClass = baseClass;
        this.classMap = ReflectUtil.loadClassesWithIds(baseClass, false);
        this.nameTranslationKey = "streamlabs.property_type.%s".formatted(nameId);
        this.customSerializers = Objects.requireNonNullElseGet(customSerializers, Set::of);
    }

    public List<? extends T> parseAll(List<Object> sections, ConfigurationSection parent, ConfigIssueHelper issueHelper) {
        //noinspection unchecked
        return sections.stream()
                .filter(obj -> {
                    boolean isMap = obj instanceof Map<?, ?>;
                    if (!isMap) {
                        issueHelper.push(baseClass, String.valueOf(sections.indexOf(obj)));
                        issueHelper.appendAtPath(WPI3.apply(nameTranslationKey, obj.toString()));
                        issueHelper.pop();
                    }
                    return isMap;
                })
                .map(map -> (Map<String, Object>) map)
                .flatMap(section -> {
                    issueHelper.push(baseClass, String.valueOf(sections.indexOf(section)));
                    Stream<? extends T> instance = Optional.ofNullable(deserialize(section, issueHelper, parent))
                            .map(Collection::stream).orElseGet(Stream::of);
                    issueHelper.pop();
                    return instance;
                }).filter(Objects::nonNull).toList();
    }

    public List<? extends T> deserialize(Map<String, Object> section, ConfigIssueHelper issueHelper, ConfigurationSection parent) {
        if (section == null) return null;

        try {
            Map<String, Class<? extends T>> classes = classMap.entrySet().stream()
                    .filter(entry -> section.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (classes.size() > 1) {
                issueHelper.appendAtPath(WPI1.apply(nameTranslationKey));
                return null;
            }

            Map.Entry<String, Class<? extends T>> propertyData = classes.entrySet().stream().findFirst().orElse(null);
            if (propertyData == null) {
                String key = section.keySet().stream().findFirst().orElse("");
                issueHelper.process(key);
                issueHelper.appendAtPath(WPI0.apply(nameTranslationKey, key));
                return null;
            }

            Object dataVal = section.get(propertyData.getKey());
            issueHelper.process(propertyData.getKey());
            T firstInstance = instantiate(propertyData.getValue());
            if (dataVal != null) {
                for (CustomSerializer<T> serializer : this.customSerializers) {
                    if (!serializer.shouldUse(dataVal, firstInstance)) continue;
                    issueHelper.pushProperty(propertyData.getKey());
                    List<? extends T> result = serializer.serialize(dataVal, propertyData, this, section, parent, issueHelper);
                    issueHelper.pop();
                    return result;
                }
            }

            List<T> instanceList = new ArrayList<>();
            instanceList.add(initializeSingle(propertyData.getValue(), 0, section, propertyData.getKey(), dataVal, parent, issueHelper));
            return instanceList;
        } catch (Exception e) {
            issueHelper.appendAtPathAndLog(EI0, e);
            return null;
        }
    }

    public T initializeSingle(Class<? extends T> stepCls, int stackOffset, Map<String, Object> section, @Nullable String key, Object value, ConfigurationSection parent, ConfigIssueHelper issueHelper) {
        T instance = instantiate(stepCls);
        PropertyLoadable<?> loadable = ((PropertyLoadable<?>) instance);
        if (key != null) issueHelper.pushProperty(key);
        for (PropertyLoadable.Serializer<?, ?> serializer : loadable.getOptionalDataSerializers()) {
            if (value == null || !serializer.from().isAssignableFrom(value.getClass())) continue;
            value = serializer.serializeObject(value);
        }

        if (value == null || !loadable.getExpectedDataType().isAssignableFrom(value.getClass())) {
            issueHelper.appendAtPath(WPI2(nameTranslationKey, instance.getExpectedDataType(), value));
            if (key != null) issueHelper.pop();
            return null;
        }
        if (key != null) issueHelper.pop();

        // "Hacky" solution to get a ConfigurationSection instance from the given map
        String id = UUID.randomUUID().toString();
        ConfigurationSection newSection = parent.createSection(id, section);
        instance.earlyLoad(issueHelper, newSection);
        ConfigPathStack stack = issueHelper.stack();
        stack.get(stack.size() - (3 + stackOffset)).process(id);
        if (key != null) issueHelper.pushProperty(key);
        //noinspection unchecked
        instance.load(value, issueHelper, newSection);
        if (key != null) issueHelper.pop();
        return instance;
    }

    private T instantiate(Class<? extends T> cls) {
        T instance = ReflectUtil.instantiate(cls, this.baseClass);
        if (instance == null)
            throw new RuntimeException("Failed to instantiate instance, check the error mentioned above!");
        return instance;
    }

    public interface CustomSerializer<T extends PropertyLoadable> {
        boolean shouldUse(@Nullable Object input, T loadableInstance);

        @Nullable List<? extends T> serialize(@Nullable Object input, Map.Entry<String, Class<? extends T>> propertyData, PropertyBasedClassInitializer<T> initializer, Map<String, Object> section, ConfigurationSection parent, ConfigIssueHelper issueHelper);
    }
}
