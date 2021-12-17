package communication.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    protected String information;
    private MessageType messageType;

    public enum MessageType {
        PLAYER_MSG, SERVER_MSG
    }
}
