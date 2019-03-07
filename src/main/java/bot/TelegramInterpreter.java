package bot;

import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import script.interpreter.InterpretationException;
import script.commands.CommandInterpreter;
import script.commands.TelescriptFunction;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class TelegramInterpreter extends CommandInterpreter {
    private TelegramLongPollingBot bot;

    TelegramInterpreter(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    private InlineKeyboardMarkup processInlineMarkup(ArrayList<ArrayList<Pair<String, String>>> inline) {
        InlineKeyboardMarkup ikm = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (List<Pair<String, String>> list : inline) {
            for (Pair<String, String> button : list) {
                row.add(new InlineKeyboardButton().setText(button.getKey()).setCallbackData(button.getValue()));
            }
            keyboard.add(row);
            row = new ArrayList<>();
        }
        ikm.setKeyboard(keyboard);
        return ikm;
    }

    @TelescriptFunction(name = "send", params = {
            "text",
            "chat",
            "reply_to",
            "inline",
            "reply",
            "resize",
            "one_time"
    })
    public void send(
            String text,
            Long chat,
            Long reply_to,
            ArrayList<ArrayList<Pair<String, String>>> inline,
            ArrayList<ArrayList<String>> reply,
            Boolean resize,
            Boolean one_time
    ) throws InterpretationException {
        if (text == null)
            throw new InterpretationException("Can't send message without text!");
        if (chat == null)
            throw new InterpretationException("Can't send to unknown chat!");
        if (reply != null && inline != null)
            throw new InterpretationException("Can't send message with reply and inline markup!");
        SendMessage sendMessage = new SendMessage().setText(text).setChatId(chat);
        if (reply_to != null)
            sendMessage.setReplyToMessageId((int) (long) reply_to);
        if (inline != null)
            sendMessage.setReplyMarkup(processInlineMarkup(inline));
        if (reply != null) {
            ReplyKeyboardMarkup rkm = new ReplyKeyboardMarkup();
            List<KeyboardRow> keyboard = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            for (List<String> row : reply) {
                for (String button : row)
                    keyboardRow.add(button);
                keyboard.add(keyboardRow);
                keyboardRow = new KeyboardRow();
            }
            rkm.setKeyboard(keyboard);
            rkm.setResizeKeyboard(resize == null ? true : resize);
            rkm.setOneTimeKeyboard(one_time == null ? false : one_time);
            sendMessage.setReplyMarkup(rkm);
        }
        try {
            bot.sendMessage(sendMessage);
        } catch (TelegramApiException exc) {
            throw new InterpretationException("Unable to send message!");
        }
    }
}