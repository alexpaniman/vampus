package script.interpreter;

import com.google.common.base.Preconditions;
import script.analysis.Lexer;
import script.analysis.LexerException;
import script.commands.CommandInterpreter;
import script.commands.TFProcessor;
import script.token.Token;
import script.token.TokenType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

import static script.token.TokenType.*;

@SuppressWarnings("WeakerAccess")
public class Interpreter {
    private Token HEAD;
    private TFProcessor tfp;
    private Map<String, Object> variables;
    private List<Function> functions;

    public static void main(String[] args) throws IOException, LexerException, InterpretationException {
        StringBuilder sb = new StringBuilder();
        for (String str : Files.readAllLines(new File("src/main/resources/test_script.tsc").toPath()))
            sb.append(str).append("\n");
        Token HEAD = new Lexer().tokenize(sb.toString());
        new Interpreter(
                new TFProcessor(
                        new CommandInterpreter()
                ),
                HEAD
        ).executeScript();
    }

    public Interpreter(TFProcessor tfp, Token HEAD) {
        this.tfp = tfp;
        if (HEAD.typed())
            this.HEAD = HEAD;
        else
            this.HEAD = HEAD.next();
        variables = new HashMap<>();
        functions = new ArrayList<>();
    }

    private class Result {
        private TokenType last;
        private Object result;

        private Result(TokenType type, Object value) {
            this.last = type;
            this.result = value;
        }
    }

    private class Function {
        private String name;
        private List<String> variables;
        private Token body;

        private Function(String name, List<String> variables, Token body) {
            this.name = name;
            this.variables = variables;
            this.body = body;
        }
    }

    private Object parseType(Token token) {
        if (token.type().equals(STRING))
            return token.value();
        else if (token.type().equals(LITERAL))
            return Long.valueOf(token.value());
        else if (token.type().equals(DOUBLE))
            return Double.parseDouble(token.value());
        else if (token.type().equals(BOOLEAN))
            return Boolean.parseBoolean(token.value());
        else if (token.type().equals(NULL))
            return null;
        else throw new IllegalArgumentException();
    }

    private Object calculateFunction() throws InterpretationException {
        String functionName = HEAD.value();
        List<Object> args = new ArrayList<>();
        Map<String, Object> args_map = new HashMap<>();
        next(2);
        Result result;
        do {
            String var = null;
            if (HEAD.type().equals(VARIABLE) && HEAD.next() != null && HEAD.next().type().equals(EQUAL)) {
                var = HEAD.value();
                next(2);
            }
            result = calculateExpression();
            if (result == null)
                break;
            else if (var != null)
                args_map.put(var, result.result);
            else
                args.add(result.result);
        } while (!result.last.equals(RRB));
        try {
            if (args_map.size() == 0)
                try {
                    return calculateFunction(functionName, args.toArray());
                } catch (InterpretationException exc) {
                    try {
                        return tfp.func(functionName, args.toArray());
                    } catch (NoSuchMethodException e) {
                        try {
                            return calculateFunction(functionName, new HashMap<>());
                        } catch (InterpretationException ie) {
                            return tfp.func(functionName, new HashMap<>());
                        }
                    }
                }
            else if (args.size() == 0)
                try {
                    return calculateFunction(functionName, args_map);
                } catch (InterpretationException exc) {
                    return tfp.func(functionName, args_map);
                }
            else
                throw new InterpretationException("Explicit arguments cannot be used with implicit ones");
        } catch (NoSuchMethodException e) {
            throw new InterpretationException("Cant't find '" + functionName + "' function");
        }
    }

