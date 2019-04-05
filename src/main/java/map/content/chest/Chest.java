package map.content.chest;

import bot.VampusBot;
import map.Message;
import map.cell.Cell;
import map.content.chest.items.*;
import map.content.Content;
import map.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unused")
public class Chest implements Content {
    private Map drawProperty;
    private Message message;
    private Item item;

    private Chest(Random random) {
        item = new RandomInstance<>(
                random,
                () -> null, Heal::new, Bow::new, Crossbow::new, Teleport::new, Rifle::new, Bomb::new
        ).instance(0, 20, 20, 20, 20, 10, 10);
    }

    public Chest() {
        this(new Random());
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        if (command.equals("get"))
            if (player.addItem(bot, item))
                player.position().deleteContent();
    }

    @Override
    public Message message() {
        if (item == null)
            return new Message("Пустой сундук");
        else
            return new Message("В сундуке лежит " + item.icon()).addRow("Взять:content get");
    }

    @Override
    public String icon() {
        return "\uD83D\uDCE6";
    }
}
