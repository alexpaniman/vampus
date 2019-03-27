package map.player;

import bot.VampusBot;
import javafx.util.Pair;
import map.State;
import map.cell.Cell;
import map.content.Content;
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
    private int hp;
    private Cell pos;
    private int game_inst;
    private final long id;
    private Item active;
    private List<Item> items;
    private Map<Object, Object> drawProperty;
    private static Logger logger = Logger.getLogger(Player.class);

    public Player(Cell pos, long id, int hp) {
        this.hp = hp;
        this.id = id;
        this.pos = pos;
        this.items = new ArrayList<>();
        this.drawProperty = new HashMap<>();
        pos.addPlayer(this);
        logger.info("New player(id = " + id + ") was be created");
    }

    public void appendDrawProperty(Map<Cell, String> drawProperty) {
        this.drawProperty.putAll(drawProperty);
    }

    public void deleteItem(Item item) {
        items.remove(item);
        if (active == item)
            active = null;
    }

    public long id() {
        return id;
    }

    public void kill() {
        hp = 0;
    }

    public int gameInstance() {
        return game_inst;
    }

    public Cell position() {
        return pos;
    }

    public Player deleteContent() {
        return this.setContent(null);
    }

    public void activate(Item item) {
        this.active = item;
        if (item != null)
            this.active.defaultState(this);
    }

    public Player setPos(Cell pos) {
        this.pos.addPlayer(this);
        this.pos = pos;
        return this;
    }

    public Player add(Item item) {
        items.add(item);
        return this;
    }

    public Player setContent(Content content) {
        pos.setContent(content);
        return this;
    }

    public boolean enter(VampusBot bot, UnaryOperator<Cell> unaryCell) {
        Cell next = unaryCell.apply(pos);
        if (next == null)
            return false;
        pos = next;
        pos.addPlayer(this);
        if (!pos.empty())
            pos.content().enter(bot, this);
        return true;
    }

    public List<Item> items() {
        return items;
    }

    private List<Pair<String, String>> itemsMap() {
        List<Pair<String, String>> items = new ArrayList<>();
        for (int index = 0; index < this.items.size(); index++)
            items.add(new Pair<>(this.items.get(index).icon(), "activate " + index));
        while (items.size() < 5)
            items.add(new Pair<>("∅", "∅"));
        return items;
    }

    private State appendControls(State state) {
        return state.addRow(itemsMap()).addRow("↑").addRow("←", "→").addRow("↓");
    }

    private void instance(VampusBot vb, State state, boolean new_instance) {
        if (new_instance)
            this.game_inst = vb.send(state, id);
        else
            vb.edit(state, id, game_inst);
    }

    private void instance(VampusBot vb, boolean new_instance, boolean debug) {
        if (active != null) {
            State state = active.state().clone();
            instance(
                    vb,
                    state.setMessage(
                            draw(
                                    debug,
                                    active.drawProperty(),
                                    state.message()
                            )
                    ),
                    new_instance
            );
        } else if (pos.empty())
            instance(vb, appendControls(new State(draw())), new_instance);
        else {
            State state = pos.content().state() == null ? null : appendControls(pos.content().state().clone());
            if (state == null)
                instance(vb, appendControls(new State(draw())), new_instance);
            else
                instance(
                        vb,
                        state.setMessage(
                                draw(
                                        false,
                                        new HashMap(),
                                        state.message()
                                )
                        ),
                        new_instance
                );
        }
    }

    public void instance(VampusBot bot) {
        instance(bot, false, false);
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
                            instance(bot, false, false);
                            break;
                        case "item":
                            assert active != null;
                            assert command.length == 2;
                            active.changeState(bot, this, command[1]);
                            instance(bot, false, false);
                            break;
                        case "activate":
                            assert command.length == 2;
                            activate(items.get(Integer.parseInt(command[1])));
                            instance(bot, false, false);
                            break;
                    }
                    break;
            }
        } catch (Exception exc) {
            logger.error("Exception occurred when executing action", exc);
        }
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
        for (Cell l : leftList) {
            StringBuilder row = new StringBuilder();
            Cell rowCell = l;
            do {
                if (position() == rowCell) {
                    row.append("\uD83D\uDC64");
                    continue;
                }
                Object drawProperty = this.drawProperty.get(rowCell);
                if (drawProperty != null) {
                    row.append(drawProperty);
                    continue;
                }
                Object cellDraw = drawPolicy.get(rowCell);
                if (cellDraw != null)
                    row.append(cellDraw);
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
        return mainBuilder.toString();
    }

    public String draw() {
        return draw(false, new HashMap<>(), null);
    }
}
