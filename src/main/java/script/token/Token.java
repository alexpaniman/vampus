package script.token;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;

@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess"})
public class Token {
    public static final Supplier<Token> EMPTY = Token::new;
    private TokenType type;
    private String value;
    private Token next;

    private Token() {

    }

    public Token(TokenType type, String value) {
        checkArgument(type != null);
        checkArgument(value != null);
        this.value = value;
        this.type = type;
        this.next = null;
    }

    public Token setNext(Token next) {
        this.next = next;
        return this;
    }

    public TokenType type(){
        return type;
    }

    public boolean typed() {
        return type != null;
    }

    public String value() {
        return value;
    }

    public Token next() {
        return next;
    }
}
