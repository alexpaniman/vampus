package map.content.deadly;

import bot.VampusBot;
import map.cell.Cell;
import map.content.Content;
import map.player.Player;

public class Hole implements Content {
    @Override
    public boolean enter(VampusBot bot, Player player) {
        player.hit(bot, 1);
        return false;
    }

    @Override
    public String icon() {
        return "\uD83D\uDD73";
    }
}
