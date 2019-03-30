package map.content.chest;

import bot.VampusBot;
import map.Message;
import map.player.Player;

import java.io.Serializable;
import java.util.Map;

public abstract class Item implements Serializable {
    private String icon;
    private String description;

    protected Message message;
    protected Map drawProperty;

    public Item(String icon, String description) {
        this.icon = icon;
        this.description = description;
    }

    public abstract void changeState(VampusBot bot, Player player, String command);

    public abstract void defaultState(Player player);

    public String icon() {
        return icon;
    }

    public Message state() {
        return message;
    }

    public Map drawProperty() {
        return drawProperty;
    }

    public String description() {
        return description;
    }
}