    private Object calculateFunction(String name, Object... variables) throws InterpretationException {
        boolean contains = false;
        Object result = null;
        for (Function func : functions)
            if (func.name.equals(name) && func.variables.size() == variables.length) {
                Map<String, Object> backup = new HashMap<>(this.variables);
                try {
                    for (int i = 0; i < variables.length; i++)
                        this.variables.put(func.variables.get(i), variables[i]);
                    Token temp = HEAD;
                    HEAD = func.body;
                    while (!HEAD.type().equals(RCB) && !HEAD.type().equals(RETURN)) {
                        execute();
                        if (HEAD.type().equals(SEMICOLON))
                            next(1);
                    }
                    if (HEAD.type().equals(RETURN)) {
                        HEAD = HEAD.next();
                        result = calculateExpression("when return value calculating");
                    }
                    HEAD = temp;
                } finally {
                    this.variables = backup;
                }
                contains = true;
                break;
            }
        if (!contains)
            throw new InterpretationException("Cannot find function '" + name + "'");
        return result;
    }

    private Object calculateFunction(String name, Map<String, Object> variables) throws InterpretationException {
        boolean contains = false;
        Object result = null;
        for (Function func : functions)
            if (func.name.equals(name)) {
                boolean equals = false;
                for (String variable : variables.keySet()) {
                    for (String var : func.variables)
                        if (var.equals(variable)) {
                            equals = true;
                            break;
                        }
                    if (!equals)
                        break;
                }
                if (variables.size() == 0)
                    equals =  true;
                if (equals) {
                    Map<String, Object> backup = new HashMap<>(this.variables);
                    try {
                        for (String var : func.variables)
                            this.variables.put(var, variables.get(var));
                        Token temp = HEAD;
                        HEAD = func.body;
                        while (!HEAD.type().equals(RCB) && !HEAD.type().equals(RETURN)) {
                            execute();
                            if (HEAD.type().equals(SEMICOLON))
                                HEAD = HEAD.next();
                        }
                        if (HEAD.type().equals(RETURN)) {
                            HEAD = HEAD.next();
                            result = calculateExpression("when return value calculating");
                        }
                        HEAD = temp;
                    } finally {
                        this.variables = backup;
                    }
                    contains = true;
                    break;
                }
            }
        if (!contains)
            throw new InterpretationException("Cannot find function '" + name + "'");
        return result;
    }

