package communication.message;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PlayerMessage extends Message {

    @Builder
    public PlayerMessage(String information) {
        super(information, MessageType.PLAYER_MSG);
    }

}