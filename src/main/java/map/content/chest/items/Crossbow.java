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
                "⤭",
                "Это арбалет, он стреляет в три случайные клетки рядом с вами.\n" +
                        "Если попадает в вампуса, то убивает его.\n" +
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
        switch (command) {
            case "shot":
                Cell pos = player.position();
                RandomInstance<Cell> random = new RandomInstance<>(new Random(), pos::up, pos::down, pos::left, pos::right);
                for (int i = 0; i < 3; i++) {
                    Cell cell = random.instance(25, 25, 25, 25);
                    if (!cell.empty())
                        if (cell.content().getClass() == Vampus.class || cell.content().getClass() == VampusInHole.class) {
                            player.message(bot, "Вы убили вампуса!", 5);
                            cell.deleteContent();
                            if (cell.content().getClass() == Vampus.class)
                                cell.setContent(new Hole());
                            logger.info("Hitting vampus from crossbow!");
                        }
                }
                player.deleteItem(this);
                break;
        }
    }
}
