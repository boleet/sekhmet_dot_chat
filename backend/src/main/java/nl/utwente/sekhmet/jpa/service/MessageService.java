package nl.utwente.sekhmet.jpa.service;

import nl.utwente.sekhmet.api.StringChecker;
import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Message;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.ConversationRepository;
import nl.utwente.sekhmet.jpa.repositories.MessageRepository;
import org.json.JSONArray;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class MessageService {
    //POST-create
    public static void postMessage(String jsonMessage, UserRepository userRepository, ConversationRepository conversationRepository, MessageRepository messageRepository)
            throws JSONException, NumberFormatException, NoSuchElementException {
        String jsonMessageAfter = StringChecker.escapeString(jsonMessage);
        JSONObject json = new JSONObject(jsonMessageAfter);
        JSONArray ja = null;
        if (json.has("messages")) {
            ja = json.getJSONArray("messages");
            messageRepository.saveAll(MessageService.postMessage(ja, userRepository, conversationRepository));
        } else {
            messageRepository.save(MessageService.postMessage(json, null, null, userRepository, conversationRepository));
        }
    }
    /*
     * postMessage can be called with User AND Conversation,
     * Only User (c == null),
     * Only Conversation (user == null),
     * Or neither (user & c == null)
     */
    public static Message postMessage(JSONObject jo, User user, Conversation c, UserRepository userRepository, ConversationRepository conversationRepository)
            throws JSONException, NumberFormatException, NoSuchElementException {
        User tempUser = user;
        if (user == null) {
           tempUser = UserService.getUser(jo.getLong("sender_id"), userRepository);
        }
        Conversation tempConversation = c;
        if (c == null) {
            tempConversation = ConversationService.getConversationById(jo.getLong("chat_id"), conversationRepository);
        }
        Long mid = jo.getLong("message_id");
        Long timestamp = jo.getLong("timestamp");

        String content = jo.getString("content");
        Message message = new Message(mid, timestamp, tempUser, tempConversation, content);

        return message;
    }

    public static List<Message> postMessage(JSONArray jo, UserRepository userRepository, ConversationRepository conversationRepository)
            throws JSONException, NumberFormatException, NoSuchElementException {
        int len = jo.length();
        List<Message> res = new ArrayList<>();
        Map<Long, User> prevUsers = new HashMap<>();
        Map<Long, Conversation> prevConversations = new HashMap<>();
        for (int i = 0; i < len; i ++) {
            JSONObject temp = jo.getJSONObject(i);
            Long pid = temp.getLong("sender_id");
            User tu = prevUsers.get(pid);
            if (tu == null) {
                tu = UserService.getUser(pid, userRepository);
                prevUsers.put(pid, tu);
            }
            Long cid = temp.getLong("chat_id");
            Conversation tc = prevConversations.get(cid);
            if (tc == null) {
                tc = ConversationService.getConversationById(cid, conversationRepository);
                prevConversations.put(cid, tc);
            }
            res.add(MessageService.postMessage(temp, tu, tc, userRepository, conversationRepository));
        }
        return res;
    }

    //GET-retrieve

    public static Message getMessage(Long id, MessageRepository messageRepository){
        return messageRepository.findById(id).get();
    }

    public static List<Message> getMessageByConversation(Conversation conversation, MessageRepository messageRepository) {
        List<Message> messageList = messageRepository.findMessageByMessageId_ConversationId(conversation.getId());
        return messageList;
    }

    public static List<Message> getMessageByTest(Test test, MessageRepository messageRepository) {
        List<Message> mesList = messageRepository.findMessagesByConversationTest(test);
        return mesList;
    }


    //PUT-update


    public static void putMessage(Long mid, MessageRepository messageRepository){
    }

    //DELETE
    public static void deleteMessage(Message message, MessageRepository messageRepository){
        messageRepository.delete(message);
    }
}
