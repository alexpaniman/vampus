package map.content;

import bot.VampusBot;
import map.Message;
import map.player.Player;

import java.io.Serializable;

public interface Content extends Serializable {
    default void changeState(VampusBot bot, Player player, String command) {}

    default boolean enter(VampusBot bot, Player player) {
        return true;
    }

    default Message message() {
        return null;
    }

    String icon();
}