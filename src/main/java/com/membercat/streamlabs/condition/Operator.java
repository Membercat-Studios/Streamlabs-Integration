package com.membercat.streamlabs.condition;

import com.membercat.streamlabs.config.issue.ConfigIssueHelper;
import com.membercat.streamlabs.util.ReflectUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.membercat.streamlabs.config.issue.Issues.HC1;
import static com.membercat.streamlabs.config.issue.Issues.HC2;

@SuppressWarnings("unused")
public interface Operator {
    String getName();

    boolean check(Object element1, Object element2);

    default void assignIssues(Object element1, Object element2, ConfigIssueHelper issueHelper) {
    }

    static Set<? extends Operator> findOperatorClasses() {
        //noinspection unchecked
        return Arrays.stream(Operator.class.getClasses())
                .filter(Operator.class::isAssignableFrom)
                .filter(Predicate.not(Class::isInterface))
                .map(cls -> ReflectUtil.instantiate((Class<? extends Operator>) cls, Operator.class))
                .collect(Collectors.toSet());
    }

    class Equality implements Operator {
        @Override
        public String getName() {
            return "=";
        }

        @Override
        public boolean check(Object element1, Object element2) {
            return element1.equals(element2);
        }
    }

    interface Numeric extends Operator {
        boolean check(Double d1, Double d2);

        @Override
        default void assignIssues(Object element1, Object element2, ConfigIssueHelper issueHelper) {
            for (Object element : List.of(element1, element2)) {
                if (element.toString().contains(",")) issueHelper.appendAtPath(HC2);
                if (element.toString().isBlank()) {
                    issueHelper.appendAtPath(HC1);
                    return;
                }
            }
        }

        @Override
        default boolean check(Object element1, Object element2) {
            if (!(element1 instanceof Double d1) || !(element2 instanceof Double d2)) return false;
            return this.check(d1, d2);
        }
    }

    class Larger implements Operator.Numeric {
        @Override
        public String getName() {
            return ">";
        }

        @Override
        public boolean check(Double d1, Double d2) {
            return d1 > d2;
        }
    }

    class LargerEquals implements Operator.Numeric {
        @Override
        public String getName() {
            return ">=";
        }

        @Override
        public boolean check(Double d1, Double d2) {
            return d1 >= d2;
        }
    }

    class Smaller implements Operator.Numeric {
        @Override
        public String getName() {
            return "<";
        }

        @Override
        public boolean check(Double d1, Double d2) {
            return d1 < d2;
        }
    }

    class SmallerEquals implements Operator.Numeric {
        @Override
        public String getName() {
            return "<=";
        }

        @Override
        public boolean check(Double d1, Double d2) {
            return d1 <= d2;
        }
    }

    class Contains implements Operator {
        @Override
        public String getName() {
            return ".>";
        }

        @Override
        public boolean check(Object element1, Object element2) {
            String s1 = element1.toString();
            String s2 = element2.toString();
            return s1.toLowerCase().contains(s2.toLowerCase());
        }
    }

    class ContainsReversed implements Operator {
        @Override
        public String getName() {
            return "<.";
        }

        @Override
        public boolean check(Object element1, Object element2) {
            String s1 = element1.toString();
            String s2 = element2.toString();
            return s2.toLowerCase().contains(s1.toLowerCase());
        }
    }
}
