package map.content.deadly;

import bot.VampusBot;
import map.cell.Cell;
import map.content.Content;
import map.player.Player;

public class VampusInHole implements Content {
    @Override
    public boolean enter(VampusBot bot, Player player, Cell current) {
        player.hit(bot, "Вы упали в яму, но до того, как вы достигли дна вас схватил вампус", 2);
        return false;
    }

    @Override
    public String icon() {
        return "\uD83E\uDD2C";
    }
}
