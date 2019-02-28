package map.tools;

import map.Map;
import map.player.Player;

import java.util.LinkedList;

public class PlayersCache {
    private LinkedList<Map> maps;
    private int max_size;

    public PlayersCache(int max_size) {
        this.maps = new LinkedList<>();
        this.max_size = max_size;
    }

    public Player find(int player_id) {
        for (Map map : maps)
            for (Player player : map.players())
                if (player.id() == player_id) {
                    maps.remove(map);
                    maps.add(map);
                    return player;
                }
        return null;
    }

    public int size() {
        return maps.size();
    }

    public LinkedList<Map> getCache() {
        return maps;
    }

    public void load(Map map) {
        maps.add(map);
        if (maps.size() > max_size)
            maps.pollFirst();
    }
}
