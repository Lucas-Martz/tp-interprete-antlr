public class Value {
    private final Type type;
    private final Object value;

    public Value(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public static Value defaultFor(Type type) {
        switch (type) {
            case INT:
                return new Value(Type.INT, 0);
            case REAL:
                return new Value(Type.REAL, 0.0);
            case STRING:
                return new Value(Type.STRING, "");
            case BOOL:
                return new Value(Type.BOOL, false);
            default:
                return new Value(Type.VOID, null);
        }
    }

    public Type getType() {
        return type;
    }

    public Object raw() {
        return value;
    }

    public int asInt() {
        return (Integer) value;
    }

    public double asReal() {
        if (type == Type.INT) {
            return asInt();
        }
        return (Double) value;
    }

    public boolean asBool() {
        return (Boolean) value;
    }

    public String asString() {
        return String.valueOf(value);
    }

    public Value castTo(Type target) {
        if (type == target) {
            return this;
        }
        if (type == Type.INT && target == Type.REAL) {
            return new Value(Type.REAL, asReal());
        }
        throw new SemanticException("No se puede convertir " + type + " a " + target + ".");
    }

    @Override
    public String toString() {
        if (type == Type.REAL) {
            double number = asReal();
            if (number == Math.rint(number)) {
                return String.format("%.1f", number);
            }
        }
        return String.valueOf(value);
    }
}
