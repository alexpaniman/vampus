package map.content.chest.items;

import bot.VampusBot;
import map.Message;
import map.cell.Cell;
import map.content.chest.Chest;
import map.content.chest.Item;
import map.content.portal.Portal;
import map.player.Player;

import java.util.*;

public class Teleport extends Item {
    public Teleport() {
        super(
                "⭖",
                "Это телепорт, он может переместить вас в ближайшую пустую клетку."
        );
    }

    @Override
    public void defaultState(Player player) {
        super.drawProperty = new HashMap<>();
        super.message = new Message(description())
                .addRow("Телепортироваться:item teleport")
                .addRow("Отмена:cancel");
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        switch (command) {
            case "teleport":
                List<Cell> cells = Arrays.asList(
                        player.position().up(),
                        player.position().left(),
                        player.position().right(),
                        player.position().down()
                );
                Collections.shuffle(cells);
                for (Cell cell: cells) {
                    Class c = cell.empty()? null : cell.content().getClass();
                    if (c == null || c == Chest.class || c == Portal.class) {
                        player.teleport(cell);
                        return;
                    }
                }
                player.deleteItem(this);
                break;
        }
    }
}
