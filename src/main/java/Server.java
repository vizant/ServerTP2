import com.google.gson.Gson;
import communication.PlayerMove;
import communication.message.PlayerMessage;
import communication.message.ServerMessage;
import entities.Game;
import entities.Player;
import entities.Role;
import utils.CommunicationUtils;
import utils.Exceptions.GameNotSavedException;
import utils.Exceptions.NoSuchPlayerInGameException;
import utils.Exceptions.NotUniqueRolesException;
import utils.GameUtils;
import utils.NameReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static java.util.Objects.isNull;

public class Server {
    public static void main(String[] args) {
        System.out.println("Server is listening on the port " + args[0]);
        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));
             Socket firstSocket = serverSocket.accept();
             Socket secondSocket = serverSocket.accept();
             PrintWriter firstWriter =
                     new PrintWriter(firstSocket.getOutputStream(), true);
             BufferedReader firstReader =
                     new BufferedReader(new InputStreamReader(firstSocket.getInputStream()));
             PrintWriter secondWriter =
                     new PrintWriter(secondSocket.getOutputStream(), true);
             BufferedReader secondReader =
                     new BufferedReader(new InputStreamReader(secondSocket.getInputStream()))
        ) {
            System.out.println("Users connected!");

            Player firstPlayer = new Player();
            firstPlayer.setReader(firstReader);
            firstPlayer.setWriter(firstWriter);
            Thread firstThread = new Thread(new NameReader(firstPlayer));
            firstThread.start();

            Player secondPlayer = new Player();
            secondPlayer.setReader(secondReader);
            secondPlayer.setWriter(secondWriter);
            Thread secondThread = new Thread(new NameReader(secondPlayer));
            secondThread.start();

            firstThread.join();
            secondThread.join();

            System.out.println("First player's name: " + firstPlayer.getName());
            System.out.println("Second player's name: " + secondPlayer.getName());

            Gson gson = new Gson();
            Game game = null;
            try {
                game = GameUtils.loadIfSavedByPlayers(firstPlayer, secondPlayer);
                System.out.println("Previous game loaded!");
                System.out.println(game);

                CommunicationUtils.send(gson.toJson(true), firstWriter, secondWriter);
                CommunicationUtils.send(gson.toJson(game), firstWriter, secondWriter);

                GameUtils.setGamePlayersIfPreviousGameLoaded(firstPlayer, secondPlayer, game);

            } catch (GameNotSavedException | NotUniqueRolesException | NoSuchPlayerInGameException exception) {
                System.out.println("Previous game was not saved!");

                CommunicationUtils.send(gson.toJson(false), firstWriter, secondWriter);

                firstPlayer.setRole(Role.WISHER);
                secondPlayer.setRole(Role.GUESSER);
            }

            String firstPlayerRole = gson.toJson(firstPlayer.getRole());
            String secondPlayerRole = gson.toJson(secondPlayer.getRole());
            CommunicationUtils.send(firstPlayerRole, firstPlayer.getWriter());
            CommunicationUtils.send(secondPlayerRole, secondPlayer.getWriter());

            System.out.println("Roles sent to users!");

            Player wisher;
            Player guesser;

            if (firstPlayer.getRole() == Role.WISHER) {
                wisher = firstPlayer;
                guesser = secondPlayer;
            } else {
                wisher = secondPlayer;
                guesser = firstPlayer;
            }

            if (isNull(game)) {
                String wordJson = CommunicationUtils.receive(wisher.getReader());
                String word = gson.fromJson(wordJson, String.class);
                game = Game.builder()
                        .wisher(wisher)
                        .guesser(guesser)
                        .word(word)
                        .build();
                System.out.println("Wished word: " + word);

                ServerMessage serverMessage = ServerMessage.builder()
                        .information("Word wished by wisher!")
                        .serverMessageType(ServerMessage.ServerMessageType.SERVER_START)
                        .build();

                String serverMessageStartJson = gson.toJson(serverMessage);
                CommunicationUtils.send(serverMessageStartJson, guesser.getWriter());
            } else {

                ServerMessage serverMessage = ServerMessage.builder()
                        .information("Its your turn now!")
                        .serverMessageType(ServerMessage.ServerMessageType.SERVER_START)
                        .build();

                String serverMessageJson = gson.toJson(serverMessage);

                PlayerMove lastPlayerMove = game.getPlayerMoves().get(game.getPlayerMoves().size() - 1);
                Player lastPlayer = lastPlayerMove.getPlayer();
                Player nextPlayer;
                if (wisher.getRole().equals(lastPlayer.getRole())) {
                    nextPlayer = guesser;
                } else {
                    nextPlayer = wisher;
                }
                CommunicationUtils.send(serverMessageJson, nextPlayer.getWriter());

                if (nextPlayer == wisher) {
                    String wisherMessageJson = CommunicationUtils.receive(wisher.getReader());
                    PlayerMessage wisherMessage = gson.fromJson(wisherMessageJson, PlayerMessage.class);

                    if (!GameUtils.isGameEnded(game, wisherMessage.getInformation())) {
                        PlayerMove wisherMove = new PlayerMove(wisherMessage, wisher);
                        ServerMessage serverMessageToGuesser = ServerMessage.builder()
                                .playerMove(wisherMove)
                                .serverMessageType(ServerMessage.ServerMessageType.SERVER_GAME)
                                .build();
                        String serverMessageToGuesserJson = gson.toJson(serverMessageToGuesser);
                        CommunicationUtils.send(serverMessageToGuesserJson, guesser.getWriter());
                        game.addPlayerMove(wisherMove);
                    }
                }
            }


            while (!game.isGameOver()) {
                String guesserMessageJson = CommunicationUtils.receive(guesser.getReader());
                PlayerMessage guesserMessage = gson.fromJson(guesserMessageJson, PlayerMessage.class);

                if (GameUtils.isGameEnded(game, guesserMessage.getInformation())) {
                    break;
                }
                PlayerMove guesserMove = new PlayerMove(guesserMessage, guesser);
                ServerMessage serverMessageToWisher = ServerMessage.builder()
                        .playerMove(guesserMove)
                        .serverMessageType(ServerMessage.ServerMessageType.SERVER_GAME)
                        .build();
                String serverMessageToWisherJson = gson.toJson(serverMessageToWisher);
                CommunicationUtils.send(serverMessageToWisherJson, wisher.getWriter());
                game.addPlayerMove(guesserMove);

                String wisherMessageJson = CommunicationUtils.receive(wisher.getReader());
                PlayerMessage wisherMessage = gson.fromJson(wisherMessageJson, PlayerMessage.class);

                if (GameUtils.isGameEnded(game, wisherMessage.getInformation())) {
                    break;
                }
                PlayerMove wisherMove = new PlayerMove(wisherMessage, wisher);
                ServerMessage serverMessageToGuesser = ServerMessage.builder()
                        .playerMove(wisherMove)
                        .serverMessageType(ServerMessage.ServerMessageType.SERVER_GAME)
                        .build();
                String serverMessageToGuesserJson = gson.toJson(serverMessageToGuesser);
                CommunicationUtils.send(serverMessageToGuesserJson, guesser.getWriter());
                game.addPlayerMove(wisherMove);
            }

            if(!game.isGameSaved())
                GameUtils.clearSaveIfExists(game);

        } catch (IOException | InterruptedException | NoSuchPlayerInGameException exception) {
            exception.printStackTrace();
        }
    }
}
