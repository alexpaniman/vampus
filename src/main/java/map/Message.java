package map;

import com.google.common.base.MoreObjects;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    private String message;
    private List<List<Pair<String, String>>> buttons;

    public Message(String message) {
        this.message = message;
        this.buttons = new ArrayList<>();
    }

    public String message() {
        return message;
    }

    public Message setMessage(String message) {
        this.message = message;
        return this;
    }

    public Message addRow(String... buttons) {
        List<Pair<String, String>> map = new ArrayList<>();
        this.buttons.add(map);
        for (String button : buttons) {
            String[] args = button.split(":");
            map.add(new Pair<>(args[0], args.length == 1? args[0] : args[1]));
        }
        return this;
    }

    public Message addRow(List<Pair<String, String>> buttons) {
        List<Pair<String, String>> list = new ArrayList<>();
        this.buttons.add(list);
        list.addAll(buttons);
        return this;
    }

    public List<List<Pair<String, String>>> buttons() {
        return buttons;
    }

    @Override
    public Message clone() {
        Message message = new Message(this.message);
        message.buttons = this.buttons;
        return message;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("message", "\n" + message)
                .add("buttons", buttons)
                .toString();
    }
}
