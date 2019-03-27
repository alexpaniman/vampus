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

    public Map findMap(int player_id) {
        for (Map map: maps)
            for (Player player: map.players())
                if (player.id() == player_id)
                    return map;
        return null;
    }

    public void load(Map map) {
        maps.add(map);
        if (maps.size() > max_size)
            maps.pollFirst();
    }

    public int delete(int player_id) {
        for (int i = 0; i < maps.size(); i ++)
            for (Player player: maps.get(i).players())
                if (player.id() == player_id) {
                    Map map = maps.remove(i);
                    return map.hashCode();
                }
        return 0;
    }

    public LinkedList<Map> maps() {
        return maps;
    }
}
