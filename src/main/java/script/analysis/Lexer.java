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
        System.out.println(sb.toString().replaceAll("[ \t\n]", ""));
        System.out.println();
        while ((token = token.next()) != null)
            System.out.print(//"Тип = " + token.type() + ". Значение = '" +
                    token.value() //+ "'");
            );
    }

    public int subStr(Token token, TokenType type, String str, char[] chars, int j, int old) {
        if (old != j)
            return j;
        for (int i = j, h = 0; h < str.length() && i < chars.length; h++, i++)
            if (chars[i] != str.charAt(h))
                return j;
        token.setNext(new Token(type, str));
        return j + str.length();
    }

    public Token tokenize(String text) throws LexerException {
        Token HEAD = Token.EMPTY.get();

        Token token = HEAD;

        char[] charArray = text.toCharArray();

        boolean readString = false;
        boolean readLiteral = false;
        boolean readVariable = false;

        StringBuilder literal = new StringBuilder();
        StringBuilder string = new StringBuilder();
        StringBuilder variable = new StringBuilder();

        for (int i = 0, line = 0; i < charArray.length; i++) {
            char sym = charArray[i];

            if (sym == '\n')
                line++;

            if (readString && sym == '\'') {
                token.setNext(new Token(STRING, string.toString()));
                token = token.next();

                string = new StringBuilder();

                readString = false;
                continue;
            }

            if (sym == '\'') {
                readString = true;
                continue;
            }

            if (readString) {
                string.append(sym);
                continue;
            }

            if (sym == '#')
                for (int j = i; j < charArray.length; j ++)
                    if (charArray[j] == '\n')
                        break;
                    else
                        i ++;

            if (readLiteral && String.valueOf(sym).matches("[^\\d]")) {
                token.setNext(new Token(LITERAL, literal.toString()));
                token = token.next();

                literal = new StringBuilder();

                readLiteral = false;
            }

            if (String.valueOf(sym).matches("\\d")) {
                readLiteral = true;

                literal.append(sym);
                continue;
            }

            if (readVariable && String.valueOf(sym).matches("[^A-Za-z]")) {
                token.setNext(new Token(LITERAL, literal.toString()));
                token = token.next();

                variable = new StringBuilder();

                readVariable = false;
            }

            if (readVariable) {
                token.setNext(new Token(VARIABLE, variable.toString()));
                token = token.next();

                variable = new StringBuilder();
            }

            final int old = i;

            i = subStr(token, SEND, "SEND", charArray, i, old);
            i = subStr(token, TEXT, "TEXT", charArray, i, old);
            i = subStr(token, PHOTO, "PHOTO", charArray, i, old);
            i = subStr(token, REPLY_TO, "REPLY_TO", charArray, i, old);
            i = subStr(token, INLINE_MARKUP, "INLINE_MARKUP", charArray, i, old);
            i = subStr(token, REPLY_MARKUP, "REPLY_MARKUP", charArray, i, old);
            i = subStr(token, THIS, "THIS", charArray, i, old);
            i = subStr(token, EDIT, "EDIT", charArray, i, old);
            i = subStr(token, USER, "USER", charArray, i, old);
            i = subStr(token, EQUAL, "=", charArray, i, old);
            i = subStr(token, COLON, ":", charArray, i, old);
            i = subStr(token, IF, "IF", charArray, i, old);
            i = subStr(token, EQUALS, "EQUALS", charArray, i, old);
            i = subStr(token, MATCHES, "MATCHES", charArray, i, old);
            i = subStr(token, COMA, ",", charArray, i, old);
            i = subStr(token, LRB, "(", charArray, i, old);
            i = subStr(token, RRB, ")", charArray, i, old);
            i = subStr(token, RCB, "}", charArray, i, old);
            i = subStr(token, LCB, "{", charArray, i, old);
            i = subStr(token, EXPECT_ANSWER, "EXPECT_ANSWER", charArray, i, old);
            i = subStr(token, CALLBACK_QUERY, "CALLBACK_QUERY", charArray, i, old);
            i = subStr(token, WHILE, "WHILE", charArray, i, old);
            i = subStr(token, FOR, "FOR", charArray, i, old);
            i = subStr(token, LCB, "{", charArray, i, old);
            i = subStr(token, STAR, "*", charArray, i, old);
            i = subStr(token, OR, "|", charArray, i, old);

            if (old == i && String.valueOf(sym).matches("[A-Za-z]")) {
                readVariable = true;
                variable.append(sym);
                continue;
            }

            if (old != i) {
                token = token.next();
                i--;
            } else if (charArray[old] != '\n' && charArray[old] != '\t' && charArray[old] != ' ') {
                StringBuilder str = new StringBuilder();
                for (int j = old; j <= Math.min(old + 10, charArray.length); j++) {
                    if (j == old)
                        str.append("Unexpected symbol --> ");
                    if (charArray[j] != ' ' && charArray[j] != '\t' && charArray[j] != '\n')
                        str.append(charArray[j]);
                }
                str.insert(22, "[");
                str.insert(24, "]");
                if (old + 10 <= charArray.length - 1)
                    str.append("...");
                throw new LexerException("Unexpected symbol on line " + line + ": " + str);
            }
        }

        if (literal.length() > 0)
            token.setNext(new Token(LITERAL, literal.toString()));

        return HEAD;
    }
}
