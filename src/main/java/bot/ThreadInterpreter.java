package bot;

import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import script.analysis.Lexer;
import script.analysis.LexerException;
import script.commands.TFProcessor;
import script.interpreter.InterpretationException;
import script.interpreter.Interpreter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Stack;

public class ThreadInterpreter implements Runnable {
    private final Stack<Update> updateStack;
    private final TelegramLongPollingBot bot;

    public ThreadInterpreter(TelegramLongPollingBot bot, Stack<Update> updateStack) {
        this.updateStack = updateStack;
        this.bot = bot;
    }

    private synchronized Update next() {
        while (updateStack.empty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return updateStack.pop();
    }

    @Override
    public void run() {
        while (true) {
            StringBuilder text = new StringBuilder();
            try {
                for (String line : Files.readAllLines(new File("src/main/resources/test_script.tsc").toPath()))
                    text.append(line).append("\n");
                new Interpreter(
                        new TFProcessor(
                                new TelegramInterpreter(bot)
                        ),
                        new Lexer().tokenize(text.toString())
                ).setupVar("current_chat", next().getMessage().getChatId()).executeScript();
            } catch (InterpretationException | IOException | LexerException exc) {
                exc.printStackTrace();
            }
        }
    }
}
