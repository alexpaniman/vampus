package map.tools;

import map.cell.Cell;
import map.content.*;
import map.content.chest.Chest;
import map.content.Content;
import map.content.portal.Portal;
import map.player.Player;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import map.Map;

import static com.google.common.base.Preconditions.checkArgument;

@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class MapGenerator {
    private Random random;
    private boolean vampus_in_hole;
    private int min_levels;
    private int max_levels;
    private int min_weight;
    private int min_height;
    private int max_weight;
    private int max_height;
    private double vampus;
    private double portal;
    private double chest;
    private double hole;

    public static void main(String[] args) {
        Player player = new MapGenerator(123).generateMap(1234567890).player(0);

        Scanner scan = new Scanner(System.in);

        System.out.println(player.draw() + "\n");

        while (true) {
            String str = scan.next();
            switch (str) {
                case "e":
                    return;
                case "a":
                    if (!player.left())
                        System.out.println("Выход за пределы!\n");
                    break;
                case "d":
                    if (!player.right())
                        System.out.println("Выход за пределы!\n");
                    break;
                case "w":
                    if (!player.up())
                        System.out.println("Выход за пределы!\n");
                    break;
                case "s":
                    if (!player.down())
                        System.out.println("Выход за пределы!\n");
                    break;
                case "t":
                    if (player.position().content() != null && player.position().content().getClass() == Portal.class && ((Portal) (player.position().content())).getDest() != null)
                        ((Portal) player.position().content()).teleport(player);
            }
            System.out.println(player.draw(true) + "\n");
            System.out.println(player.position().content() + "\n");
        }
    }

    public MapGenerator(long seed) {
        this.random = new Random(seed);
        Properties prop = new Properties();
        try {
            prop.load(new FileReader("src/main/resources/game_settings.properties"));
            this.vampus_in_hole = Boolean.parseBoolean(prop.getProperty("game.vampus_in_hole"));
            this.max_levels = Integer.parseInt(prop.getProperty("game.max.levels.count"));
            this.min_levels = Integer.parseInt(prop.getProperty("game.min.levels.count"));
            this.max_height = Integer.parseInt(prop.getProperty("game.max.level.height"));
            this.max_weight = Integer.parseInt(prop.getProperty("game.max.level.weight"));
            this.min_height = Integer.parseInt(prop.getProperty("game.min.level.height"));
            this.min_weight = Integer.parseInt(prop.getProperty("game.min.level.weight"));
            this.vampus = Double.parseDouble(prop.getProperty("game.per_cell.vampus"));
            this.portal = Double.parseDouble(prop.getProperty("game.per_cell.portal"));
            this.chest = Double.parseDouble(prop.getProperty("game.per_cell.chest"));
            this.hole = Double.parseDouble(prop.getProperty("game.per_cell.hole"));
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    private int random(int min, int max) {
        return (int) Math.floor(random.nextDouble() * (max - min + 1) + min);
    }

    private <T extends Content> List<Cell> fill(List<Cell> empty, Supplier<T> supp, int count) {
        List<Cell> content_cells_list = new ArrayList<>();
        List<Cell> settable = new ArrayList<>(empty);
        for (int i = 0; i < count; i++) {
            Content content = supp.get();
            if (settable.size() == 0)
                return content_cells_list;
            Cell cell = settable.get(random.nextInt(settable.size()));
            settable.removeIf(current -> content.notSettable().apply(cell, current) || current == cell);
            empty.remove(cell);
            cell.setContent(content);
            content_cells_list.add(cell);
        }
        return content_cells_list;
    }

    public Map generateMap(int... id) {
        checkArgument(id.length >= 1);

        List<Cell[][]> levels = new ArrayList<>();
        List<Cell> empty_list = new ArrayList<>();
        List<Portal> portal_list = new ArrayList<>();
        Cell portal_to_link = null;
        for (int level = 0; level < random(min_levels, max_levels); level++) {
            Cell[][] map = new Cell[random(min_height, max_height)][random(min_weight, max_weight)];
            Cell level_main = Cell.newBuilder()
                    .thisLevel()
                    .emptyItem()
                    .emptyCoordinates()
                    .noOneWas()
                    .build();
            map[0][0] = level_main;
            for (Cell[] row : map)
                for (int i = 0; i < row.length; i++)
                    if (row[i] == null)
                        row[i] = Cell.newBuilder()
                                .noOneWas()
                                .emptyCoordinates()
                                .setLevel(level_main)
                                .emptyItem()
                                .build();
            List<Cell> empty = new LinkedList<>();
            for (int j = 0; j < map.length; j++) {
                Cell[] row = map[j];
                for (int i = 0; i < row.length; i++) {
                    Cell cell = row[i];
                    if (i - 1 >= 0)
                        cell.setLeft(row[i - 1]);
                    if (i + 1 < row.length)
                        cell.setRight(row[i + 1]);
                    if (j - 1 >= 0)
                        cell.setUp(map[j - 1][i]);
                    if (j + 1 < map.length)
                        cell.setDown(map[j + 1][i]);
                    empty.add(cell);
                }
            }
            int total = map.length * map[0].length;

            int vampus_in_hole = this.vampus_in_hole && random.nextBoolean() ? 1 : 0;
            int vampus_count = (int) Math.ceil(total * vampus) - vampus_in_hole;
            int hole_count = (int) Math.ceil(total * hole);
            int portal_count = (int) Math.ceil(total * portal);
            int chest_count = (int) Math.ceil(total * chest);

            portal_count = portal_count >= 2 ? portal_count : 2;

            fill(empty, () -> new VampusInHole(), vampus_in_hole);
            fill(empty, () -> new Vampus(), vampus_count);
            fill(empty, () -> new Hole(), hole_count);
            fill(empty, () -> new Chest(), chest_count);
            List<Cell> portals = fill(empty, () -> new Portal(), portal_count);

            if (portal_to_link == null)
                portal_to_link = portals.remove(random.nextInt(portals.size()));

            else {
                Cell link = portals.remove(random.nextInt(portals.size()));
                ((Portal) portal_to_link.content()).setDest(link);
                ((Portal) link.content()).setDest(portal_to_link);
                portal_to_link = portals.remove(random.nextInt(portals.size()));
            }

            portal_list.addAll(
                    portals
                            .stream()
                            .map(cell -> (Portal) cell.content())
                            .collect(Collectors.toList())
            );

            empty_list.addAll(empty);
            levels.add(map);
        }

        assert empty_list.size() - id.length >= 0;

        while (portal_list.size() > empty_list.size() - id.length)
            portal_list.remove(random.nextInt(portal_list.size()));

        for (Portal portal : portal_list)
            portal.setDest(empty_list.remove(random.nextInt(empty_list.size())));

        Player[] players = new Player[id.length];
        for (int i = 0; i < id.length; i++)
            players[i] = new Player(empty_list.remove(random.nextInt(empty_list.size())), id[i]);

        return new Map(levels, players);
    }
}
