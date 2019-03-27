package map;

import com.google.common.base.MoreObjects;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class State implements Serializable {
    private String message;
    private List<List<Pair<String, String>>> buttons;

    public State(String message) {
        this.message = message;
        this.buttons = new ArrayList<>();
    }

    public String message() {
        return message;
    }

    public State setMessage(String message) {
        this.message = message;
        return this;
    }

    public State addRow(String... buttons) {
        List<Pair<String, String>> map = new ArrayList<>();
        this.buttons.add(map);
        for (String button : buttons) {
            String[] args = button.split(":");
            map.add(new Pair<>(args[0], args.length == 1? args[0] : args[1]));
        }
        return this;
    }

    public State addRow(List<Pair<String, String>> buttons) {
        List<Pair<String, String>> list = new ArrayList<>();
        this.buttons.add(list);
        list.addAll(buttons);
        return this;
    }

    public List<List<Pair<String, String>>> buttons() {
        return buttons;
    }

    @Override
    public State clone() {
        State state = new State(this.message);
        state.buttons = this.buttons;
        return state;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("message", "\n" + message)
                .add("buttons", buttons)
                .toString();
    }
}
