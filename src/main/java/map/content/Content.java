package map.content;

import bot.VampusBot;
import map.State;
import map.player.Player;

import java.io.Serializable;

public interface Content extends Serializable {
    default void changeState(VampusBot bot, Player player, String command) {}

    default void enter(VampusBot bot, Player player) {}

    default State state() {
        return null;
    }

    String icon();
}