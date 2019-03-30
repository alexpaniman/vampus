package bot;

import javafx.util.Pair;
import map.Map;
import map.Message;
import map.player.Player;
import map.tools.MapGenerator;
import map.tools.PlayersCache;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
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
                        ***REMOVED***,
                        ***REMOVED***
                )
        );
        logger.debug("Vampus bot was successful registered");
    }

    private VampusBot(String username, String token) {
        logger.info("Creating vampus bot with username = '" + username + "' and token = '" + token + "'");
        this.username = username;
        this.token = token;
        long sleep = 60000;
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
        logger.info("Start cache loader every " + sleep / 1000D + " seconds");
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
            logger.error("Exception occurred when executing uploadCache: ", exc);
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
            logger.error("Exception occurred when executing createMap: ", exc);
        }
    }

    //TODO
    private void deleteMap(int id) {
        try {
            int map_hash = pc.delete(id);
            connection
                    .createStatement()
                    .execute("DELETE FROM maps WHERE map_hash = " + map_hash);
            logger.debug("Map with hash = " + map_hash + " was successful deleted");
        } catch (SQLException exc) {
            logger.error("Exception occurred when executing deleteMap: ", exc);
        }
    }

    private void createUser(int id) {
        try {
            connection
                    .createStatement()
                    .execute("INSERT INTO USERS(ID) VALUES(" + id + ")");
            logger.debug("User with id = " + id + " was successful created");
        } catch (SQLException exc) {
            logger.error("Exception occurred when executing createUser: ", exc);
        }
    }

    private InlineKeyboardMarkup inline(Message message) {
        InlineKeyboardMarkup ikm = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (List<Pair<String, String>> buttons : message.buttons()) {
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

    public void edit(Message message, long chat, long message_id) {
        EditMessageText editMessage = new EditMessageText()
                .setChatId(chat)
                .setText(message.message())
                .setReplyMarkup(inline(message))
                .setMessageId(Math.toIntExact(message_id));
        try {
            sendApiMethod(editMessage);
            logger.debug("Message = " + message_id + " in chat = " + chat + " was edited");
        } catch (TelegramApiException exc) {
            logger.error("Exception occurred when executing edit: ", exc);
        }
    }

    public int send(Message message, long chat) {
        SendMessage sendMessage = new SendMessage()
                .setChatId(chat)
                .setText(message.message())
                .setReplyMarkup(inline(message));
        try {
            logger.debug("Sending message to chat = " + chat);
            return sendApiMethod(sendMessage).getMessageId();
        } catch (TelegramApiException exc) {
            logger.error("Exception occurred when executing send: ", exc);
            return -1;
        }
    }

    public void delete(long chat, int message_id) {
        try {
            deleteMessage(
                    new DeleteMessage()
                            .setMessageId(message_id)
                            .setChatId(String.valueOf(chat))
            );
        } catch (TelegramApiException exc) {
            logger.error("Exception occurred when executing delete: ", exc);
        }
    }

    public void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException exc) {
            logger.error("Exception occurred when executing sleep: ", exc);
        }
    }

    private void addGame(long id, int game, int owner_message) {
        try {
            connection
                    .createStatement()
                    .execute(
                            "INSERT INTO MAPS_TO_JOIN(GAME, OWNER_MESSAGE, PLAYERS) VALUES(" +
                                    game + ", " +
                                    owner_message + ", '" +
                                    id + "')"
                    );
        } catch (SQLException exc) {
            logger.error("Exception occurred when executing addGame: ", exc);
        }

    }

    private void joinGame(long game, int id) {
        try {
            connection
                    .createStatement()
                    .execute(
                            "UPDATE MAPS_TO_JOIN SET PLAYERS = CONCAT(PLAYERS, ' ', '" +
                                    id +
                                    "') WHERE GAME = " +
                                    game
                    );
        } catch (SQLException exc) {
            logger.error("Exception occurred when executing joinGame: ", exc);
        }
    }

    private void editOwner(long game) {
        try {
            ResultSet rs = connection
                    .createStatement()
                    .executeQuery("SELECT * FROM MAPS_TO_JOIN WHERE GAME = " + game);
            rs.next();
            edit(
                    new Message(
                            "Создана новая игра: " +
                                    game +
                                    ".\n" +
                                    (
                                            rs
                                                    .getString("PLAYERS")
                                                    .split(" ")
                                                    .length - 1
                                    ) +
                                    " игроков присоеденилось."
                    ).addRow("Начать:start_game " + game),
                    Integer.parseInt(
                            rs.getString("PLAYERS").split(" ")[0]
                    ),
                    rs.getInt("OWNER_MESSAGE")
            );
        } catch (SQLException exc) {
            logger.error("Exception occurred when executing ownerMessage: ", exc);
        }
    }

    private List<Integer> gamesList() {
        try {
            ResultSet rs = connection
                    .createStatement()
                    .executeQuery("SELECT GAME FROM MAPS_TO_JOIN");
            List<Integer> list = new ArrayList<>();
            while (rs.next())
                list.add(rs.getInt("GAME"));
            return list;
        } catch (SQLException exc) {
            logger.error("Exception occurred when executing gamesList: ", exc);
            return new ArrayList<>();
        }
    }

    private void createGame(int game) {
        try {
            ResultSet rs = connection
                    .createStatement()
                    .executeQuery("SELECT PLAYERS FROM MAPS_TO_JOIN WHERE GAME = " + game);
            rs.next();
            Map map = new MapGenerator().generateMap(
                    Arrays
                            .stream(rs.getString("PLAYERS").split(" "))
                            .mapToInt(Integer::parseInt)
                            .toArray()
            );
            createMap(map);
            for (Player player: map.players())
                player.newInstance(this);
        } catch (SQLException exc) {
            logger.error("Exception occurred when executing createGame: ", exc);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                org.telegram.telegrambots.api.objects.Message message = update.getMessage();
                assert message.getChatId() == (long) update.getMessage().getFrom().getId();
                long chat_id = message.getChatId();
                Player player = findPlayer((int) chat_id);
                if (message.hasText()) {
                    String text = message.getText();
                    switch (text) {
                        case "/game":
                            if (player != null) {
                                int message_id = send(
                                        new Message("Вы уже в игре! Для начала покиньте текущую игру."),
                                        chat_id
                                );
                                Thread.sleep(5000);
                                delete(chat_id, message_id);
                                return;
                            }
                            send(
                                    new Message("Выбирите: ")
                                            .addRow("Присоединиться к игре:join game")
                                            .addRow("Новая многопользовательская игра:multi game")
                                            .addRow("Новая  одиночная игра:single game"),
                                    chat_id
                            );
                            break;
                        case "/leave_game:":
                            pc
                                    .findMap((int) chat_id)
                                    .removePlayer((int) chat_id);
                            break;
                        default:
                            if (player != null)
                                player.action(this, text);
                            break;
                    }
                }
            } else if (update.hasCallbackQuery()) {
                CallbackQuery query = update.getCallbackQuery();

                String data = query.getData();
                int id = Math.toIntExact(query.getMessage().getChatId());
                int currentMessage = query.getMessage().getMessageId();
                Player player = findPlayer(id);
                switch (data) {
                    case "join game":
                        delete(id, currentMessage);
                        Message messageSend = new Message("Выберите игру:");
                        for (int game : gamesList())
                            messageSend.addRow(game + ":join " + game);
                        send(messageSend, id);
                        break;
                    case "multi game":
                        delete(id, currentMessage);
                        int game = (int) (Math.random() * 10000);
                        int owner_message = send(
                                new Message("Создана новая игра: " + game + ".\n 0 игроков присоеденилось.")
                                        .addRow("Начать:start_game " + game),
                                id
                        );
                        addGame(id, game, owner_message);
                        break;
                    case "single game":
                        delete(id, currentMessage);
                        map.Map map = new MapGenerator().generateMap(id);
                        createMap(map);
                        map.player(0).action(this, "instance");
                        break;
                    default:
                        String[] args = data.split(" ");
                        if (args[0].equals("join")) {
                            int g = Integer.valueOf(args[1]);
                            joinGame(g, id);
                            editOwner(g);
                            delete(id, currentMessage);
                        } else if (args[0].equals("start_game")) {
                            int g = Integer.valueOf(args[1]);
                            createGame(g);
                        } else {
                            if (player != null)
                                player.action(this, data);
                        }
                        break;
                }
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