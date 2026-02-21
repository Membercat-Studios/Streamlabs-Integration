package com.membercat.streamlabs.util.yaml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface YamlPropertyIssueAssigner {
    String propertyName();
}
