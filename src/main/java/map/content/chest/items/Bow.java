package map.content.chest.items;

import bot.VampusBot;
import javafx.util.Pair;
import map.State;
import map.cell.Cell;
import map.content.chest.Item;
import map.content.deadly.Hole;
import map.content.deadly.Vampus;
import map.content.deadly.VampusInHole;
import map.player.Player;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Bow extends Item {

    private static Logger logger = Logger.getLogger(Bomb.class);

    public Bow() {
        super(
                "\uD83C\uDFF9",
                "Этот предмет стреляет в заданном направлении. " +
                        "Если он попадёт в вампуса, то убьёт его."
        );
        super.drawProperty = new HashMap();
    }

    @Override
    public void defaultState(Player player) {
        List<Pair<String, String>> items = new ArrayList<>();
        for (int index = 0; index < player.items().size(); index++)
            items.add(new Pair<>(player.items().get(index).icon(), "activate " + index));
        for (int i = 0; i < 10 - items.size(); i++)
            items.add(new Pair<>("∅", "∅"));
        super.state = new State(description())
                .addRow("↑:item ↑")
                .addRow("←:item ←", "→:item →")
                .addRow("↓:item ↓")
                .addRow("Отмена:cancel");
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        Cell cell;
        switch (command) {
            case "↑":
                cell = player.position().up();
                break;
            case "←":
                cell = player.position().left();
                break;
            case "→":
                cell = player.position().right();
                break;
            case "↓":
                cell = player.position().down();
                break;
            default:
                return;
        }
        logger.debug("Bow shot!");
        if (cell.content().getClass() == Vampus.class || cell.content().getClass() == VampusInHole.class) {
            bot.edit(new State("Вы убили вампуса!"), player.id(), player.gameInstance());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException exc) {
                exc.printStackTrace();
            }
            cell.deleteContent();
            if (cell.content().getClass() == Vampus.class)
                cell.setContent(new Hole());
            logger.info("Hitting vampus from bow!");
        }
        bot.edit(new State("Вы промахнулись!"), player.id(), player.gameInstance());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException exc) {
            exc.printStackTrace();
        }
        player.deleteItem(this);
    }
}
