package script.execute;

import script.token.Token;
import script.token.TokenType;
import static script.token.TokenType.*;

public class Interpreter {
    private Token HEAD;

    public Interpreter(Token HEAD) {
        if (HEAD.typed())
            this.HEAD = HEAD;
        else
            this.HEAD = HEAD.next();
    }

    private boolean subSeq(TokenType[] types) {
        Token HEAD = this.HEAD;
        for (int i = 0; i < types.length; i++, HEAD = HEAD.next())
            if (!HEAD.type().equals(types[i]))
                return false;
        return true;
    }

    public void execute() throws InterpretationException {
        if (ended())
            throw new InterpretationException("Interpretation is already completed");
        if (subSeq(new TokenType[]{SEND})) {

        } else if (subSeq(new TokenType[]{EQUAL, RCB}));
    }

    private Token head() throws InterpretationException {
        if (ended())
            throw new InterpretationException("Interpretation is already completed");
        return HEAD;
    }

    private boolean ended() {
        return HEAD == null;
    }
}
