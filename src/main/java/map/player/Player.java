package map.player;

import bot.VampusBot;
import map.Message;
import map.Pair;
import map.cell.Cell;
import map.content.deadly.Hole;
import map.content.deadly.Vampus;
import map.content.deadly.VampusInHole;
import map.content.chest.Chest;
import map.content.chest.Item;
import map.content.portal.Portal;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "SameParameterValue", "unused"})
public class Player implements Serializable {

    private final long id;
    private int game_inst;
    private int hp;

    private Cell pos;

    private List<Item> items;
    private Item active;

    private Map<Object, Object> drawProperty;

    private final static Logger logger = Logger.getLogger(Player.class);


    public Player(Cell pos, long id, int hp) {
        this.hp = hp;
        this.id = id;
        this.pos = pos;
        this.game_inst = 0;
        this.items = new ArrayList<>();
        this.drawProperty = new HashMap<>();
        pos.addPlayer(this);
        logger.info("New player(id = " + id + ") was be created");
    }


    //*******************Unique user id********************
    public long id() {
        return id;
    }
    //*****************************************************


    //**********************Manage hp**********************
    public void hit(VampusBot bot, String message, int damage) {
        hp -= damage;
        if (hp > 0)
            message(bot, message + " и вы потеряли " + damage + " хп, но вы успели вернуться на предыдущую клетку!", 5);
        else kill(bot);
    }

