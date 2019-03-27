package bot;

import javafx.util.Pair;
import map.State;
import map.player.Player;
import map.tools.MapGenerator;
import map.tools.PlayersCache;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.*;
import java.sql.*;
import java.util.*;

public class VampusBot extends TelegramLongPollingBot {
    private static Logger logger = Logger.getLogger(VampusBot.class);
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

    private String username;
    private String token;

    public static void main(String[] args) throws TelegramApiRequestException {
        ApiContextInitializer.init();
        TelegramBotsApi tba = new TelegramBotsApi();
        tba.registerBot(
                new VampusBot(
                        "VampusBot",
                        "717417135:AAGs6eBKLau79igq5nYD4O5svyCD4-QMLT0"
                )
        );
        logger.debug("Vampus bot was successful registered");
    }

    private VampusBot(String username, String token) {
        logger.info("Creating vampus bot with username = '" + username + "' and token = '" + token + "'");
        this.username = username;
        this.token = token;
        long sleep = 600000;
        Thread saver = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                uploadCache();
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException exc) {
                    exc.printStackTrace();
                }
            }
        });
        saver.setDaemon(true);
        saver.setName("CacheSaver");
        saver.start();
        logger.info("Start cache loader every " + sleep / 60000D + " seconds");
    }

    private Player findPlayer(int id) throws SQLException, IOException {
        Player player = pc.find(id);
        if (player == null) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT map_hash FROM users WHERE id = " + id);
            if (!rs.next()) {
                createUser(id);
                return null;
            }
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
                    map.Map map = (map.Map) ooi.readObject();
                    pc.load(map);
                } catch (ClassNotFoundException exc) {
                    return null;
                }

            }
            player = pc.find(id);
            logger.debug("Successful downloading player from database");
        } else
            logger.debug("Successful uploading player from cache");
        return player;
    }

    private void uploadCache() {
        try {
            logger.debug("Uploading cache(size = " + pc.maps().size() + ")");
            for (map.Map map : pc.maps()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                oos.writeObject(map);
                int map_hash = map.hashCode();
                String encoded = Base64
                        .getEncoder()
                        .encodeToString(outputStream.toByteArray());
                connection.createStatement().execute(
                        "UPDATE MAPS SET SERIALIZED_MAP = '" + encoded + "' WHERE MAP_HASH = " + map_hash);
                logger.debug("Successful uploading map with hash = " + map_hash);
            }

        } catch (IOException | SQLException exc) {
            exc.printStackTrace();
        }
    }

    private void createMap(map.Map map) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(map);
            int map_hash = map.hashCode();
            PreparedStatement ps = connection.prepareStatement("UPDATE USERS SET MAP_HASH = " + map_hash + " WHERE ID = ?");
            for (Player player : map.players()) {
                ps.setLong(1, player.id());
                ps.addBatch();
            }
            ps.executeBatch();
            connection
                    .createStatement()
                    .execute(
                            "INSERT INTO MAPS(MAP_HASH, SERIALIZED_MAP) VALUES(" +
                                    map_hash +
                                    ", '" +
                                    Base64
                                            .getEncoder()
                                            .encodeToString(
                                                    outputStream.toByteArray()
                                            ) +
                                    "')"
                    );
            pc.load(map);
            logger.debug("Map with hash = " + map.hashCode() + " was successful uploaded to database");
        } catch (SQLException | IOException exc) {
            exc.printStackTrace();
        }
    }

    private void deleteMap(int id) {
        try {
            int map_hash = pc.delete(id);
            connection
                    .createStatement()
                    .execute("DELETE FROM maps WHERE map_hash = " + map_hash);
            logger.debug("Map with hash = " + map_hash + " was successful deleted");
        } catch (SQLException exc) {
            exc.printStackTrace();
        }
    }

    private void createUser(int id) {
        try {
            connection
                    .createStatement()
                    .execute("INSERT INTO USERS(ID) VALUES(" + id + ")");
            logger.debug("User with id = " + id + " was successful created");
        } catch (SQLException exc) {
            exc.printStackTrace();
        }
    }

    private InlineKeyboardMarkup inline(State state) {
        InlineKeyboardMarkup ikm = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (List<Pair<String, String>> buttons : state.buttons()) {
            for (Pair<String, String> pair : buttons)
                row.add(
                        new InlineKeyboardButton()
                                .setText(pair.getKey())
                                .setCallbackData(pair.getValue())
                );
            keyboard.add(row);
            row = new ArrayList<>();
        }
        ikm.setKeyboard(keyboard);
        return ikm;
    }

    public void edit(State state, long chat, long message_id) {
        EditMessageText editMessage = new EditMessageText()
                .setChatId(chat)
                .setText(state.message())
                .setReplyMarkup(inline(state))
                .setMessageId(Math.toIntExact(message_id));
        try {
            sendApiMethod(editMessage);
            logger.debug("Message = " + message_id + " in chat = " + chat + " was edited");
        } catch (TelegramApiException exc) {
            exc.printStackTrace();
        }
    }

    public int send(State state, long chat) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(chat)
                .setText(state.message())
                .setReplyMarkup(inline(state));
        try {
            logger.debug("Sending message to chat = " + chat);
            return sendApiMethod(sendMessage).getMessageId();
        } catch (TelegramApiException exc) {
            throw new IllegalArgumentException(exc);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                long chat_id = message.getChatId();
                Player player = findPlayer((int) chat_id);
                if (message.hasText()) {
                    String text = message.getText();
                    switch (text) {
                        case "/new_game":
                            deleteMap((int) chat_id);
                            map.Map map = new MapGenerator().generateMap((int) (long) chat_id);
                            createMap(map);
                            map.player(0).action(this, "instance");
                            break;
                        case "/delete_game:":
                            deleteMap((int) chat_id);
                            break;
                        default:
                            if (player != null)
                                player.action(this, text);
                            break;
                    }
                }
            } else if (update.hasCallbackQuery()) {
                CallbackQuery query = update.getCallbackQuery();
                Message message = query.getMessage();
                String data = query.getData();
                Player player = findPlayer(Math.toIntExact(message.getChatId()));
                if (player != null)
                    player.action(this, data);
            }
        } catch (Exception exc) {
            logger.error("Exception occurred:", exc);
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}