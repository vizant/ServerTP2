package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import communication.message.ServerMessage;
import entities.Game;
import entities.Player;
import entities.Role;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import utils.Exceptions.GameNotSavedException;
import utils.Exceptions.NoSuchPlayerInGameException;
import utils.Exceptions.NotUniqueRolesException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.isNull;

public class GameUtils {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String schemeName = "scheme.json";

    public static void save(Game game) {
        try (
                PrintWriter printWriter = new PrintWriter(game.getName())
        ) {
            String gameJson = gson.toJson(game);
            printWriter.write(gameJson);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to write data!");
        }
    }

    public static Game loadIfSavedByPlayers(Player first, Player second)
            throws GameNotSavedException {
        String gameName = generateGameName(first, second);
        if(!Files.exists(Paths.get(gameName)))
            throw new GameNotSavedException();
        Game game = null;
        try (
                FileReader readerValidationScheme = new FileReader(schemeName);
                FileReader readerJsonObject = new FileReader(gameName)
        ) {
            JSONObject jsonSchema = new JSONObject(
                    new JSONTokener(readerValidationScheme));
            JSONObject jsonSubject = new JSONObject(
                    new JSONTokener(readerJsonObject));
            Schema schema = SchemaLoader.load(jsonSchema);
            schema.validate(jsonSubject);
            System.out.println(jsonSubject);
            game = gson.fromJson(jsonSubject.toString(), Game.class);
        } catch (IOException e) {
            throw new GameNotSavedException();
        }catch (ValidationException e){
            System.out.println("Validation error!");
            try {
                Files.delete(Paths.get(gameName));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            throw new GameNotSavedException();
        }
        return game;
    }


    public static String generateGameName(Player first, Player second) {
        String firstPlayerName = first.getName();
        String secondPlayerName = second.getName();
        final String gameTittle = "game";
        final String fileFormat = ".json";

        StringBuilder gameNameBuilder = new StringBuilder();
        if (firstPlayerName.compareTo(secondPlayerName) < 0)
            gameNameBuilder.append(firstPlayerName).append(secondPlayerName);
        else
            gameNameBuilder.append(secondPlayerName).append(firstPlayerName);
        gameNameBuilder.append(gameTittle).append(fileFormat);

        return gameNameBuilder.toString();
    }

    public static boolean isGameEnded(Game game, String information) {
        Function<String, ServerMessage> serverMessageFactory =
                (msg) -> ServerMessage.builder()
                        .information(msg)
                        .serverMessageType(ServerMessage.ServerMessageType.SERVER_END)
                        .build();

        BiConsumer<Game, String> sendServerMessage =
                (g, msg) -> {
                    ServerMessage endGameServerMsg = serverMessageFactory.apply(msg);
                    String endGameJson = gson.toJson(endGameServerMsg);
                    CommunicationUtils.send(endGameJson,
                            g.getWisher().getWriter(), g.getGuesser().getWriter());
                };

        if (information.equalsIgnoreCase(game.getWord())) {
            sendServerMessage.accept(game, "Game over!");
            game.setGameOver(true);
            game.setGameSaved(false);
            return true;
        }
        if (information.equalsIgnoreCase("*exit*")) {
            sendServerMessage.accept(game, "One of the players wanted to finish the game!");
            game.setGameOver(true);
            game.setGameSaved(false);
            return true;
        }
        if (information.equalsIgnoreCase("*save*")) {
            save(game);
            game.setGameOver(true);
            game.setGameSaved(true);
            sendServerMessage.accept(game, "One of the players wanted to save and finish the game!");
            return true;
        }
        return false;
    }

    public static void setGamePlayers(Player firstPlayer, Player secondPlayer, Game game) throws NotUniqueRolesException {
        if (firstPlayer.getRole().equals(Role.WISHER)
                && secondPlayer.getRole().equals(Role.GUESSER)) {
            game.setWisher(firstPlayer);
            game.setGuesser(secondPlayer);
        } else if (firstPlayer.getRole().equals(Role.GUESSER)
                && secondPlayer.getRole().equals(Role.WISHER)) {
            game.setGuesser(firstPlayer);
            game.setWisher(secondPlayer);
        } else {
            throw new NotUniqueRolesException();
        }
    }

    //устанавливаем роли в соотвествии с прошлой игрой по имени
    public static void setGamePlayersIfPreviousGameLoaded(Player firstPlayer, Player secondPlayer,
                                                          Game game) throws NoSuchPlayerInGameException, NotUniqueRolesException {
        if (isNull(firstPlayer.getRole()) || isNull(secondPlayer.getRole())) {
            firstPlayer.setRole(game.getPlayerRoleByName(firstPlayer.getName()));
            secondPlayer.setRole(game.getPlayerRoleByName(secondPlayer.getName()));
            setGamePlayers(firstPlayer, secondPlayer, game);
        }
    }

    public static void clearSaveIfExists(Game game) throws IOException {
        Files.deleteIfExists(Paths.get(game.getName()));
    }
}
