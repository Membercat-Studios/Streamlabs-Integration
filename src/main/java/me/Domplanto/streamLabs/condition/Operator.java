package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.util.ReflectUtil;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public interface Operator {
    String getName();

    boolean check(Object element1, Object element2);

    static Set<? extends Operator> findOperatorClasses() {
        //noinspection unchecked
        return Arrays.stream(Operator.class.getClasses())
                .filter(Operator.class::isAssignableFrom)
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

    class Larger implements Operator {
        @Override
        public String getName() {
            return ">";
        }

        @Override
        public boolean check(Object element1, Object element2) {
            if (!(element1 instanceof Double d1) || !(element2 instanceof Double d2)) return false;

            return d1 > d2;
        }
    }

    class LargerEquals implements Operator {
        @Override
        public String getName() {
            return ">=";
        }

        @Override
        public boolean check(Object element1, Object element2) {
            if (!(element1 instanceof Double d1) || !(element2 instanceof Double d2)) return false;

            return d1 >= d2;
        }
    }

    class Smaller implements Operator {
        @Override
        public String getName() {
            return "<";
        }

        @Override
        public boolean check(Object element1, Object element2) {
            if (!(element1 instanceof Double d1) || !(element2 instanceof Double d2)) return false;

            return d1 < d2;
        }
    }

    class SmallerEquals implements Operator {
        @Override
        public String getName() {
            return "<=";
        }

        @Override
        public boolean check(Object element1, Object element2) {
            if (!(element1 instanceof Double d1) || !(element2 instanceof Double d2)) return false;

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
