package map.content.deadly;

import bot.VampusBot;
import map.cell.Cell;
import map.content.Content;
import map.player.Player;

public class Vampus implements Content {
    @Override
    public boolean enter(VampusBot bot, Player player, Cell current) {
        player.hit(bot, "Вас схватил вампус", 1);
        return false;
    }

    @Override
    public String icon() {
        return "\uD83D\uDE21";
    }
}