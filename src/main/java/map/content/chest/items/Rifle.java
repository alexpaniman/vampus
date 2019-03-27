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
import java.util.LinkedHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class Rifle extends Item {
    private static final Logger logger = Logger.getLogger(Rifle.class);

    public Rifle() {
        super(
                "🔫",
                "Это ружьё, оно стреляет в выбраном направлении. Пуля летит до тех пор, пока не убьёт вампуса или не врежется в стену."
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
        UnaryOperator<Cell> operator;
        switch (command) {
            case "↑":
                operator = Cell::up;
                break;
            case "←":
                operator = Cell::left;
                break;
            case "→":
                operator = Cell::right;
                break;
            case "↓":
                operator = Cell::down;
                break;
            default:
                return;
        }
        Cell cell = player.position();
        while ((cell = operator.apply(cell)) != null) {
            final Cell finalCell = cell;
            super.drawProperty = new HashMap<Cell, String>() {{
                put(finalCell, "❌");
            }};
            player.instance(bot);
            bot.sleep(1);
            if (!cell.empty())
                if (cell.content().getClass() == Vampus.class || cell.content().getClass() == VampusInHole.class) {
                    bot.edit(new State("Попадание в вампуса!"), player.id(), player.gameInstance());
                    bot.sleep(5);
                    if (cell.content().getClass() == Vampus.class)
                        cell.deleteContent();
                    else {
                        cell.deleteContent();
                        cell.setContent(new Hole());
                    }
                    break;
                }
        }
    }
}
