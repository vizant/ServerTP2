package entities;

import communication.PlayerMove;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import utils.GameUtils;
import utils.Exceptions.NoSuchPlayerInGameException;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Game {
    private String name;
    private String word;
    private List<PlayerMove> playerMoves;
    private Player wisher;
    private Player guesser;
    private transient boolean isGameOver;
    private transient boolean isGameSaved;

    @Builder
    public Game(Player wisher, Player guesser, String word) {
        this.wisher = wisher;
        this.guesser = guesser;
        this.word = word;
        this.name = GameUtils.generateGameName(wisher, guesser);
        this.playerMoves = new ArrayList<>();
    }

    public void addPlayerMove(PlayerMove playerMove) throws NoSuchPlayerInGameException {
        if(!playerMove.getPlayer().equals(wisher)
                && !playerMove.getPlayer().equals(guesser))
            throw new NoSuchPlayerInGameException();
        playerMoves.add(playerMove);
        GameUtils.save(this);
    }

    public Role getPlayerRoleByName(String name) throws NoSuchPlayerInGameException {
        if(wisher.getName().equals(name))
            return wisher.getRole();
        else if(guesser.getName().equals(name))
            return guesser.getRole();
        throw new NoSuchPlayerInGameException();
    }

    @Override
    public String toString() {
        return playerMoves.stream()
                .map(playerMove -> playerMove.getPlayer().getName() +
                        ": " + playerMove.getInformation())
                .reduce((x, y) -> x + "\n" + y).orElse("");
    }
}