    public void kill(VampusBot bot) {
        hp = 0;
        bot.edit(new Message("Вас убили! Игра завершена."), id, game_inst);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException exc) {
            logger.error(exc);
        }
        removeInstance(bot);
        game_inst = 0;
        bot.leaveGame((int) id);
    }
    //*****************************************************


    //*******************Manage position*******************
    public Cell position() {
        return pos;
    }

    public void teleport(Cell position) {
        this.pos.addPlayer(this);
        this.pos = position;
    }
    //*****************************************************


    //********************Manage items*********************
    public void activate(Item item) {
        this.active = item;
        if (item != null)
            this.active.defaultState(this);
    }

    public void deleteItem(Item item) {
        items.remove(item);
        if (active == item)
            active = null;
    }

    public boolean addItem(VampusBot bot, Item item) {
        if (items.size() == 5) {
            message(bot, "Ваш инвентарь переполнен!", 5);
            return false;
        }
        items.add(item);
        return true;
    }
    //*****************************************************


    //*******Creating game instances using vampusBot*******
    private Message appendControls(Message message) {
        List<Pair<String, String>> items = new ArrayList<>();
        for (int index = 0; index < this.items.size(); index++)
            items.add(new Pair<>(this.items.get(index).icon(), "activate " + index));
        while (items.size() < 5)
            items.add(new Pair<>("∅", "∅"));
        return message.addRow(items).addRow("↑").addRow("←", "→").addRow("↓");
    }

    private void instance(VampusBot vb, Message message, boolean new_instance) {
        if (new_instance)
            this.game_inst = vb.send(message, id);
        else
            vb.edit(message, id, game_inst);
    }

    private void instance(VampusBot vb, boolean new_instance, boolean debug) {
        if (active != null) {
            Message message = active.state().clone();
            instance(
                    vb,
                    message.setMessage(
                            draw(
                                    debug,
                                    active.drawProperty(),
                                    message.message()
                            )
                    ),
                    new_instance
            );
        } else if (pos.empty())
            instance(vb, appendControls(new Message(draw())), new_instance);
        else {
            Message message = pos.content().message() == null ? null : appendControls(pos.content().message().clone());
            if (message == null)
                instance(vb, appendControls(new Message(draw())), new_instance);
            else
                instance(
                        vb,
                        message.setMessage(
                                draw(
                                        false,
                                        new HashMap(),
                                        message.message()
                                )
                        ),
                        new_instance
                );
        }
    }

    public void instance(VampusBot bot) {
        instance(bot, false, false);
    }

    public void newInstance(VampusBot bot) {
        if (game_inst != 0)
            bot.delete(id, game_inst);
        instance(bot, true, false);
    }

    public void removeInstance(VampusBot bot) {
        if (game_inst != 0)
            bot.delete(id, game_inst);
    }

    public void message(VampusBot bot, String text, double showSeconds) {
        bot.edit(new Message(text), id, game_inst);
        try {
            Thread.sleep((long) (showSeconds * 1000));
        } catch (InterruptedException exc) {
            logger.error(exc);
        }
        instance(bot);
    }
    //*****************************************************


    //************Executing action from command************
    public boolean enter(VampusBot bot, UnaryOperator<Cell> unaryCell) {
        Cell next = unaryCell.apply(pos);
        if (next == null)
            return false;
        if (!next.empty()) {
            if (next.content().enter(bot, this))
                teleport(next);
        } else {
            teleport(next);
        }
        return true;
    }

    public void action(VampusBot bot, String action) {
        logger.debug("Execute action: '" + action + "'");
        try {
            switch (action) {
                case "↑":
                case "←":
                case "→":
                case "↓":
                    switch (action) {
                        case "↑":
                            enter(bot, Cell::up);
                            break;
                        case "←":
                            enter(bot, Cell::left);
                            break;
                        case "→":
                            enter(bot, Cell::right);
                            break;
                        case "↓":
                            enter(bot, Cell::down);
                            break;
                    }
                    instance(bot, false, false);
                    break;
                case "instance":
                    instance(bot, true, false);
                    logger.info("Creating new game instance: " + game_inst);
                    break;
                case "cancel":
                    logger.debug("Deactivate " + active.icon());
                    this.active = null;
                    instance(bot, true, false);
                    break;
                default:
                    String[] command = action.split(" ");
                    switch (command[0]) {
                        case "content":
                            assert !pos.empty();
                            assert command.length == 2;
                            pos.content().changeState(bot, this, command[1]);
                            instance(bot);
                            break;
                        case "item":
                            assert active != null;
                            assert command.length == 2;
                            active.changeState(bot, this, command[1]);
                            instance(bot);
                            break;
                        case "activate":
                            assert command.length == 2;
                            activate(items.get(Integer.parseInt(command[1])));
                            instance(bot);
                            break;
                    }
                    break;
            }
        } catch (Exception exc) {
            logger.error(exc);
        }
    }
    //*****************************************************


    //***********************Drawing***********************
    public void addDrawProperty(Map<Cell, String> drawProperty) {
        this.drawProperty.putAll(drawProperty);
    }

    private String feelingsFrom(Player player, Cell thisCell) {
        if (!thisCell.contains(player))
            return "⬛";
        Set<Class> set = Stream.of(thisCell.down(), thisCell.right(), thisCell.up(), thisCell.left())
                .filter(Objects::nonNull)
                .map(Cell::content)
                .filter(Objects::nonNull)
                .map(Object::getClass)
                .collect(Collectors.toSet());
        boolean smell = false;
        boolean wind = false;
        if (set.contains(VampusInHole.class)) {
            smell = true;
            wind = true;
        } else if (set.contains(Vampus.class))
            smell = true;
        else if (set.contains(Hole.class))
            wind = true;

        if (smell && wind)
            return "\uD83C\uDF2B";
        else if (smell)
            return "♨";
        else if (wind)
            return "\uD83D\uDCA8";
        else
            return "⬜";
    }

    private String debugFrom(Player player, Cell thisCell) {
        if (thisCell.empty())
            return "⬜";
        else {
            Class aClass = thisCell.content().getClass();
            if (aClass.equals(Hole.class))
                return "\uD83D\uDD73";
            else if (aClass.equals(Portal.class))
                return "\uD83C\uDF65";
            else if (aClass.equals(Chest.class))
                return "\uD83D\uDCE6";
            else if (aClass.equals(Vampus.class))
                return "\uD83D\uDE21";
            else if (aClass.equals(VampusInHole.class))
                return "\uD83E\uDD2C";
            else
                return "⬜";
        }
    }

    private String feelingsMessage(Player player) {
        Set<Class> set = Stream.of(
                player.position().down(),
                player.position().right(),
                player.position().up(),
                player.position().left()
        )
                .filter(Objects::nonNull)
                .map(Cell::content)
                .filter(Objects::nonNull)
                .map(Object::getClass)
                .collect(Collectors.toSet());

        boolean smell = false;
        boolean wind = false;

        if (set.contains(Hole.class))
            wind = true;
        else if (set.contains(Vampus.class))
            smell = true;
        else if (set.contains(VampusInHole.class)) {
            smell = true;
            wind = true;
        }

        if (smell && wind)
            return "Вы чувствуете ветер и запах";
        else if (smell)
            return "Вы чувствуете запах";
        else if (wind)
            return "Вы чувствуете ветер";
        else
            return "Вы ничего не чувствуете";
    }

    private String toLines(String message, int length) {
        StringBuilder main = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (String word : message.split(" "))
            if (line.length() + word.length() > length) {
                main.append(line).append("\n");
                line = new StringBuilder();
                line.append(word).append(" ");
            } else line.append(word).append(" ");
        if (line.length() > 0)
            main.append(line);
        return main.toString();
    }

    public String draw(boolean debug, Map drawPolicy, String message) {
        List<Cell> leftList = new ArrayList<>();
        Cell leftCell = this.position().level();
        do {
            leftList.add(leftCell);
        } while ((leftCell = leftCell.down()) != null);
        StringBuilder mainBuilder = new StringBuilder();
        for (Cell rowCell : leftList) {
            StringBuilder row = new StringBuilder();
            loop: do {
                if (position() == rowCell) {
                    row.append("\uD83D\uDC64");
                    continue;
                }
                /*TODO
                for (Player player : rowCell.players()) {
                    if (player.position() == rowCell) {
                        row.append("\uD83D\uDC68\uD83C\uDFFB\u200D\uD83E\uDDB0");
                        continue loop;
                    }
                }
                */
                Object image;
                if ((image = drawPolicy.get(rowCell)) != null)
                    row.append(image);
                else if ((image = drawProperty.get(rowCell)) != null)
                    row.append(image);
                else if (debug)
                    row.append(debugFrom(this, rowCell));
                else
                    row.append(feelingsFrom(this, rowCell));
            } while ((rowCell = rowCell.right()) != null);
            mainBuilder.append(row.toString()).append("\n");
        }
        int right_size = 0;
        Cell level = pos.level();
        while (level != null) {
            right_size++;
            level = level.right();
        }
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < right_size; i++)
            line.append("➖");
        mainBuilder
                .append("\n")
                .append(line)
                .append("\n")
                .append(toLines(feelingsMessage(this), right_size * 2))
                .append("\n");
        if (active != null) {
            mainBuilder
                    .append("\n")
                    .append(line)
                    .append("\n");
            mainBuilder
                    .append("[")
                    .append(active.icon())
                    .append("] описание:\n\n")
                    .append(toLines(active.description(), right_size * 2))
                    .append("\n");
        } else if (message != null)
            mainBuilder
                    .append("\n")
                    .append(line)
                    .append("\n")
                    .append(message)
                    .append("\n");
        mainBuilder.append("\n♥: ").append(hp);
        return mainBuilder.toString();
    }

    public String draw() {
        return draw(false, new HashMap<>(), null);
    }
    //*****************************************************
}
