package map.content.chest.items;

import bot.VampusBot;
import map.Message;
import map.cell.Cell;
import map.content.chest.Item;
import map.content.chest.RandomInstance;
import map.content.deadly.Hole;
import map.content.deadly.Vampus;
import map.content.deadly.VampusInHole;
import map.player.Player;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Random;

public class Crossbow extends Item {
    private static final Logger logger = Logger.getLogger(Crossbow.class);

    public Crossbow() {
        super(
                "↗",
                "Это арбалет, он стреляет в четыре случайные клетки рядом с вами. " +
                        "Если попадает в вампуса, то убивает его. " +
                        "Арбалет может выстрелить в одну и ту же клетку несколько раз."
        );
    }

    @Override
    public void defaultState(Player player) {
        super.drawProperty = new HashMap<>();
        super.message = new Message(description())
                .addRow("Выстрелить:item shot")
                .addRow("Отмена:cancel");
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        if ("shot".equals(command)) {
            Cell pos = player.position();
            RandomInstance<Cell> random = new RandomInstance<>(new Random(), pos::up, pos::down, pos::left, pos::right);
            int count = 0;
            for (int i = 0; i < 4; i++) {
                Cell cell = random.instance(25, 25, 25, 25);
                if (!cell.empty())
                    if (cell.content().getClass() == Vampus.class || cell.content().getClass() == VampusInHole.class) {
                        if (cell.content().getClass() == VampusInHole.class)
                            cell.setContent(new Hole());
                        else
                            cell.deleteContent();
                        count++;

                        logger.info("Hitting vampus from crossbow!");
                    }
            }
            if (count == 0)
                player.message(bot, "Вы промахнулись!", 5);
            else if (count == 1)
                player.message(bot, "Вы убили вампуса!", 5);
            else
                player.message(bot, "Вы убили " + count + " вампусов!", 5);
            player.deleteItem(this);
        }
    }
}
