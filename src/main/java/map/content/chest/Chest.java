package map.content.chest;

import map.cell.Cell;
import map.content.chest.items.*;
import map.content.Content;
import java.util.Random;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public class Chest implements Content {
    private Item item;

    public Chest(Random random) {
        int rand = (int) Math.ceil(random.nextDouble() * 100);
        if (rand <= 60)
            item = new Bow();
        else if (rand <= 80)
            item = new Crossbow();
        else if (rand <= 90)
            item = new Rifle();
        else if (rand <= 95)
            item = new Bomb();
        else if (rand <= 100)
            item = new Teleport();
    }

    public Chest() {
        this(new Random());
    }

    public Item getItem() {
        return item;
    }

    public boolean empty() {
        return item == null;
    }

    @Override
    public BiFunction<Cell, Cell, Boolean> notSettable() {
        return (cell, current) -> false;
    }
}
