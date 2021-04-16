package nl.utwente.sekhmet.jpa.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.aspectj.apache.bcel.util.ClassLoaderRepository;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "conversation")
public class Conversation {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user1;

    @OneToOne
    private User user2;

    @ManyToOne
    private Test test;

    @Column(columnDefinition = "boolean default false")
    private Long unreadForStudent = 0L;

    private Long unreadForTeacher = 0L;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Message> messages;

    protected Conversation() {}

    public Conversation(User user1, User user2, Test test) {
        this.user1 = user1;
        this.user2 = user2;
        this.test = test;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public static JsonObject conversationToJsonElement(Conversation conversation, List<Message> messageList, JsonElement users, char role, boolean isAnnouncement) {
        Long testId = conversation.getTest().getId();
        Long cid = conversation.getId();
        Long subject1;
        Long subject2;
        User u = conversation.getUser1();
        if (u == null) {
            subject1 = null;
        } else {
            subject1 = u.getId();
        }
        u = conversation.getUser2();
        if (u == null) {
            subject2 = null;
        } else {
            subject2 = u.getId();
        }
        Map<Long, Message> idList = new HashMap<>();
        Map<Long, Long> idToTime = new HashMap<>();
        for (int i = messageList.size()-1; i >= 0; i --) {
            Message m = messageList.get(i);
            if (!idList.containsKey(m.getMid())) {
                idList.put(m.getMid(), m);
            }
        }
        JsonObject res = new JsonObject();

        res.addProperty("chat_id", cid);
        res.addProperty("test_id", testId);
        res.addProperty("subject1_id",subject1);
        res.addProperty("subject2_id",subject2);
        res.addProperty("unread",conversation.isUnread(role));
        res.addProperty("newest_message",getNewestMessageKey(messageList));
        res.add("people",users);
        res.add("messages",Message.messageToJsonObject(messageList,role,isAnnouncement));
        return res;

    }
    public static String conversationToJson(Conversation conversation, List<Message> messageList, String users, char role, boolean isAnnouncement) {
        String testId = conversation.getTest().getId().toString();
        String cid = conversation.getId().toString();
        String subject1;
        String subject2;
        User u = conversation.getUser1();
        if (u == null) {
            subject1 = "null";
        } else {
            subject1 = u.getId().toString();
        }
        u = conversation.getUser2();
        if (u == null) {
            subject2 = "null";
        } else {
            subject2 = u.getId().toString();
        }
        Map<Long, Message> idList = new HashMap<>();
        Map<Long, Long> idToTime = new HashMap<>();
        for (int i = messageList.size()-1; i >= 0; i --) {
            Message m = messageList.get(i);
            if (!idList.containsKey(m.getMid())) {
                idList.put(m.getMid(), m);
            }
        }

        String json = "{ \"chat_id\":" + cid + ", " +
                "\"test_id\":" + testId + ", " +
                "\"subject1_id\":" + subject1 + ", " +
                "\"subject2_id\":" + subject2 + ", " +
                "\"unread\":" + conversation.isUnread(role) + ", " +
                "\"newest_message\":" + getNewestMessageKey(messageList) + "," +
                users +
                Message.messageToJson(messageList, "\"test_id\":" + testId, role, isAnnouncement) +
                "}";
        return json;
    }

    public Long isUnread(char role) {
        switch (role) {
            case Enrollment.Role.STUDENT:
                return  unreadForStudent;
            case Enrollment.Role.TEACHER:
                return unreadForTeacher;
        }
        return 0L;
    }

    //add is true for xtra unread, false for all read
    public void setRead(char role, Long add) {
        switch (role) {
            case Enrollment.Role.STUDENT:
                this.unreadForStudent = add;
                break;
            case Enrollment.Role.TEACHER:
                unreadForTeacher = add;
                break;
        }
    }

    public boolean equals(Conversation conversation) {
        if (conversation == null) {
            return false;
        }
        return this.getId().equals(conversation.getId());
    }

    public Set<Message> getMessages() {
        return messages;
    }

    public static String getNewestMessageKey(List<Message> messages) {
        List<Message> msg = messages.stream().sorted().collect(Collectors.toList());
        Map<String, Long> idToTimestamp = new HashMap<>();
        for (Message m : msg) {
            String key = m.getSender().getId() + ":" + m.getMid();
            Long temp;
            if ((temp = idToTimestamp.get(key)) == null || temp > m.getTimestamp()) {
                idToTimestamp.put(key, m.getTimestamp());
            }
        }
        String max = null;
        for (String key: idToTimestamp.keySet()) {
            if (max == null || idToTimestamp.get(key) > idToTimestamp.get(max)) {
                max = key;
            }
        }
        if (max == null) {
            return "null";
        } else {
            return "" + max + "";
        }

    }

}
