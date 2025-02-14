package me.Domplanto.streamLabs.condition;

import me.Domplanto.streamLabs.util.ReflectUtil;

import java.util.Set;

public interface Operator {
    String getName();

    boolean check(Object element1, Object element2);

    static Set<? extends Operator> findOperatorClasses() {
        return ReflectUtil.initializeClasses(Operator.class);
    }
}
