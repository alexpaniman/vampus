package map;

import com.google.common.base.Objects;
import map.cell.Cell;
import map.player.Player;

import java.io.Serializable;
import java.util.List;

public class Map implements Serializable {
    private List<Cell[][]> levels;
    private Player[] players;

    public Map(List<Cell[][]> levels, Player[] players) {
        this.levels = levels;
        this.players = players;
    }

    public Player player(int ind) {
        return players[ind];
    }

    public Player[] players() {
        return players;
    }

    public Cell[][] level(int ind) {
        return levels.get(ind);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(levels, players);
    }
}
