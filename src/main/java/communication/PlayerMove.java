package communication;

import communication.message.PlayerMessage;
import entities.Player;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerMove {
    private Player player;
    private String information;

    public PlayerMove(String information, Player player) {
        this.information = information;
        this.player = player;
    }

    public PlayerMove(PlayerMessage playerMessage, Player player){
        this.information = playerMessage.getInformation();
        this.player = player;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", player.getName(), information);
    }
}
