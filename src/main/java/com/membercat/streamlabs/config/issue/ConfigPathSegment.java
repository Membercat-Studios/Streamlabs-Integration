package com.membercat.streamlabs.config.issue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigPathSegment {
    String id();
}