    private Result calculateExpression() throws InterpretationException {
        List<Object> expression = new ArrayList<>();
        Stack<Token> operations = new Stack<>();
        Map<TokenType, Integer> priority = new HashMap<TokenType, Integer>() {{
            put(XOR, 5);
            put(MOD, 5);
            put(MULTIPLY, 4);
            put(DIVIDE, 4);
            put(SUBTRACT, 3);
            put(ADD, 3);
            put(MORE_OR_EQUAL, 2);
            put(LESS_OR_EQUAL, 2);
            put(NOT_EQUAL, 2);
            put(EQUALS, 2);
            put(LESS, 2);
            put(MORE, 2);
            put(LOGICAL_AND, 1);
            put(LOGICAL_OR, 1);
            put(AND, 1);
            put(OR, 1);
            put(COLON, 0);
            put(LRB, 0);
            put(RRB, 0);
        }};
        TokenType last;
        int brackets_count = 0;
        int square_brackets_count = 0;
        for (; ; HEAD = HEAD.next()) {
            if (HEAD == null) {
                last = null;
                break;
            } else if (
                    (HEAD.type().equals(RRB) && brackets_count == 0)
                            || (HEAD.type().equals(RSB) && square_brackets_count == 0)
                            || HEAD.type().equals(COMA)
                            || HEAD.type().equals(LCB)
                            || HEAD.type().equals(SEMICOLON)
                            || HEAD.type().equals(RCB)
                    ) {
                last = HEAD.type();
                if (last == COMA)
                    HEAD = HEAD.next();
                break;
            } else if (
                    HEAD.type().equals(LITERAL)
                            || HEAD.type().equals(STRING)
                            || HEAD.type().equals(DOUBLE)
                            || HEAD.type().equals(BOOLEAN)
                            || HEAD.type().equals(NULL)
                    )
                expression.add(parseType(HEAD));
            else if (HEAD.type().equals(FUNCTION)) {
                expression.add(calculateFunction());
                if (HEAD == null) {
                    last = null;
                    break;
                }
            } else if (HEAD.type().equals(LSB) && square_brackets_count == 0) {
                HEAD = HEAD.next();
                List<Object> elements = new ArrayList<>();
                Result result;
                do {
                    result = calculateExpression();
                    if (result == null) {
                        break;
                    } else {
                        elements.add(result.result);
                    }
                } while (result.last != RSB);
                expression.add(elements);
            } else if (HEAD.type().equals(LCB))
                square_brackets_count++;
            else if (HEAD.type().equals(RCB))
                square_brackets_count--;
            else if (HEAD.type().equals(VARIABLE)) {
                Object value = variables.get(HEAD.value());
                if (!variables.containsKey(HEAD.value()))
                    throw new InterpretationException("Variable '" + HEAD.value() + "' wasn't declared");
                expression.add(value);
            } else if (HEAD.type().equals(RRB)) {
                brackets_count--;
                for (Token token; !(token = operations.pop()).type().equals(LRB); )
                    expression.add(token);
            } else if (HEAD.type().equals(LRB)) {
                brackets_count++;
                operations.push(HEAD);
            } else {
                int curr_op_pr = priority.get(HEAD.type());
                int stack_op_pr = operations.empty() ? -1 : priority.get(operations.peek().type());
                if (curr_op_pr > stack_op_pr)
                    operations.push(HEAD);
                else {
                    expression.add(operations.pop());
                    while (!operations.empty() && priority.get(operations.peek().type()) >= curr_op_pr)
                        expression.add(operations.pop());
                    operations.push(HEAD);
                }
            }
        }
        if (!operations.empty())
            for (Token token; !operations.empty() && (token = operations.pop()) != null; )
                expression.add(token);
        Stack<Object> execute = new Stack<>();
        for (Object obj : expression)
            if (obj instanceof Token) {
                assert !execute.empty();
                Object first_operand = execute.pop();
                Object second_operand = execute.pop();
                String funcName = ((Token) obj).value();
                try {
                    execute.push(tfp.func(funcName, second_operand, first_operand));
                } catch (NoSuchMethodException e) {
                    throw new InterpretationException("Cant't find or access '" + funcName + "' function");
                }
            } else {
                execute.push(obj);
            }
        if (!execute.empty())
            return new Result(last, execute.pop());
        else
            return null;
    }

    private Object calculateExpression(String text) throws InterpretationException {
        Result result = calculateExpression();
        if (result == null)
            throw new InterpretationException("Empty expression expected " + text);
        else
            return result.result;
    }

    private boolean subSeq(TokenType[] types) {
        Token HEAD = this.HEAD;
        for (int i = 0; i < types.length; i++, HEAD = HEAD.next())
            if (!HEAD.type().equals(types[i]))
                return false;
        return true;
    }

    private void executeLoopIf(boolean condition) throws InterpretationException {
        if (HEAD.type().equals(LCB)) {
            if (condition) {
                next(1);
                while (!HEAD.type().equals(RCB))
                    execute();
            } else {
                for (; ; HEAD = HEAD.next())
                    if (HEAD.type().equals(RCB))
                        break;
            }
            HEAD = HEAD.next();
        } else {
            if (condition)
                execute();
            else {
                for (; ; HEAD = HEAD.next())
                    if (HEAD.type().equals(SEMICOLON))
                        break;
                HEAD = HEAD.next();
            }
        }
    }

    public void executeScript() throws InterpretationException {
        while (HEAD != null)
            execute();
    }

