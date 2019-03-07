package bot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

public class ScriptBot extends TelegramLongPollingBot {
    private final Map<Long, Stack<Update>> updates;
    private Class<? extends Runnable> processor;
    private String username;
    private String token;

    public static void main(String[] args) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi tba = new TelegramBotsApi();
        tba.registerBot(
                new ScriptBot(
                        ThreadInterpreter.class,
                        ***REMOVED***,
                        ***REMOVED***
                )
        );
    }

    public ScriptBot(Class<? extends Runnable> processor, String username, String token) {
        this.processor = processor;
        this.username = username;
        this.token = token;
        this.updates = new HashMap<>();
    }

    @Override
    public void onUpdateReceived(Update update) {
        Function<? super Long, ? extends Stack<Update>> func = str -> {
            final Stack<Update> stack = new Stack<>();
            try {
                new Thread(
                        processor
                                .getConstructor(TelegramLongPollingBot.class, Stack.class)
                                .newInstance(this, stack)
                ).start();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exc) {
                exc.printStackTrace();
            }
            return stack;
        };
        if (update.hasMessage()) {
            updates.computeIfAbsent(update.getMessage().getChatId(), func).push(update);
        } else
            updates.computeIfAbsent(update.getCallbackQuery().getMessage().getChatId(), func).push(update);
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
