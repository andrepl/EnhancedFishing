package com.norcode.bukkit.enhancedfishing;

public class DoubleModifier {
    public static enum Type {
        ADDER, MULTIPLIER
    }
    private Type type;
    private double value;

    public DoubleModifier() {
        this.type = Type.MULTIPLIER;
        this.value = 1;
    }

    public DoubleModifier(double value) {
        this.type = Type.ADDER;
        this.value = value;
    }

    public DoubleModifier(String s) {
        if (s.startsWith("x")) {
            type = Type.MULTIPLIER;
            value = Double.parseDouble(s.substring(1));
        } else {
            type = Type.ADDER;
            value = Double.parseDouble(s);
        }
    }

    public double apply(double v) {
        switch (type) {
        case ADDER:
            v += value;
            break;
        case MULTIPLIER:
            v *= value;
            break;
        }
        return v;
    }
}
