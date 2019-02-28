package map.player;

import com.google.common.base.Joiner;
import com.google.common.cache.LoadingCache;
import map.cell.Cell;
import map.content.Content;
import map.content.Hole;
import map.content.Vampus;
import map.content.VampusInHole;
import map.content.chest.Chest;
import map.content.chest.items.Item;
import map.content.portal.Portal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Player implements Serializable {
    private Cell pos;
    private final long id;
    private List<Item> items;

    public static void main(String[] args) {
        /*MapGenerator generator = new MapGenerator(123);
        Cell cell = generator.generateMap();
        StringBuilder init = new StringBuilder();
        StringBuilder link = new StringBuilder();

        Cell i = cell;
        for (int g = 0; (i = i.down()) != null; g ++) {
            Cell j = i;
            for (int q = 0; (j = j.right()) != null; q ++) {
                String ind = "\"" + j.hash() + "\"";
                init.append(ind).append(";\n");
                if (j.left() != null)
                    link.append(ind).append("--").append("\"").append(j.left().hash()).append("\"").append(";\n");
                if (j.right() != null)
                    link.append(ind).append("--").append("\"").append(j.right().hash()).append("\"").append(";\n");
                if (j.up() != null)
                    link.append(ind).append("--").append("\"").append(j.up().hash()).append("\"").append(";\n");
                if (j.down() != null)
                    link.append(ind).append("--").append("\"").append(j.down().hash()).append("\"").append(";\n");
            }
        }
        String dot = "graph G {\n" + init + link + "}";
        File file = new File("graph.dot");
        try {
            PrintWriter pw = new PrintWriter(file);
            pw.print(dot);
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 100; i ++)
        System.out.println(new Player(generator.generateMap().down().down().right().right().right().right(), "Alex").serialize());
        FileOutputStream fos = new FileOutputStream("test.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(generator.generateMap());
        oos.flush();
        oos.close();
        FileInputStream fis = new FileInputStream("test.txt");
        ObjectInputStream ois = new ObjectInputStream(fis);
        Cell cell = (Cell) ois.readObject();
        System.out.println(new LevelDraw(new Player(cell, "None")).draw(true));*/
    }

    public Player(Cell pos, long id) {
        this.id = id;
        this.pos = pos;
        this.items = new ArrayList<>();
        pos.addUser(this);
    }

    public static Player parse(LoadingCache<Integer, Cell> cellCache, LoadingCache<Integer, Item> itemCache, String str) {
        try {
            String[] split = str.split("-");
            int y = Integer.parseInt(split[1]);
            int x = Integer.parseInt(split[2]);
            long id = Integer.parseInt(split[0]);
            int level_hash = Integer.parseInt(split[3]);
            Cell pos = cellCache.get(level_hash);
            List<Item> items = new ArrayList<>();
            for (int i = 4; i < split.length; i++)
                items.add(itemCache.get(Integer.parseInt(split[i])));
            for (int i = 0; i < y; i++) pos = pos.down();
            for (int i = 0; i < x; i++) pos = pos.right();
            return new Player(pos, id).setItems(items);
        } catch (ExecutionException exec) {
            throw new IllegalArgumentException();
        }
    }

    private Player setItems(List<Item> items) {
        this.items = items;
        return this;
    }

    public Player setPos(Cell pos) {
        this.pos = pos;
        return this;
    }

    public Player add(Item item) {
        items.add(item);
        return this;
    }

    public long id() {
        return id;
    }

    public Cell position() {
        return pos;
    }

    public Content content() {
        return pos.content();
    }

    public Player setContent(Content content) {
        pos.setContent(content);
        return this;
    }

    public Player deleteContent() {
        return this.setContent(null);
    }

    public boolean left() {
        if (pos.left() == null)
            return false;
        pos = pos.left();
        pos.addUser(this);
        return true;
    }

    public boolean right() {
        if (pos.right() == null)
            return false;
        pos = pos.right();
        pos.addUser(this);
        return true;
    }

    public boolean up() {
        if (pos.up() == null)
            return false;
        pos = pos.up();
        pos.addUser(this);
        return true;
    }

    public boolean down() {
        if (pos.down() == null)
            return false;
        pos = pos.down();
        pos.addUser(this);
        return true;
    }

    private String feelingsFrom(Player player, Cell thisCell) {
        if (player.position() == thisCell)
            return "\uD83D\uDC64";
        if (!thisCell.contains(player.id()))
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
        }
        else if (set.contains(Vampus.class))
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
        if (player.position() == thisCell)
            return "\uD83D\uDC64";
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

    public String draw(boolean debug) {
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
                if (debug) {
                    row.append(debugFrom(this, rowCell));
                } else
                    row.append(feelingsFrom(this, rowCell));
            } while ((rowCell = rowCell.right()) != null);
            mainBuilder.append(row.toString()).append("\n");
        }
        return mainBuilder.toString();
    }

    public String draw() {
        return draw(false);
    }

    public String serialize() {
        int y = 0; for (Cell cell = pos; (cell = cell.up()) != null; )   y ++;
        int x = 0; for (Cell cell = pos; (cell = cell.left()) != null; ) x ++;
        return      id  +
                    "-" +
                    x   +
                    "-" +
                    y   +
                    "-" +
                    pos
                            .level()
                            .hashCode() + (
                            items.size() == 0?
                                    "" : "-" +
                                    Joiner
                                            .on("-")
                                            .join(
                                                    items
                                                            .stream()
                                                            .mapToInt(Object::hashCode)
                                                            .boxed()
                                                            .collect(Collectors.toList())
                                            )
                    );
    }
}
