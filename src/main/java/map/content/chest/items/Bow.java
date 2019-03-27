package map.content.chest.items;

import bot.VampusBot;
import map.State;
import map.cell.Cell;
import map.content.chest.Item;
import map.content.deadly.Hole;
import map.content.deadly.Vampus;
import map.content.deadly.VampusInHole;
import map.player.Player;
import org.apache.log4j.Logger;

import java.util.HashMap;

public class Bow extends Item {

    private final static Logger logger = Logger.getLogger(Bow.class);

    public Bow() {
        super(
                "\uD83C\uDFF9",
                "Этот предмет стреляет в заданном направлении. " +
                        "Если он попадёт в вампуса, то убьёт его."
        );
    }

    @Override
    public void defaultState(Player player) {
        super.drawProperty = new HashMap<>();
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
        if (!cell.empty() && cell.content().getClass() == Vampus.class || cell.content().getClass() == VampusInHole.class) {
            bot.edit(new State("Вы убили вампуса!"), player.id(), player.gameInstance());
            bot.sleep(5);
            cell.deleteContent();
            if (cell.content().getClass() == Vampus.class)
                cell.setContent(new Hole());
            logger.info("Hitting vampus from bow!");
        }
        bot.edit(new State("Вы промахнулись!"), player.id(), player.gameInstance());
        bot.sleep(5);
        player.deleteItem(this);
    }
}
