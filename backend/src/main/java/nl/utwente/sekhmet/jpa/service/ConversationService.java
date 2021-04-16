package nl.utwente.sekhmet.jpa.service;

import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.ConversationRepository;
import nl.utwente.sekhmet.jpa.repositories.TestRepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ConversationService {
    //POST-create

    public static Conversation postConversation(Test test, User user1, User user2) {
        return new Conversation(user1, user2, test);
    }

    public static boolean postConversationTestStart(User student, int chatType, Test test, ConversationRepository conversationRepository) {
        Conversation con = new Conversation(student, null, test);
        conversationRepository.save(con);
        switch (chatType) {
            case 0:
                break;
            case 1:
                //announcement
                test.setAnnouncements(con);
                break;
            case 2:
                //teacher chat
                test.setTeacherConversation(con);
                break;
        }
        return true;
    }

    public static boolean checkExistsConversation(User student, User user1, Test test, ConversationRepository conversationRepository) {
        Conversation con = conversationRepository.findConversationByTest_IdAndUser1AndUser2(test.getId(), student, user1);
        return con != null;
    }

    public static Conversation getAnnouncementConversation(Long tid, ConversationRepository conversationRepository) {
        return conversationRepository.findConversationByTest_IdAndUser1AndUser2(tid, null, null);
    }

    //GET-retrieve
    public static Conversation getConversationById(Long id, ConversationRepository conversationRepository) {
        Conversation conversation = conversationRepository.findConversationById(id);
        return conversation;
    }

    public static List<Conversation> getConversationByTest(Test test, ConversationRepository conversationRepository) {
        //List<Conversation> conversations = conversationRepository.findConversationByTest(test);
        //return conversations;
        return conversationRepository.findConversationByTest_Id(test.getId());
    }

    public static Conversation getConversationByTestIdAndUser1AndUser2(Long tid, User user1, User user2, ConversationRepository conversationRepository) {
        Conversation conversation = conversationRepository.findConversationByTest_IdAndUser1AndUser2(tid, user1, user2);
        return conversation;
    }

    public static List<Conversation> getConversationByTestId(Long tid, ConversationRepository conversationRepository) {
        Iterable<Conversation> conversationIterable = conversationRepository.findAll();
        List<Conversation> conversations = new ArrayList<>();
        for (Iterator<Conversation> it = conversationIterable.iterator(); it.hasNext();) {
            if (it.next().getTest().getId() == tid) {
                conversations.add(it.next());
            }
        }
        if (conversations.isEmpty()) {
            throw new IllegalStateException("Test_id: " + tid + " ||| This test does not have any chats!");
        }
        return conversations;
    }

    public static Conversation getAnnouncements(Long testId, ConversationRepository conversationRepository) throws NoSuchElementException {
        return conversationRepository.findConversationByTest_IdAndAndUser1(testId, null);
    }

    //PUT-update
    public static void putConversation(Conversation conversation, ConversationRepository conversationRepository){
        conversationRepository.save(conversation);
    }

    //DELETE Empty Conversations For A Test
    public static void deleteEmptyConversations(Long tid, TestRepository testRepository, ConversationRepository conversationRepository) {
        testRepository.deleteReferenceAnnouncement(3L);
        testRepository.deleteReferenceTeacherChat(3L);
        conversationRepository.deleteEmptyByTest_Id(3L);
    }

    //DELETE
    public static void deleteConversation(Long id, ConversationRepository conversationRepository){
        conversationRepository.deleteById(id);
    }
}
