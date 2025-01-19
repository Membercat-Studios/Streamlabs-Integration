package me.Domplanto.streamLabs.config.issue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigPathSegment {
    String id();
}
