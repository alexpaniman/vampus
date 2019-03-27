package map.content.chest;

import bot.VampusBot;
import map.State;
import map.content.chest.items.*;
import map.content.Content;
import map.player.Player;

import java.util.Map;
import java.util.Random;

@SuppressWarnings("unused")
public class Chest implements Content {
    private Map drawProperty;
    private State state;
    private Item item;

    private Chest(Random random) {
        item = new RandomInstance<>(random, Rifle::new).instance(100);
        /*int rand = (int) Math.ceil(random.nextDouble() * 100);
        if (rand <= 60)
            item = new Bow();
        else if (rand <= 80)
            item = new Crossbow();
        else if (rand <= 90)
            item = new Rifle();
        else if (rand <= 95)
            item = new Bomb();
        else if (rand <= 100)
            item = new Teleport();*/

        //item = new Bomb();
    }

    public Chest() {
        this(new Random());
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        if (command.equals("get")) {
            player.add(item);
            player.position().deleteContent();
        }
    }

    @Override
    public State state() {
        return new State("В сундуке лежит " + item.icon()).addRow("Взять:content get");
    }

    @Override
    public String icon() {
        return "\uD83D\uDCE6";
    }
}
