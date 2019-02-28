package map.content.portal;

import map.cell.Cell;
import map.content.Content;
import map.player.Player;

import java.util.function.BiFunction;

public class Portal implements Content {
    private Cell dest;

    public Portal() {

    }

    public Cell getDest() {
        return dest;
    }

    public void teleport(Player player) {
        assert dest != null;
        player.setPos(dest);
    }

    public void setDest(Cell dest) {
        this.dest = dest;
    }

    @Override
    public BiFunction<Cell, Cell, Boolean> notSettable() {
        return (cell, current) -> false;
    }
}
