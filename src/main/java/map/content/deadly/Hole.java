package map.content.deadly;

import bot.VampusBot;
import map.content.Content;
import map.player.Player;

public class Hole implements Content {
    @Override
    public void enter(VampusBot bot, Player player) {
        player.kill();
    }

    @Override
    public String icon() {
        return "\uD83D\uDD73";
    }
}
