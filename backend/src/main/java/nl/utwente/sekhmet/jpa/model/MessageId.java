package nl.utwente.sekhmet.jpa.model;

import javax.persistence.*;
import java.io.Serializable;

@Embeddable
public class MessageId implements Serializable {

    @Column(name = "mid", nullable = false)
    public Long mid;

    @Column(name = "timestamp")
    public Long timestamp;

    @Column(name = "conversation_id")
    public Long conversationId;

    @Column(name = "sender_id")
    public Long senderId;

    public MessageId() {

    }

    public MessageId(Long mid, Long timestamp, Long conversationId, Long senderId) {
        this.mid = mid;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
        this.senderId = senderId;
    }
}
