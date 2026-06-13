public class SemanticAnalyzer extends MiniLangBaseVisitor<Type> {
    private final SymbolTable symbols;

    public SemanticAnalyzer(SymbolTable symbols) {
        this.symbols = symbols;
    }

    public static boolean isAssignable(Type expected, Type actual) {
        return expected == actual || (expected == Type.REAL && actual == Type.INT);
    }

    @Override
    public Type visitProgram(MiniLangParser.ProgramContext ctx) {
        for (MiniLangParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return Type.VOID;
    }

    @Override
    public Type visitVarDecl(MiniLangParser.VarDeclContext ctx) {
        String name = ctx.ID().getText();
        Type declaredType = parseType(ctx.type().getText());
        if (symbols.exists(name)) {
            throw error(ctx.getStart().getLine(), "La variable '" + name + "' ya fue declarada.");
        }

        if (ctx.expr() != null) {
            Type valueType = visit(ctx.expr());
            if (!isAssignable(declaredType, valueType)) {
                throw error(ctx.getStart().getLine(), "No se puede inicializar '" + name + "' de tipo " + declaredType + " con " + valueType + ".");
            }
        }

        symbols.declare(name, declaredType);
        return Type.VOID;
    }

    @Override
    public Type visitAssignment(MiniLangParser.AssignmentContext ctx) {
        String name = ctx.ID().getText();
        ensureDeclared(name, ctx.getStart().getLine());
        Type expected = symbols.typeOf(name);
        Type actual = visit(ctx.expr());
        if (!isAssignable(expected, actual)) {
            throw error(ctx.getStart().getLine(), "No se puede asignar " + actual + " a '" + name + "' de tipo " + expected + ".");
        }
        return Type.VOID;
    }

    @Override
    public Type visitPrintStmt(MiniLangParser.PrintStmtContext ctx) {
        visit(ctx.expr());
        return Type.VOID;
    }

    @Override
    public Type visitIfStmt(MiniLangParser.IfStmtContext ctx) {
        requireBool(visit(ctx.expr()), ctx.getStart().getLine(), "La condicion del if debe ser booleana.");
        visit(ctx.block(0));
        visit(ctx.block(1));
        return Type.VOID;
    }

    @Override
    public Type visitDoWhileStmt(MiniLangParser.DoWhileStmtContext ctx) {
        visit(ctx.block());
        requireBool(visit(ctx.expr()), ctx.getStart().getLine(), "La condicion del do-while debe ser booleana.");
        return Type.VOID;
    }

    @Override
    public Type visitBlock(MiniLangParser.BlockContext ctx) {
        for (MiniLangParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return Type.VOID;
    }

    @Override
    public Type visitLogicalOr(MiniLangParser.LogicalOrContext ctx) {
        Type type = visit(ctx.logicalAnd(0));
        for (int i = 1; i < ctx.logicalAnd().size(); i++) {
            requireBool(type, ctx.getStart().getLine(), "El operador || requiere operandos booleanos.");
            Type right = visit(ctx.logicalAnd(i));
            requireBool(right, ctx.getStart().getLine(), "El operador || requiere operandos booleanos.");
            type = Type.BOOL;
        }
        return type;
    }

    @Override
    public Type visitLogicalAnd(MiniLangParser.LogicalAndContext ctx) {
        Type type = visit(ctx.equality(0));
        for (int i = 1; i < ctx.equality().size(); i++) {
            requireBool(type, ctx.getStart().getLine(), "El operador && requiere operandos booleanos.");
            Type right = visit(ctx.equality(i));
            requireBool(right, ctx.getStart().getLine(), "El operador && requiere operandos booleanos.");
            type = Type.BOOL;
        }
        return type;
    }

    @Override
    public Type visitEquality(MiniLangParser.EqualityContext ctx) {
        Type type = visit(ctx.comparison(0));
        for (int i = 1; i < ctx.comparison().size(); i++) {
            Type right = visit(ctx.comparison(i));
            if (!areComparable(type, right)) {
                throw error(ctx.getStart().getLine(), "No se pueden comparar " + type + " y " + right + ".");
            }
            type = Type.BOOL;
        }
        return type;
    }

    @Override
    public Type visitComparison(MiniLangParser.ComparisonContext ctx) {
        Type type = visit(ctx.additive(0));
        for (int i = 1; i < ctx.additive().size(); i++) {
            Type right = visit(ctx.additive(i));
            requireNumeric(type, ctx.getStart().getLine(), "Los operadores relacionales requieren numeros.");
            requireNumeric(right, ctx.getStart().getLine(), "Los operadores relacionales requieren numeros.");
            type = Type.BOOL;
        }
        return type;
    }

    @Override
    public Type visitAdditive(MiniLangParser.AdditiveContext ctx) {
        Type type = visit(ctx.multiplicative(0));
        for (int i = 1; i < ctx.multiplicative().size(); i++) {
            Type right = visit(ctx.multiplicative(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            if ("+".equals(op) && type == Type.STRING && right == Type.STRING) {
                type = Type.STRING;
            } else {
                requireNumeric(type, ctx.getStart().getLine(), "El operador " + op + " requiere numeros.");
                requireNumeric(right, ctx.getStart().getLine(), "El operador " + op + " requiere numeros.");
                type = promote(type, right);
            }
        }
        return type;
    }

    @Override
    public Type visitMultiplicative(MiniLangParser.MultiplicativeContext ctx) {
        Type type = visit(ctx.unary(0));
        for (int i = 1; i < ctx.unary().size(); i++) {
            Type right = visit(ctx.unary(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            requireNumeric(type, ctx.getStart().getLine(), "El operador " + op + " requiere numeros.");
            requireNumeric(right, ctx.getStart().getLine(), "El operador " + op + " requiere numeros.");
            if ("/".equals(op) && isLiteralZero(ctx.unary(i))) {
                throw error(ctx.getStart().getLine(), "Division por cero.");
            }
            type = promote(type, right);
        }
        return type;
    }

    @Override
    public Type visitUnary(MiniLangParser.UnaryContext ctx) {
        if (ctx.primary() != null) {
            return visit(ctx.primary());
        }

        String op = ctx.getChild(0).getText();
        Type type = visit(ctx.unary());
        if ("!".equals(op)) {
            requireBool(type, ctx.getStart().getLine(), "El operador ! requiere un booleano.");
            return Type.BOOL;
        }

        requireNumeric(type, ctx.getStart().getLine(), "El operador unario - requiere un numero.");
        return type;
    }

    @Override
    public Type visitPrimary(MiniLangParser.PrimaryContext ctx) {
        if (ctx.INT() != null) {
            return Type.INT;
        }
        if (ctx.REAL() != null) {
            return Type.REAL;
        }
        if (ctx.STRING() != null) {
            return Type.STRING;
        }
        if (ctx.BOOL() != null) {
            return Type.BOOL;
        }
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            ensureDeclared(name, ctx.getStart().getLine());
            return symbols.typeOf(name);
        }
        return visit(ctx.expr());
    }

    private static Type parseType(String text) {
        switch (text) {
            case "int":
                return Type.INT;
            case "real":
                return Type.REAL;
            case "string":
                return Type.STRING;
            case "bool":
                return Type.BOOL;
            default:
                throw new SemanticException("Tipo desconocido: " + text);
        }
    }

    private void ensureDeclared(String name, int line) {
        if (!symbols.exists(name)) {
            throw error(line, "La variable '" + name + "' no fue declarada.");
        }
    }

    private static void requireBool(Type type, int line, String message) {
        if (type != Type.BOOL) {
            throw error(line, message + " Tipo recibido: " + type + ".");
        }
    }

    private static void requireNumeric(Type type, int line, String message) {
        if (type != Type.INT && type != Type.REAL) {
            throw error(line, message + " Tipo recibido: " + type + ".");
        }
    }

    private static boolean areComparable(Type left, Type right) {
        if ((left == Type.INT || left == Type.REAL) && (right == Type.INT || right == Type.REAL)) {
            return true;
        }
        return left == right;
    }

    private static Type promote(Type left, Type right) {
        return left == Type.REAL || right == Type.REAL ? Type.REAL : Type.INT;
    }

    private static boolean isLiteralZero(MiniLangParser.UnaryContext ctx) {
        if (ctx.primary() == null) {
            return false;
        }
        MiniLangParser.PrimaryContext primary = ctx.primary();
        if (primary.INT() != null) {
            return Integer.parseInt(primary.INT().getText()) == 0;
        }
        if (primary.REAL() != null) {
            return Double.parseDouble(primary.REAL().getText()) == 0.0;
        }
        return false;
    }

    private static SemanticException error(int line, String message) {
        return new SemanticException("Error semantico en linea " + line + ": " + message);
    }
}
