package map.content.chest.items;

import bot.VampusBot;
import map.Message;
import map.cell.Cell;
import map.content.chest.Item;
import map.content.deadly.Hole;
import map.content.deadly.Vampus;
import map.content.deadly.VampusInHole;
import map.player.Player;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.function.UnaryOperator;

public class Rifle extends Item {
    private static final Logger logger = Logger.getLogger(Rifle.class);

    public Rifle() {
        super(
                "🔫",
                "Это ружьё, оно стреляет в выбраном направлении.\n" +
                        "Пуля летит до тех пор, пока не убьёт вампуса или не врежется в стену."
        );
    }

    @Override
    public void defaultState(Player player) {
        super.drawProperty = new HashMap<>();
        super.message = new Message(description())
                .addRow("↑:item ↑")
                .addRow("←:item ←", "→:item →")
                .addRow("↓:item ↓")
                .addRow("Отмена:cancel");
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        UnaryOperator<Cell> operator;
        String arrow;
        switch (command) {
            case "↑":
                operator = Cell::up;
                arrow = "⬆";
                break;
            case "←":
                operator = Cell::left;
                arrow = "⬅";
                break;
            case "→":
                operator = Cell::right;
                arrow = "➡";
                break;
            case "↓":
                operator = Cell::down;
                arrow = "⬇";
                break;
            default:
                return;
        }
        Cell cell = player.position();
        while ((cell = operator.apply(cell)) != null) {
            final Cell finalCell = cell;
            super.drawProperty = new HashMap<Cell, String>() {{
                put(finalCell, arrow);
            }};
            player.instance(bot);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException exc) {
                logger.error(exc);
            }
            if (!cell.empty()) {
                if (cell.content().getClass() == Vampus.class || cell.content().getClass() == VampusInHole.class) {
                    player.message(bot, "Попадание в вампуса!", 5);
                    if (cell.content().getClass() == Vampus.class)
                        cell.deleteContent();
                    else {
                        cell.deleteContent();
                        cell.setContent(new Hole());
                    }
                    break;
                }
            } else {
                super.drawProperty = new HashMap<>();
            }
        }
        player.deleteItem(this);
    }
}
