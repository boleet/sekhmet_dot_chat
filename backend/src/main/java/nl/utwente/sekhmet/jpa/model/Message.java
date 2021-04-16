package nl.utwente.sekhmet.jpa.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import reactor.core.publisher.Mono;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
//@IdClass(MessageId.class)
@Table(name = "message")
public class Message implements Serializable, Comparable<Message> {
    @EmbeddedId
    private MessageId messageId;

    @MapsId("sender_id")
    @ManyToOne(targetEntity=User.class)
    private User sender;

    @MapsId("conversation_id")
    @ManyToOne(targetEntity = Conversation.class)
    private Conversation conversation;

    private boolean visible;

    @Column(columnDefinition="TEXT")
    private String content;

    public Message() {}

    public Message(Long mid, Long timestamp, User sender, Conversation conversation, String content) {
        this.content = content;
        visible = true;
        this.messageId = new MessageId(mid, timestamp, conversation.getId(), sender.getId());
        setSender(sender);
        setConversation(conversation);
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
        this.messageId.senderId = sender.getId();
    }

    public Long getTimestamp() {
        return this.messageId.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.messageId.timestamp = timestamp;
    }

    public Long getMid() {
        return this.messageId.mid;
    }

    public void setMid(Long mid) {
        this.messageId.mid = mid;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
        this.messageId.conversationId = conversation.getId();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public static String messageToJson(Message message, String insertion) {
        String cid = message.getConversation().getId().toString();
        String mid = message.getMid().toString();
        String time = message.getTimestamp().toString();
        String sender = message.getSender().getId().toString();
        String content = message.getContent();
        String json = "\""+ sender + ":" + message.getMid() + "\":" +
                "{ \"message_id\":" + mid + ", " +
                "\"chat_id\":" + cid + ", " +
                "\"timestamp\":" + time + ", " +
                "\"sender_id\":" + sender + ", " +
                insertion +
                "\"visible\":" + message.getVisible() + "," +
                "\"content\":\"" + fastReplace(content) + "\"}";
        return json;
    }

    public static String messageToJson(Message message, String insertion, char role, Long t, boolean isAnnouncement) {
        String cid = message.getConversation().getId().toString();
        String mid = message.getMid().toString();
        String time = t != null ? t.toString() : message.getTimestamp().toString();
        String sender = message.getSender().getId().toString();
        String content = message.getContent();
        if (isAnnouncement && !message.getVisible()) {
            content = "This announcement has been deleted";
        } else {
            if (role == 'S' && !message.getVisible()) {
                content = "This message has been deleted";
            }
        }

        String json = "\""+ sender + ":" + message.getMid() + "\":" +
                "{ \"message_id\":" + mid + ", " +
                "\"chat_id\":" + cid + ", " +
                "\"timestamp\":" + time + ", " +
                "\"sender_id\":" + sender + ", " +
                insertion +
                "\"visible\":" + message.getVisible() + "," +
                "\"content\":\"" + fastReplace(content) + "\"}";
        return json;
    }

    public static JsonObject messageToJsonObject(Collection<Message> msg, char role, boolean isAnnouncement) {
        JsonObject res = new JsonObject();

        Map<String, Message> idWithSenderToMessage = new HashMap<>();
        Map<String, Boolean> messageDoubles = new HashMap<>();
        Map<String, Long> idToTimestamp = new HashMap<>();

        for (Message m : msg) {
            Long id = m.getMid();
            String key = id + ":" + m.getSender().getId();
            if (idWithSenderToMessage.containsKey(key)) {
                if (idWithSenderToMessage.get(key).getTimestamp() < m.getTimestamp()) {
                    //update to store newer message
                    idWithSenderToMessage.put(key, m);
                }
                if (idToTimestamp.get(key) > m.getTimestamp()) {
                    idToTimestamp.put(key, m.getTimestamp());
                }
                messageDoubles.put(key, true);
            } else {
                messageDoubles.put(key, false);
                idWithSenderToMessage.put(key, m);
                idToTimestamp.put(key, m.getTimestamp());
            }
        }
        LinkedHashMap<String, Long> reverseSortedMap = new LinkedHashMap<>();
        idToTimestamp.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));

