import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Type> types = new HashMap<>();
    private final Map<String, Value> values = new HashMap<>();

    public void declare(String name, Type type) {
        if (types.containsKey(name)) {
            throw new SemanticException("La variable '" + name + "' ya fue declarada.");
        }
        types.put(name, type);
        values.put(name, Value.defaultFor(type));
    }

    public boolean exists(String name) {
        return types.containsKey(name);
    }

    public Type typeOf(String name) {
        Type type = types.get(name);
        if (type == null) {
            throw new SemanticException("La variable '" + name + "' no fue declarada.");
        }
        return type;
    }

    public Value valueOf(String name) {
        if (!values.containsKey(name)) {
            throw new SemanticException("La variable '" + name + "' no fue declarada.");
        }
        return values.get(name);
    }

    public void assign(String name, Value value) {
        Type expected = typeOf(name);
        if (!SemanticAnalyzer.isAssignable(expected, value.getType())) {
            throw new SemanticException(
                    "No se puede asignar " + value.getType() + " a la variable '" + name + "' de tipo " + expected + "."
            );
        }
        values.put(name, value.castTo(expected));
    }
}
