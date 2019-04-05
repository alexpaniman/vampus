package map.content.chest.items;

import bot.VampusBot;
import map.Message;
import map.content.chest.Item;
import map.player.Player;

import java.util.HashMap;

public class Heal extends Item {
    public Heal() {
        super(
                "♥",
                "Это зелье восстановления здоровья, оно восстанавливет от 1хп до 3хп. " +
                        "У вас не может быть больше 10хп."
        );
    }

    @Override
    public void defaultState(Player player) {
        super.drawProperty = new HashMap<>();
        super.message = new Message(description())
                .addRow("Восстановить здоровье:item heal")
                .addRow("Отмена:cancel");
    }

    @Override
    public void changeState(VampusBot bot, Player player, String command) {
        if (command.equals("heal")) {
            int hp = (int) Math.ceil(Math.random() * 2 + 1);
            hp = Math.min(player.hp() + hp, 10/*Max hp*/);
            player.message(bot, "Вы восстановили " + (player.id() - hp) + "хп.", 5);
        }
    }
}
