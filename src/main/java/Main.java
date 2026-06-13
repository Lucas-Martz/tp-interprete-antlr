import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Uso: mvn exec:java -Dexec.args=\"ruta/al/programa.ml\"");
            System.exit(1);
        }

        try {
            CharStream input = CharStreams.fromFileName(args[0]);
            MiniLangLexer lexer = new MiniLangLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(new ThrowingErrorListener());

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            MiniLangParser parser = new MiniLangParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ThrowingErrorListener());

            MiniLangParser.ProgramContext tree = parser.program();

            SymbolTable semanticSymbols = new SymbolTable();
            new SemanticAnalyzer(semanticSymbols).visit(tree);

            SymbolTable runtimeSymbols = new SymbolTable();
            copyDeclarations(tree, runtimeSymbols);
            new Interpreter(runtimeSymbols).visit(tree);
        } catch (IOException exception) {
            System.err.println("No se pudo leer el archivo: " + args[0]);
            System.exit(1);
        } catch (SemanticException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        } catch (RuntimeException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("Error sintactico")) {
                System.err.println(exception.getMessage());
                System.exit(1);
            }
            throw exception;
        }
    }

    private static void copyDeclarations(MiniLangParser.ProgramContext tree, SymbolTable runtimeSymbols) {
        for (MiniLangParser.StatementContext statement : tree.statement()) {
            declareFromStatement(statement, runtimeSymbols);
        }
    }

    private static void declareFromStatement(MiniLangParser.StatementContext statement, SymbolTable runtimeSymbols) {
        if (statement.varDecl() != null) {
            MiniLangParser.VarDeclContext declaration = statement.varDecl();
            runtimeSymbols.declare(declaration.ID().getText(), parseType(declaration.type().getText()));
        } else if (statement.block() != null) {
            declareFromBlock(statement.block(), runtimeSymbols);
        } else if (statement.ifStmt() != null) {
            declareFromBlock(statement.ifStmt().block(0), runtimeSymbols);
            declareFromBlock(statement.ifStmt().block(1), runtimeSymbols);
        } else if (statement.doWhileStmt() != null) {
            declareFromBlock(statement.doWhileStmt().block(), runtimeSymbols);
        }
    }

    private static void declareFromBlock(MiniLangParser.BlockContext block, SymbolTable runtimeSymbols) {
        for (MiniLangParser.StatementContext statement : block.statement()) {
            declareFromStatement(statement, runtimeSymbols);
        }
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
}
