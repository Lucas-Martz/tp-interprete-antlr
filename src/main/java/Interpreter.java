public class Interpreter extends MiniLangBaseVisitor<Value> {
    private final SymbolTable symbols;

    public Interpreter(SymbolTable symbols) {
        this.symbols = symbols;
    }

    @Override
    public Value visitProgram(MiniLangParser.ProgramContext ctx) {
        for (MiniLangParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return Value.defaultFor(Type.VOID);
    }

    @Override
    public Value visitVarDecl(MiniLangParser.VarDeclContext ctx) {
        String name = ctx.ID().getText();
        Type type = parseType(ctx.type().getText());
        Value value = ctx.expr() == null ? Value.defaultFor(type) : visit(ctx.expr()).castTo(type);
        symbols.assign(name, value);
        return Value.defaultFor(Type.VOID);
    }

    @Override
    public Value visitAssignment(MiniLangParser.AssignmentContext ctx) {
        symbols.assign(ctx.ID().getText(), visit(ctx.expr()));
        return Value.defaultFor(Type.VOID);
    }

    @Override
    public Value visitPrintStmt(MiniLangParser.PrintStmtContext ctx) {
        System.out.println(visit(ctx.expr()));
        return Value.defaultFor(Type.VOID);
    }

    @Override
    public Value visitIfStmt(MiniLangParser.IfStmtContext ctx) {
        if (visit(ctx.expr()).asBool()) {
            visit(ctx.block(0));
        } else {
            visit(ctx.block(1));
        }
        return Value.defaultFor(Type.VOID);
    }

    @Override
    public Value visitRepeatUntilStmt(MiniLangParser.RepeatUntilStmtContext ctx) {
        do {
            visit(ctx.block());
        } while (!visit(ctx.expr()).asBool());
        return Value.defaultFor(Type.VOID);
    }

    @Override
    public Value visitBlock(MiniLangParser.BlockContext ctx) {
        for (MiniLangParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return Value.defaultFor(Type.VOID);
    }

    @Override
    public Value visitLogicalOr(MiniLangParser.LogicalOrContext ctx) {
        Value result = visit(ctx.logicalAnd(0));
        for (int i = 1; i < ctx.logicalAnd().size(); i++) {
            result = new Value(Type.BOOL, result.asBool() || visit(ctx.logicalAnd(i)).asBool());
        }
        return result;
    }

    @Override
    public Value visitLogicalAnd(MiniLangParser.LogicalAndContext ctx) {
        Value result = visit(ctx.equality(0));
        for (int i = 1; i < ctx.equality().size(); i++) {
            result = new Value(Type.BOOL, result.asBool() && visit(ctx.equality(i)).asBool());
        }
        return result;
    }

    @Override
    public Value visitEquality(MiniLangParser.EqualityContext ctx) {
        Value result = visit(ctx.comparison(0));
        for (int i = 1; i < ctx.comparison().size(); i++) {
            Value right = visit(ctx.comparison(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            boolean equals = valuesEqual(result, right);
            result = new Value(Type.BOOL, "==".equals(op) ? equals : !equals);
        }
        return result;
    }

    @Override
    public Value visitComparison(MiniLangParser.ComparisonContext ctx) {
        Value result = visit(ctx.additive(0));
        for (int i = 1; i < ctx.additive().size(); i++) {
            Value right = visit(ctx.additive(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            double leftNumber = result.asReal();
            double rightNumber = right.asReal();
            switch (op) {
                case "<":
                    result = new Value(Type.BOOL, leftNumber < rightNumber);
                    break;
                case "<=":
                    result = new Value(Type.BOOL, leftNumber <= rightNumber);
                    break;
                case ">":
                    result = new Value(Type.BOOL, leftNumber > rightNumber);
                    break;
                case ">=":
                    result = new Value(Type.BOOL, leftNumber >= rightNumber);
                    break;
                default:
                    throw new SemanticException("Operador relacional no soportado: " + op);
            }
        }
        return result;
    }

    @Override
    public Value visitAdditive(MiniLangParser.AdditiveContext ctx) {
        Value result = visit(ctx.multiplicative(0));
        for (int i = 1; i < ctx.multiplicative().size(); i++) {
            Value right = visit(ctx.multiplicative(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            if ("+".equals(op) && result.getType() == Type.STRING && right.getType() == Type.STRING) {
                result = new Value(Type.STRING, result.asString() + right.asString());
            } else if ("+".equals(op)) {
                result = numeric(result, right, result.asReal() + right.asReal());
            } else {
                result = numeric(result, right, result.asReal() - right.asReal());
            }
        }
        return result;
    }

    @Override
    public Value visitMultiplicative(MiniLangParser.MultiplicativeContext ctx) {
        Value result = visit(ctx.unary(0));
        for (int i = 1; i < ctx.unary().size(); i++) {
            Value right = visit(ctx.unary(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            if ("*".equals(op)) {
                result = numeric(result, right, result.asReal() * right.asReal());
            } else {
                if (right.asReal() == 0.0) {
                    throw new SemanticException("Division por cero en ejecucion.");
                }
                result = numeric(result, right, result.asReal() / right.asReal());
            }
        }
        return result;
    }

    @Override
    public Value visitUnary(MiniLangParser.UnaryContext ctx) {
        if (ctx.primary() != null) {
            return visit(ctx.primary());
        }

        String op = ctx.getChild(0).getText();
        Value value = visit(ctx.unary());
        if ("!".equals(op)) {
            return new Value(Type.BOOL, !value.asBool());
        }
        if (value.getType() == Type.INT) {
            return new Value(Type.INT, -value.asInt());
        }
        return new Value(Type.REAL, -value.asReal());
    }

    @Override
    public Value visitPrimary(MiniLangParser.PrimaryContext ctx) {
        if (ctx.INT() != null) {
            return new Value(Type.INT, Integer.parseInt(ctx.INT().getText()));
        }
        if (ctx.REAL() != null) {
            return new Value(Type.REAL, Double.parseDouble(ctx.REAL().getText()));
        }
        if (ctx.STRING() != null) {
            return new Value(Type.STRING, unescape(ctx.STRING().getText()));
        }
        if (ctx.BOOL() != null) {
            return new Value(Type.BOOL, Boolean.parseBoolean(ctx.BOOL().getText()));
        }
        if (ctx.ID() != null) {
            return symbols.valueOf(ctx.ID().getText());
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

    private static Value numeric(Value left, Value right, double result) {
        if (left.getType() == Type.REAL || right.getType() == Type.REAL) {
            return new Value(Type.REAL, result);
        }
        return new Value(Type.INT, (int) result);
    }

    private static boolean valuesEqual(Value left, Value right) {
        if ((left.getType() == Type.INT || left.getType() == Type.REAL)
                && (right.getType() == Type.INT || right.getType() == Type.REAL)) {
            return Double.compare(left.asReal(), right.asReal()) == 0;
        }
        return left.raw().equals(right.raw());
    }

    private static String unescape(String text) {
        String content = text.substring(1, text.length() - 1);
        return content
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
