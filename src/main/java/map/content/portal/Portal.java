package map.content.portal;

import bot.VampusBot;
import map.State;
import map.cell.Cell;
import map.content.Content;
import map.player.Player;

public class Portal implements Content {
    private Cell dest;

    public void teleport(Player player) {
        player.setPos(dest);
    }

    public void setDest(Cell dest) {
        this.dest = dest;
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        switch (command) {
            case "teleport":
                teleport(player);
                break;
        }
    }

    @Override
    public State state() {
        return new State("Вы находитесь рядом с порталом!").addRow("Зайти в портал:content teleport");
    }

    @Override
    public String icon() {
        return "\uD83D\uDCAE";
    }
}
