package map.content.portal;

import bot.VampusBot;
import map.Message;
import map.cell.Cell;
import map.content.Content;
import map.player.Player;

public class Portal implements Content {
    private Cell dest;

    public void setDest(Cell dest) {
        this.dest = dest;
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        switch (command) {
            case "teleport":
                player.teleport(dest);
                break;
        }
    }

    @Override
    public Message message() {
        return new Message("Вы находитесь рядом с порталом!").addRow("Зайти в портал:content teleport");
    }

    @Override
    public String icon() {
        return "\uD83D\uDCAE";
    }
}
