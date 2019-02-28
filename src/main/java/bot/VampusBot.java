package bot;

import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.*;
import java.sql.*;

import map.Map;
import map.player.Player;
import map.tools.MapGenerator;
import map.tools.PlayersCache;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public class VampusBot extends TelegramLongPollingBot {
    private Logger logger = Logger.getLogger(VampusBot.class);
    private Connection connection;
    private PlayersCache pc = new PlayersCache(20);

    {
        try {
            Properties properties = new Properties();
            properties.load(new FileReader("src/main/resources/database.properties"));
            connection = DriverManager.getConnection(properties.getProperty("url"), properties);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi tba = new TelegramBotsApi();
        tba.registerBot(new VampusBot());
        new Thread(() -> {
            for (;;) {
                System.out.println("Thread is working");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private VampusBot() {

    }

    private Player findPlayer(int id) throws SQLException, IOException {
        Player player = pc.find(id);
        if (player == null) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT map_hash FROM users WHERE id = " + id);
            if (!rs.next())
                return null;
            String map_hash = rs.getString("map_hash");
            if (map_hash == null)
                return null;
            rs = statement.executeQuery("SELECT serialized_map FROM maps WHERE map_hash = '" + map_hash + "'");
            while (rs.next()) {
                ObjectInputStream ooi = new ObjectInputStream(
                        new ByteArrayInputStream(
                                Base64.getDecoder().decode(
                                        rs.getString("serialized_map")
                                )
                        )
                );
                try {
                    Map map = (Map) ooi.readObject();
                    pc.load(map);
                } catch (ClassNotFoundException exc) {
                    return null;
                }

            }
            player = pc.find(id);
            logger.info("Successful downloading player from database");
        } else
            logger.info("Successful uploading player from cache");
        return player;
    }

    private void uploadMap(Map map) throws IOException, SQLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(map);
        int map_hash = map.hashCode();
        PreparedStatement ps = connection.prepareStatement("UPDATE USERS SET MAP_HASH = ? WHERE ID = ?");
        for (Player player : map.players()) {
            ps.setInt(1, map_hash);
            ps.setLong(2, player.id());
            ps.addBatch();
        }
        ps.executeBatch();
        connection.createStatement().execute("INSERT INTO MAPS(MAP_HASH, SERIALIZED_MAP) VALUES(" + map_hash + ", '" + Base64.getEncoder().encodeToString(baos.toByteArray()) + "')");
        logger.info("Map was successful uploaded to database");
    }

    private void createUser(int id) throws SQLException {
        connection.createStatement().execute("INSERT INTO USERS(ID) VALUES(" + id + ")");
        logger.info("User with id = " + id + " was successful created");
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                User user = message.getFrom();

                if (message.hasText()) {
                    String text = message.getText();

                    logger.info(
                            "Receiving text message from " +
                                    (user.getUserName() == null ? user.getId() : user.getUserName()) +
                                    " with text = '" +
                                    text +
                                    "'"
                    );

                    Player player = findPlayer(user.getId());

                    if (player == null)
                        switch (text) {
                            case "/new_game":
                                sendApiMethod(
                                        new SendMessage()
                                                .setChatId((long) user.getId())
                                                .setText("Новая игра:")
                                                .setReplyMarkup(
                                                        new InlineKeyboardMarkup()
                                                                .setKeyboard(
                                                                        new ArrayList<List<InlineKeyboardButton>>() {{
                                                                            add(
                                                                                    new ArrayList<InlineKeyboardButton>() {{
                                                                                        add(new InlineKeyboardButton().setText("Одиночная").setCallbackData("new single game"));
                                                                                    }}
                                                                            );
                                                                            add(
                                                                                    new ArrayList<InlineKeyboardButton>() {{
                                                                                        add(new InlineKeyboardButton().setText("Многопользовательская").setCallbackData("new multi player game"));
                                                                                    }}
                                                                            );
                                                                        }}
                                                                )
                                                )
                                );
                                break;
                        }

                }
            } else {

                if (update.hasCallbackQuery()) {
                    User user = update.getCallbackQuery().getFrom();
                    String data = update.getCallbackQuery().getData();

                    Message message = update.getCallbackQuery().getMessage();

                    Player current_player = findPlayer(user.getId());

                    switch (data) {
                        case "new single game":
                            MapGenerator generator = new MapGenerator(System.currentTimeMillis());
                            Map map = generator.generateMap(user.getId());
                            uploadMap(map);
                            Player player = map.player(0);
                            sendApiMethod(
                                    new SendMessage()
                                    .setChatId((long) user.getId())
                                    .setText(player.draw())
                                    .setReplyMarkup(
                                            new InlineKeyboardMarkup().setKeyboard(
                                                    new ArrayList<List<InlineKeyboardButton>>(){{
                                                        add(
                                                                new ArrayList<InlineKeyboardButton>(){{
                                                                    add(new InlineKeyboardButton().setText("↑").setCallbackData("up"));
                                                                }}
                                                        );
                                                        add(
                                                                new ArrayList<InlineKeyboardButton>(){{
                                                                    add(new InlineKeyboardButton().setText("←").setCallbackData("left"));
                                                                    add(new InlineKeyboardButton().setText("→").setCallbackData("right"));
                                                                }}
                                                        );
                                                        add(
                                                                new ArrayList<InlineKeyboardButton>(){{
                                                                    add(new InlineKeyboardButton().setText("↓").setCallbackData("down"));
                                                                }}
                                                        );
                                                    }}
                                            )
                                    )
                            );
                            break;
                        case "new multi player game":
                            //TODO
                            break;
                        case "up":
                            assert current_player != null;
                            current_player.up();
                            sendApiMethod(editGameInst(current_player, message));
                            break;
                        case "left":
                            assert current_player != null;
                            current_player.left();
                            sendApiMethod(editGameInst(current_player, message));
                            break;
                        case "right":
                            assert current_player != null;
                            current_player.right();
                            sendApiMethod(editGameInst(current_player, message));
                            break;
                        case "down":
                            assert current_player != null;
                            current_player.down();
                            sendApiMethod(editGameInst(current_player, message));
                            break;
                    }
                }
            }
        } catch (Exception exc) {
            logger.error("Exception occurred: ", exc);
        }
    }

    private EditMessageText editGameInst(Player player, Message message) {
        return new EditMessageText()
                .setChatId(message.getChatId())
                .setMessageId(message.getMessageId())
                .setText(player.draw())
                .setReplyMarkup(
                        new InlineKeyboardMarkup().setKeyboard(
                                new ArrayList<List<InlineKeyboardButton>>(){{
                                    add(
                                            new ArrayList<InlineKeyboardButton>(){{
                                                add(new InlineKeyboardButton().setText("↑").setCallbackData("up"));
                                            }}
                                    );
                                    add(
                                            new ArrayList<InlineKeyboardButton>(){{
                                                add(new InlineKeyboardButton().setText("←").setCallbackData("left"));
                                                add(new InlineKeyboardButton().setText("→").setCallbackData("right"));
                                            }}
                                    );
                                    add(
                                            new ArrayList<InlineKeyboardButton>(){{
                                                add(new InlineKeyboardButton().setText("↓").setCallbackData("down"));
                                            }}
                                    );
                                }}
                        )
                );
    }

    @Override
    public String getBotUsername() {
        return "VampusBot";
    }

    @Override
    public String getBotToken() {
        return "717417135:AAGs6eBKLau79igq5nYD4O5svyCD4-QMLT0";
    }

}
