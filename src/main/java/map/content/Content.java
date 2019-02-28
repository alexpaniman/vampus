package map.content;

import map.cell.Cell;

import java.io.Serializable;
import java.util.function.BiFunction;

public interface Content extends Serializable {
    BiFunction<Cell, Cell, Boolean> notSettable();
}