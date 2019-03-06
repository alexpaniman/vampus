package script.analysis;

import script.token.Token;
import script.token.TokenType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static script.token.TokenType.*;

@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess"})
public class Lexer {

    public static void main(String[] args) throws IOException, LexerException {
        StringBuilder sb = new StringBuilder();
        for (String str : Files.readAllLines(new File("src/main/resources/test_script.tsc").toPath()))
            sb.append(str).append("\n");
        Token token = new Lexer().tokenize(sb.toString());
        while ((token = token.next()) != null)
            System.out.println("Тип = " + token.type() + ". Значение = '" + token.value() + "'");
    }

    private boolean subSeq(char[] arr, int start, String str) {
        for (int i = 0, j = start; i < str.length(); i ++, j ++)
            if (arr[j] != str.charAt(i))
                return false;
        return true;
    }

    public Token tokenize(String text) throws LexerException {
        Token HEAD = Token.EMPTY.get();

        Token token = HEAD;

        char[] charArray = text.toCharArray();

        for (int i = 0; i < charArray.length; i++) {
            char sym = charArray[i];

            if (sym == ' ' || sym == '\t' || sym == '\n')
                continue;

            if (sym == '#') {
                for (int j = i; j < charArray.length; j++) {
                    if (charArray[j] == '\n')
                        break;
                    else
                        i++;
                }
                continue;
            }

            if (sym == '\'') {
                StringBuilder string = new StringBuilder();
                boolean closed = false;
                for (int j = i + 1; j < charArray.length; j++)
                    if (charArray[j] == '\'') {
                        closed = true;
                        break;
                    } else
                        string.append(charArray[j]);
                int line = 0;
                for (int j = 0; j < i; j++)
                    if (charArray[j] == '\n')
                        line++;
                if (!closed)
                    throw new LexerException("Can't find end of string, which begins on " + (line + 1) + " line: " + string + "...");
                token.setNext(new Token(STRING, string.toString()));
                token = token.next();
                i += string.length() + 1;
                continue;
            }

            if (Character.isDigit(sym)) {
                StringBuilder literal = new StringBuilder();
                for (int j = i; j < charArray.length; j++)
                    if (Character.isDigit(charArray[j]) || charArray[j] == '.')
                        literal.append(charArray[j]);
                    else
                        break;
                try {
                    Integer.parseInt(literal.toString());
                    token.setNext(new Token(LITERAL, literal.toString()));
                } catch (NumberFormatException nfe) {
                    try {
                        Double.parseDouble(literal.toString());
                        token.setNext(new Token(DOUBLE, literal.toString()));
                    } catch (NumberFormatException exc) {
                        throw new LexerException("Illegal number format: " + literal.toString());
                    }
                }
                token = token.next();
                i += literal.length() - 1;
                continue;
            }

            if (Character.toString(sym).matches("^[A-Za-z_]$")) {
                if (subSeq(charArray, i, "for")) {
                    token.setNext(new Token(FOR, "for"));
                    token = token.next();
                    i += 2;
                    continue;
                } else if (subSeq(charArray, i, "while")) {
                    token.setNext(new Token(WHILE, "while"));
                    token = token.next();
                    i += 4;
                    continue;
                } else if (subSeq(charArray, i, "if")) {
                    token.setNext(new Token(IF, "if"));
                    token = token.next();
                    i ++;
                    continue;
                } else if (subSeq(charArray, i, "switch")) {
                    token.setNext(new Token(SWITCH, "switch"));
                    token = token.next();
                    i += 5;
                    continue;
                } else if (subSeq(charArray, i, "true")) {
                    token.setNext(new Token(BOOLEAN, "true"));
                    token = token.next();
                    i += 3;
                    continue;
                } else if (subSeq(charArray, i, "false")) {
                    token.setNext(new Token(BOOLEAN, "false"));
                    token = token.next();
                    i += 4;
                    continue;
                } else if (subSeq(charArray, i, "else")) {
                    token.setNext(new Token(ELSE, "else"));
                    token = token.next();
                    i += 3;
                    continue;
                } else if (subSeq(charArray, i, "do_while")) {
                    token.setNext(new Token(DO_WHILE, "do_while"));
                    token = token.next();
                    i += 7;
                    continue;
                } else if (subSeq(charArray, i, "and")) {
                    token.setNext(new Token(LOGICAL_AND, "and"));
                    token = token.next();
                    i += 2;
                    continue;
                } else if (subSeq(charArray, i, "or")) {
                    token.setNext(new Token(LOGICAL_OR, "or"));
                    token = token.next();
                    i ++;
                    continue;
                } else if (subSeq(charArray, i, "return")) {
                    token.setNext(new Token(RETURN, "return"));
                    token = token.next();
                    i += 5;
                    continue;
                } else if (subSeq(charArray, i, "null")) {
                    token.setNext(new Token(NULL, "null"));
                    token = token.next();
                    i += 3;
                    continue;
                }
                StringBuilder variable = new StringBuilder();
                for (int j = i; j < charArray.length; j++)
                    if (Character.toString(charArray[j]).matches("^[A-Za-z\\d_]$"))
                        variable.append(charArray[j]);
                    else
                        break;
                for (int j = variable.length() + i; j < charArray.length; j++)
                    if (charArray[j] == '(') {
                        token.setNext(new Token(FUNCTION, variable.toString()));
                        break;
                    } else if (charArray[j] != ' ' && charArray[j] != '\n' && charArray[j] != '\t')
                        break;
                if (token.next() == null)
                    token.setNext(new Token(VARIABLE, variable.toString()));
                token = token.next();
                i += variable.length() - 1;
                continue;
            }

            if (subSeq(charArray, i, "!=")) {
                token.setNext(new Token(NOT_EQUAL, "!="));
                token = token.next();
                i ++;
                continue;
            } else if (subSeq(charArray, i, "==")) {
                token.setNext(new Token(EQUALS, "=="));
                token = token.next();
                i ++;
                continue;
            } else if (subSeq(charArray, i, "|")) {
                token.setNext(new Token(OR, "|"));
                token = token.next();
                continue;
            } else if (subSeq(charArray, i, "&")) {
                token.setNext(new Token(AND, "&"));
                token = token.next();
                continue;
            } else if (subSeq(charArray, i, ">=")) {
                token.setNext(new Token(MORE_OR_EQUAL, ">="));
                token = token.next();
                i ++;
                continue;
            } else if (subSeq(charArray, i, "<=")) {
                token.setNext(new Token(LESS_OR_EQUAL, "<="));
                token = token.next();
                i ++;
                continue;
            } else if (subSeq(charArray, i, ">")) {
                token.setNext(new Token(MORE, ">"));
                token = token.next();
                continue;
            } else if (subSeq(charArray, i, "<")) {
                token.setNext(new Token(LESS, "<"));
                token = token.next();
                continue;
            } else if (subSeq(charArray, i, "%")) {
                token.setNext(new Token(MOD, "%"));
                token = token.next();
                continue;
            } else if (subSeq(charArray, i, "^")) {
                token.setNext(new Token(XOR, "^"));
                token = token.next();
                continue;
            }

            switch (sym) {
                case '(':
                    token.setNext(new Token(LRB, "("));
                    token = token.next();
                    continue;
                case ')':
                    token.setNext(new Token(RRB, ")"));
                    token = token.next();
                    continue;
                case '{':
                    token.setNext(new Token(LCB, "{"));
                    token = token.next();
                    continue;
                case '}':
                    token.setNext(new Token(RCB, "}"));
                    token = token.next();
                    continue;
                case '[':
                    token.setNext(new Token(LSB, "["));
                    token = token.next();
                    continue;
                case ']':
                    token.setNext(new Token(RSB, "]"));
                    token = token.next();
                    continue;
                case ':':
                    token.setNext(new Token(COLON, ":"));
                    token = token.next();
                    continue;
                case ';':
                    token.setNext(new Token(SEMICOLON, ";"));
                    token = token.next();
                    continue;
                case ',':
                    token.setNext(new Token(COMA, ","));
                    token = token.next();
                    continue;
                case '*':
                    token.setNext(new Token(MULTIPLY, "*"));
                    token = token.next();
                    continue;
                case '=':
                    token.setNext(new Token(EQUAL, "="));
                    token = token.next();
                    continue;
                case '/':
                    token.setNext(new Token(DIVIDE, "/"));
                    token = token.next();
                    continue;
                case '-':
                    token.setNext(new Token(SUBTRACT, "-"));
                    token = token.next();
                    continue;
                case '+':
                    token.setNext(new Token(ADD, "+"));
                    token = token.next();
                    continue;
            }

            StringBuilder str = new StringBuilder();
            int line = 0;
            for (int j = 0; j < i; j++)
                if (charArray[j] == '\n')
                    line++;
            for (int j = i; j <= Math.min(i + 10, charArray.length - 1); j++) {
                if (charArray[j] == '\n')
                    break;
                str.append(charArray[j]);
            }
            if (str.length() < charArray.length - 1)
                str.append("...");
            throw new LexerException("Unexpected symbol on line " + (line + 1) + " ==> " + str);
        }

        return HEAD;
    }
}