    private void execute() throws InterpretationException {
        if (HEAD == null)
            throw new InterpretationException("Interpretation is already completed");
        if (subSeq(new TokenType[]{VARIABLE, EQUAL})) {
            String name = HEAD.value();
            HEAD = HEAD.next().next();
            variables.put(name, Objects.requireNonNull(calculateExpression()).result);
            HEAD = HEAD.next();
            return;
        }
        if (subSeq(new TokenType[]{FUNCTION})) {
            int length = 0;
            int brackets = 0;
            for (Token temp = HEAD; ; temp = temp.next()) {
                if (temp.type().equals(RRB))
                    brackets++;
                else if (temp.type().equals(LRB))
                    brackets--;
                else if (temp.type().equals(EQUAL) && brackets == 0)
                    break;
                else if (temp.type().equals(SEMICOLON)) {
                    length = -1;
                    break;
                }
                length++;
            }
            if (length == -1) {
                calculateFunction();
                HEAD = HEAD.next();
                if (HEAD != null)
                    HEAD = HEAD.next();
            } else {
                String name = HEAD.value();
                HEAD = HEAD.next().next();
                List<String> params = new ArrayList<>();
                for (int i = 0; i < length - 3; HEAD = HEAD.next(), i++)
                    if (HEAD.type().equals(VARIABLE))
                        params.add(HEAD.value());
                HEAD = HEAD.next().next().next();
                functions.add(new Function(name, params, HEAD));
                for (; ; HEAD = HEAD.next())
                    if (HEAD.type().equals(RCB))
                        break;
                HEAD = HEAD.next();
            }
            return;
        }
        if (subSeq(new TokenType[]{IF})) {
            HEAD = HEAD.next().next();
            Boolean condition = (boolean) calculateExpression("when if condition calculating");
            HEAD = HEAD.next();
            executeLoopIf(condition);
            if (HEAD.type().equals(ELSE)) {
                HEAD = HEAD.next();
                executeLoopIf(!condition);
            }
            return;
        }
        if (subSeq(new TokenType[]{WHILE})) {
            HEAD = HEAD.next().next();
            Token condition = HEAD;
            boolean result = (boolean) calculateExpression("when while condition calculating");
            while (result) {
                executeLoopIf(true);
                HEAD = condition;
                result = (boolean) calculateExpression("when while condition calculating");
                HEAD = HEAD.next();
            }
            return;
        }
        if (subSeq(new TokenType[]{FOR})) {
            HEAD = HEAD.next().next();
            execute();
            Token condition = HEAD;
            boolean result = (boolean) calculateExpression("when for condition calculating");
            HEAD = HEAD.next();
            Token iterator = HEAD;
            for (; ; HEAD = HEAD.next())
                if (HEAD.type().equals(RRB))
                    break;
            Token body = HEAD.next();
            while (result) {
                HEAD = body;
                executeLoopIf(true);
                HEAD = iterator;
                execute();
                HEAD = condition;
                result = (boolean) calculateExpression("when for condition calculating");
            }
            HEAD = body;
            if (HEAD.type().equals(LCB)) {
                for (; ; HEAD = HEAD.next())
                    if (HEAD.type().equals(RCB))
                        break;
            } else {
                for (; ; HEAD = HEAD.next())
                    if (HEAD.type().equals(SEMICOLON))
                        break;
            }
            HEAD = HEAD.next();
            return;
        }
        if (subSeq(new TokenType[]{DO_WHILE})) {
            HEAD = HEAD.next().next();
            Token condition = HEAD;
            boolean result = true;
            for (; ; HEAD = HEAD.next())
                if (HEAD.type().equals(RRB))
                    break;
            Token body = HEAD.next();
            while (result) {
                HEAD = body;
                executeLoopIf(true);
                HEAD = condition;
                result = (boolean) calculateExpression("when do_while condition calculating");
            }
            return;
        }
        HEAD = null;
    }

    private boolean next(int num) {
        for (int i = 0; i < num; i ++) {
            if (HEAD == null)
                return false;
            HEAD = HEAD.next();
        }
        return true;
    }

    private void nextTo(TokenType type) {
        int sb = 0;
        int cb = 0;
        int rb = 0;
        while (HEAD != null && !HEAD.type().equals(type) && sb == 0 && cb == 0 && rb == 0) {
            if (HEAD.type() == LRB)
                rb ++;
            else if (HEAD.type() == RRB)
                rb --;
            else if (HEAD.type() == LCB)
                cb ++;
            else if (HEAD.type() == RCB)
                cb --;
            else if (HEAD.type() == LSB)
                sb ++;
            else if (HEAD.type() == RSB)
                sb --;
            next(1);
        }
    }
}
