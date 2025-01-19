package me.Domplanto.streamLabs.util.yaml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface YamlPropertyCustomDeserializer {
    String propertyName() default "";
    boolean onlyUseWhenActuallySet() default true;
}
