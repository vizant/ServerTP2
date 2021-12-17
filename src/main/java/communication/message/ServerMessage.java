package communication.message;

import communication.PlayerMove;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ServerMessage extends Message {
    private final ServerMessageType serverMessageType;
    private final PlayerMove playerMove;

    @Builder
    public ServerMessage(String information, ServerMessageType serverMessageType, PlayerMove playerMove) {
        super(information, MessageType.SERVER_MSG);
        this.serverMessageType = serverMessageType;
        this.playerMove = playerMove;
    }

    public enum ServerMessageType{
        SERVER_START, SERVER_GAME, SERVER_END
    }

}
