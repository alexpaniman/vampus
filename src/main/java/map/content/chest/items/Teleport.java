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
                "Это телепорт, он может переместить вас в пустую клетку."
        );
    }

    @Override
    public void defaultState(Player player) {
        super.drawProperty = new HashMap<>();
        super.message = new Message(description())
                .addRow("Телепортироваться:item teleport")
                .addRow("Отмена:cancel");
    }

    private boolean goodCell(Cell cell) {
        return cell == null || cell.content() == null || cell.content().getClass() == Chest.class || cell.content().getClass() == Portal.class;
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        if ("teleport".equals(command)) {
            Cell level = player.position().level();
            List<Cell> upList = new ArrayList<>();
            Cell cell = level;
            do
                upList.add(cell);
            while ((cell = cell.right()) != null);
            Collections.shuffle(upList);
            for (Cell c : upList) {
                cell = c;
                while (cell != null) {
                    if (!cell.contains(player))
                        if (cell.content() == null || cell.content().getClass() == Chest.class || cell.content().getClass() == Portal.class)
                            if (goodCell(cell.up()) && goodCell(cell.down()) && goodCell(cell.right()) && goodCell(cell.left())) {
                                player.teleport(cell);
                                player.deleteItem(this);
                                return;
                            }
                    cell = cell.down();
                }
            }
            player.message(bot, "Такой клетки нет!", 5);

        }
    }
}
