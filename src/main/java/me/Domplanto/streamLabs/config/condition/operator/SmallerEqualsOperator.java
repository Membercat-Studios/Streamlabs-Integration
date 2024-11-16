package me.Domplanto.streamLabs.config.condition.operator;

import me.Domplanto.streamLabs.config.condition.Operator;

@SuppressWarnings("unused")
public class SmallerEqualsOperator implements Operator {
    @Override
    public String getName() {
        return "<";
    }

    @Override
    public boolean check(Object element1, Object element2) {
        if (!(element1 instanceof Double d1) || !(element2 instanceof Double d2)) return false;

        return d1 <= d2;
    }
}
