package me.Domplanto.streamLabs.condition.operator;

import me.Domplanto.streamLabs.condition.Operator;

@SuppressWarnings("unused")
public class ContainsOperator implements Operator {
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
