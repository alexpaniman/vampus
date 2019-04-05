package map.content.chest.items;

import bot.VampusBot;
import map.Message;
import map.cell.Cell;
import map.content.deadly.Hole;
import map.content.deadly.Vampus;
import map.content.deadly.VampusInHole;
import map.content.chest.Item;
import map.player.Player;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

public class Bomb extends Item {
    private static final Logger logger = Logger.getLogger(Bomb.class);

    private Cell target;

    public Bomb() {
        super(
                "\uD83D\uDCA3",
                "Это бомба, она уничтожает всех вамсусов и ямы в радиусе, который вы выбираете сами. " +
                        "Бомба может убить вас, чем выше радиус - тем меньше у вас шанс выжить."
        );
    }

    @Override
    public void defaultState(Player player) {
        this.target = player.position().level();
        super.drawProperty = new HashMap<Cell, String>() {{
            put(target, "❌");
        }};
        super.message = new Message(description())
                .addRow("↑:item ↑")
                .addRow("←:item ←", "\uD83D\uDCA3:item explode", "→:item →")
                .addRow("↓:item ↓")
                .addRow("Отмена:cancel");
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        switch (command) {
            case "↑":
                if (target.up() != null)
                    target = target.up();
                super.drawProperty = new HashMap<Cell, String>() {{
                    put(target, "❌");
                }};
                break;
            case "←":
                if (target.left() != null)
                    target = target.left();
                super.drawProperty = new HashMap<Cell, String>() {{
                    put(target, "❌");
                }};
                break;
            case "→":
                if (target.right() != null)
                    target = target.right();
                super.drawProperty = new HashMap<Cell, String>() {{
                    put(target, "❌");
                }};
                break;
            case "↓":
                if (target.down() != null)
                    target = target.down();
                super.drawProperty = new HashMap<Cell, String>() {{
                    put(target, "❌");
                }};
                break;
            case "explode":
                super.message = new Message("Выберите радиус взрыва")
                        .addRow("1 - 15%:item 1")
                        .addRow("2 - 30%:item 2")
                        .addRow("3 - 45%:item 3")
                        .addRow("4 - 60%:item 4")
                        .addRow("5 - 75%:item 5");
                break;
            default:
                if (command.matches("\\d+"))
                    explode(bot, player, target, command);
                player.deleteItem(this);
                logger.info("Explode bomb(radius = " + command + ")");
                break;
        }

    }

    private void add(List<Cell> list, Cell start, Function<Cell, Cell> next, int num) {
        Cell cell = next.apply(start);
        for (int i = 0; i < num; i++) {
            if (cell == null)
                return;
            list.add(cell);
            cell = next.apply(cell);
        }
    }

    private void explode(VampusBot bot, Player player, Cell pos, String radius) {
        int rad = Integer.parseInt(radius);

        double random = Math.random();
        double target = rad * 15d / 100d;
        if (random < target) {
            player.kill(bot);
            return;
        }

        List<Cell> tokens = new ArrayList<>();

        tokens.add(pos);

        add(tokens, pos, Cell::right, rad);
        add(tokens, pos, Cell::left, rad);

        for (Cell curr : new ArrayList<>(tokens)) {
            add(tokens, curr, Cell::up, rad);
            add(tokens, curr, Cell::down, rad);
        }

        Collections.shuffle(tokens);

        int count = 0;
        super.drawProperty = new HashMap<>();

        for (Cell cell : tokens) {
            if (!cell.empty())
                if (
                        Stream
                                .of(Vampus.class, Hole.class, VampusInHole.class)
                                .anyMatch(c -> c == cell.content().getClass())
                        )
                    cell.deleteContent();
            player.addDrawProperty(new HashMap<Cell, String>() {{
                put(cell, cell.icon());
            }});
            if (count % 5 == 0) {
                player.instance(bot);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException exc) {
                    exc.printStackTrace();
                }
            }
            count++;
        }
        if ((count - 1) % 5 != 0)
            player.instance(bot);
    }
}