        for (String key : reverseSortedMap.keySet()) {
            Message m = idWithSenderToMessage.get(key);
            JsonObject messageObject = new JsonObject();

            String content = m.getContent();

            if (isAnnouncement && !m.getVisible()) {
                content = "This announcement has been deleted";
            } else {
                if (role == 'S' && !m.getVisible()) {
                    content = "This message has been deleted";
                }
            }

            messageObject.addProperty("message_id",m.getMid());
            messageObject.addProperty("chat_id",m.getConversation().getId());
            messageObject.addProperty("test_id",m.getConversation().getTest().getId());
            messageObject.addProperty("timestamp", m.getTimestamp());
            messageObject.addProperty("sender_id", m.getSender().getId());
            messageObject.addProperty("content", content);
            messageObject.addProperty("visible", m.getVisible());
            messageObject.addProperty("updated",messageDoubles.get(key));

            res.add(m.getSender().getId() +":"+ m.getMid(),messageObject);
        }

        return res;
    }

    public static String messageToJson(Collection<Message> msg, String extraInsertion, char role, boolean isAnnouncement) {
        StringBuilder sb = new StringBuilder("\"messages\":{");
        boolean first = true;

        Map<String, Message> idWithSenderToMessage = new HashMap<>();
        Map<String, Boolean> messageDoubles = new HashMap<>();
        Map<String, Long> idToTimestamp = new HashMap<>();

        for (Message m : msg) {
            Long id = m.getMid();
            String key = id + ":" + m.getSender().getId();
            if (idWithSenderToMessage.containsKey(key)) {
                if (idWithSenderToMessage.get(key).getTimestamp() < m.getTimestamp()) {
                    //update to store newer message
                    idWithSenderToMessage.put(key, m);
                }
                if (idToTimestamp.get(key) > m.getTimestamp()) {
                    idToTimestamp.put(key, m.getTimestamp());
                }
                messageDoubles.put(key, true);
            } else {
                messageDoubles.put(key, false);
                idWithSenderToMessage.put(key, m);
                idToTimestamp.put(key, m.getTimestamp());
            }
        }
        LinkedHashMap<String, Long> reverseSortedMap = new LinkedHashMap<>();
        idToTimestamp.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));

        for (String key : reverseSortedMap.keySet()) {
            Message m = idWithSenderToMessage.get(key);
            if (!first) {
                sb.append(", ");
            }
            first = false;
            //String key = m.getMid() + ":" + m.sender.getId();
            String insertion = "\"updated\":" + messageDoubles.get(key) + ",";
            if (!extraInsertion.equals("")) {
                insertion += extraInsertion + ", ";
            }
            sb.append(Message.messageToJson(m, insertion, role, idToTimestamp.get(key), isAnnouncement));
        }
        return sb.append("}").toString();
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setMessageId(MessageId messageId) {
        this.messageId = messageId;
    }

    public MessageId getMessageId() {
        return this.messageId;
    }

    static String fastReplace( String str) {
        int targetLength = "\n".length();
        if( targetLength == 0 ) {
            return str;
        }
        int idx2 = str.indexOf( "\n" );
        if( idx2 < 0 ) {
            return str;
        }
        StringBuilder buffer = new StringBuilder( targetLength > "\\n".length() ? str.length() : str.length() * 2 );
        int idx1 = 0;
        do {
            buffer.append( str, idx1, idx2 );
            buffer.append( "\\n" );
            idx1 = idx2 + targetLength;
            idx2 = str.indexOf( "\n", idx1 );
        } while( idx2 > 0 );
        buffer.append( str, idx1, str.length() );
        return buffer.toString();
    }

    public int compareTo(Message m) {
        if (m == null) {
            return 0;
        }
        return Long.compare(this.getTimestamp(), m.getTimestamp() );
    }
}

