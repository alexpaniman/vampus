package bot;

import com.google.common.base.Joiner;
import map.Map;
import map.Message;
import map.Pair;
import map.player.Player;
import map.tools.MapGenerator;
import map.tools.PlayersCache;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
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
    private volatile PlayersCache pc;

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

    //***************create and register bot***************
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
        this.username = username;
        this.token = token;
        logger.info("Creating vampus bot with username = '" + username + "' and token = '" + token + "'");

        this.pc = new PlayersCache(20);

        /*Cache loader timeout*/
        final long millis = 60000;

        //Starting cache loader
        Thread saver = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                uploadCache();
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException exc) {
                    break;
                }
            }
        });
        saver.setName("CacheSaver");
        saver.setDaemon(true);
        saver.start();

        logger.info("Start cache loader every " + millis / 1000D + " seconds");
    }
    //*****************************************************


    //****************Manage players cache*****************
    private void createUser(int id) {
        try {
            connection
                    .createStatement()
                    .execute("INSERT INTO USERS(ID) VALUES(" + id + ")");
            logger.debug("User with id = " + id + " was successful created");
        } catch (SQLException exc) {
            logger.error(exc);
        }
    }

    private Player findPlayer(int id) {
        try {
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
        } catch (SQLException | IOException exc) {
            logger.error(exc);
            return null;
        }
    }

    private void createMap(map.Map map) {
        try {
            pc.clean(this::deleteMap);
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
                                    Base64.getEncoder().encodeToString(outputStream.toByteArray()) +
                                    "')"
                    );
            pc.load(map);
            logger.debug("Map with hash = " + map.hashCode() + " was successful uploaded to database");
        } catch (SQLException | IOException exc) {
            logger.error(exc);
        }
    }

    private void deleteMap(int map_hash) {
        try {
            connection
                    .createStatement()
                    .execute("DELETE FROM maps WHERE map_hash = " + map_hash);
            logger.debug("Map with hash = " + map_hash + " was successful deleted");
        } catch (SQLException exc) {
            logger.error(exc);
        }
    }

    private void uploadCache() {
        try {
            logger.debug("Uploading cache(size = " + pc.maps().size() + ")");
            for (map.Map map : pc.maps()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                oos.writeObject(map);
                int map_hash = map.hashCode();
                String encoded = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                connection.createStatement().execute("UPDATE MAPS SET SERIALIZED_MAP = '" + encoded + "' WHERE MAP_HASH = " + map_hash);
                logger.debug("Successful uploading map with hash = " + map_hash);
            }
        } catch (IOException | SQLException exc) {
            logger.error(exc);
        }
    }

    public void leaveGame(int id) {
        pc.findMap(id).removePlayer(id);
        pc.clean(this::deleteMap);
        try {
            connection.createStatement().execute("UPDATE USERS SET MAP_HASH = null WHERE ID = " + id);
        } catch (SQLException exc) {
            logger.error(exc);
        }
    }
    //*****************************************************


    //*******************Manage messages*******************
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
            logger.error(exc);
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
            logger.error(exc);
            return 0;
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
            logger.error(exc);
        }
    }

    private void message(int id, String text, double showSeconds) {
        new Thread(() -> {
            int message = send(
                    new Message(text),
                    id
            );
            try {
                Thread.sleep((long) (showSeconds * 1000));
            } catch (InterruptedException exc) {
                logger.error(exc);
            }
            delete(id, message);
        }).start();
    }
    //*****************************************************


    //********************Manage status********************
    private String readStatus(int id) {
        try {
            ResultSet rs = connection
                    .createStatement()
                    .executeQuery("SELECT STATUS FROM USERS WHERE ID = " + id);
            if (!rs.next())
                return "";
            return rs.getString("STATUS");
        } catch (SQLException exc) {
            logger.error(exc);
            return "";
        }
    }

    private void writeStatus(int id, String status) {
        try {
            connection
                    .createStatement()
                    .execute("UPDATE USERS SET STATUS = '" + status + "' WHERE ID = " + id);
        } catch (SQLException exc) {
            logger.error(exc);
        }
    }

    private Message joinMessage() {
        try {
            ResultSet rs = connection
                    .createStatement()
                    .executeQuery("SELECT * FROM USERS");
            Message message = new Message("Выберите игру:");
            while (rs.next()) {
                String[] args = rs.getString("STATUS").split(" ");
                if (args[0].equals("create"))
                    message.addRow(args[1] + ":join " + args[1]);
            }
            return message;
        } catch (SQLException exc) {
            logger.error(exc);
            return null;
        }
    }
    //*****************************************************


    //******************Action listeners*******************
    private void onText(int id, String message) {
        String[] status = readStatus(id).split(" ");
        Player player = findPlayer(id);
        switch (message) {
            case "/leave_game":
                switch (status[0]) {
                    case "join":
                    case "game":
                        writeStatus(id, "");
                        message(id, "Вы успешно покинули текущую игру!", 5);
                        return;
                    case "create":
                        try {
                            ResultSet rs = connection
                                    .createStatement()
                                    .executeQuery("SELECT * FROM USERS");
                            while (rs.next()) {
                                String[] arr = rs.getString("STATUS").split(" ");
                                if (arr[0].equals("join")) {
                                    arr = arr[1].split(":");
                                    if (arr[0].equals(status[1])) {
                                        int i = rs.getInt("ID");
                                        delete(Integer.parseInt(arr[1]), i);
                                        writeStatus(i, "");
                                    }
                                }
                            }
                            writeStatus(id, "");
                        } catch (SQLException exc) {
                            logger.error(exc);
                        }
                        message(id, "Вы успешно покинули текущую игру!", 5);
                        return;
                    default:
                        if (player != null) {
                            leaveGame(id);
                            player.removeInstance(this);
                            message(id, "Вы успешно покинули текущую игру!", 5);
                        } else {
                            message(id, "На данный момент у вас нет активной игры!", 5);
                        }
                }
                return;
            case "/game":
                if (
                        player != null
                                || status[0].equals("join")
                                || status[0].equals("create")
                                || status[0].equals("game")
                        ) {
                    message(id, "Вы уже в игре!", 5);
                    return;
                }
                send(
                        new Message("Игра:")
                                .addRow("Присоединиться:join_game")
                                .addRow("Одиночная игра:single_game")
                                .addRow("Многопользовательская игра:multi_player_game"),
                        id
                );
                return;
            default:
                if (status[0].equals("game")) {
                    send(
                            new Message("Новая игра: '" + message + "'. 0 человек присоединилось!")
                                    .addRow("⥁:⥁")
                                    .addRow("Начать:start_game"),
                            id
                    );
                    writeStatus(id, "create " + message);
                }
                break;
        }
    }

    private void onCallbackQuery(int id, int message_id, String data) {
        String[] status = readStatus(id).split(" ");
        Player player = findPlayer(id);
        String[] args = data.split(" ");
        switch (args[0]) {
            case "join_game":
                if (
                        player != null
                                || status[0].equals("join")
                                || status[0].equals("create")
                                || status[0].equals("game")
                        ) {
                    message(id, "Вы уже в игре!", 5);
                    delete(id, message_id);
                    return;
                }
                Message message = joinMessage();
                if (message != null)
                    send(message, id);
                delete(id, message_id);
                return;
            case "join":
                String game = data.substring(5);
                int messageId = send(
                        new Message("Вы зарегестрировались в '" + game + "' игре. Ожидайте, пока создатель игры начнёт её или выйдите из игры: /leave_game"),
                        id
                );
                writeStatus(id, "join " + game + ":" + messageId);
                delete(id, message_id);
                return;
            case "single_game":
                Map map = new MapGenerator().generateMap(id);
                createMap(map);
                map.player(0).newInstance(this);
                delete(id, message_id);
                return;
            case "multi_player_game":
                writeStatus(id, "game");
                send(new Message("Введите название игры:"), id);
                delete(id, message_id);
                return;
            case "⥁":
                if (!status[0].equals("create"))
                    return;
                try {
                    game = status[1];
                    ResultSet rs = connection
                            .createStatement()
                            .executeQuery("SELECT * FROM USERS");
                    int count = 0;
                    while (rs.next()) {
                        String[] arr = rs.getString("STATUS").split(" ");
                        if (arr[0].equals("join") && arr[1].split(":")[0].equals(game))
                            count++;
                    }
                    edit(
                            new Message("Новая игра: '" + status[1] + "'. " + count + " человек присоединилось!")
                                    .addRow("⥁:⥁")
                                    .addRow("Начать:start_game"),
                            id,
                            message_id
                    );
                } catch (SQLException exc) {
                    logger.error(exc);
                }
                return;
            case "start_game":
                try {
                    if (!status[0].equals("create"))
                        return;
                    game = Joiner.on(" ").join(Arrays.stream(status).skip(1).toArray());
                    ResultSet rs = connection
                            .createStatement()
                            .executeQuery("SELECT * FROM USERS");
                    List<Integer> ids = new ArrayList<>();
                    ids.add(id);
                    while (rs.next()) {
                        String[] cStatus = rs.getString("STATUS").split(" ");
                        int cId = rs.getInt("ID");
                        if (cStatus[0].equals("join")) {
                            String[] arg = cStatus[1].split(":");
                            if (arg[0].equals(game)) {
                                ids.add(cId);
                                delete(cId, Integer.parseInt(arg[1]));
                                writeStatus(cId, "");
                            }
                        }
                    }
                    map = new MapGenerator().generateMap(
                            Arrays
                                    .stream(ids.toArray())
                                    .map(Object::toString)
                                    .mapToInt(Integer::parseInt)
                                    .toArray()
                    );
                    createMap(map);
                    for (Player plr : map.players())
                        plr.newInstance(this);
                    delete(id, message_id);
                    writeStatus(id, "");
                } catch (SQLException exc) {
                    logger.error(exc);
                }
                return;
            default:
                if (player != null)
                    player.action(this, data);
                break;
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText())
                onText(
                        (int) (long) update.getMessage().getChatId(),
                        update.getMessage().getText()
                );
            else if (update.hasCallbackQuery())
                onCallbackQuery(
                        update.getCallbackQuery().getFrom().getId(),
                        update.getCallbackQuery().getMessage().getMessageId(),
                        update.getCallbackQuery().getData()
                );
        } catch (Exception exc) {
            logger.error(exc);
        }
    }
    //*****************************************************


    //*******************Bot properties********************
    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
    //*****************************************************
}