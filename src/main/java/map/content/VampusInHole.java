package map.content;

import map.cell.Cell;

import java.util.function.BiFunction;

public class VampusInHole implements Content {

    @Override
    public BiFunction<Cell, Cell, Boolean> notSettable() {
        return (cell, current) -> current.aroundAnyEquals(cell);
    }
}
