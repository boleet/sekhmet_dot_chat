package nl.utwente.sekhmet.jpa;

import nl.utwente.sekhmet.api.RestConversationApiController;
import nl.utwente.sekhmet.api.RestTestApiController;
import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.repositories.*;
import nl.utwente.sekhmet.ut.UTOAuth2User;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConversationEndpointTest {
    @Autowired
    private TestRepository testRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;

    private RestConversationApiController rca;

    private UTOAuth2User student;
    private UTOAuth2User unattachedStudent;
    private UTOAuth2User unattachedTeacher;
    private UTOAuth2User teacher;
    private UTOAuth2User moduleCoordinator;
    private UTOAuth2User systemAdmin;

    Test t;
    long tid;

    @Before
    public void setup() {
        moduleCoordinator = BaseTester.setUpDummyUser(9990009990L, userRepository);
        teacher = BaseTester.setUpDummyUser(9990009991L, userRepository);
        unattachedTeacher = BaseTester.setUpDummyUser(9990009992L, userRepository);
        student = BaseTester.setUpDummyUser(9990009993L, userRepository);
        unattachedStudent = BaseTester.setUpDummyUser(9990009994L, userRepository);
        systemAdmin = BaseTester.setUpDummyUser(9990009999L, userRepository);

        rca = new RestConversationApiController(messageRepository, conversationRepository, testRepository, userRepository, enrollmentRepository);

        t = new ArrayList<>(courseRepository.findById(112233445566L).get().getTests()).get(0);
        tid = t.getId();
    }

    @After
    public void cleanUp() {
        t = testRepository.findTestById(tid);
        t.setEndTime(null);
        t.setStartTime(null);
        t = testRepository.save(t);
    }

    @org.junit.Test
    @Transactional
    public void testAllConversations() {
        RestTestApiController rta = new RestTestApiController(testRepository, courseRepository, enrollmentRepository, conversationRepository, messageRepository);
        rta.openTest( tid, true, systemAdmin);
        ResponseEntity re = rca.getAllConversation(tid, teacher);
        assertEquals(re.getStatusCode(), HttpStatus.OK);
        try {
            List<Conversation> cc = conversationRepository.findConversationByTest(t);
            assertEquals(cc.size() >= 2, true);
            assertEquals(new JSONObject(re.getBody().toString()).getJSONObject("chats").length(), cc.size());
            ResponseEntity re2 = rca.getAllConversation(tid, student);
            assertEquals(new JSONObject(re2.getBody().toString()).getJSONObject("chats").length(), 2);
        } catch (JSONException e) {

        }
        rta.closeTest(tid, systemAdmin);
    }

    @org.junit.Test
    public void testAllConversationsSecurity() throws JSONException{
        RestTestApiController rta = new RestTestApiController(testRepository, courseRepository, enrollmentRepository, conversationRepository, messageRepository);
        rta.openTest( tid, true, systemAdmin);
        List<Conversation> cc = conversationRepository.findConversationByTest(t);

        ResponseEntity re = rca.getAllConversation(tid, systemAdmin);
        assertEquals(re.getStatusCode(), HttpStatus.OK);

        re = rca.getAllConversation(tid, moduleCoordinator);
        assertEquals(re.getStatusCode(), HttpStatus.OK);
        assertEquals(new JSONObject(re.getBody().toString()).getJSONObject("chats").length(), cc.size());

        re = rca.getAllConversation(tid, teacher);
        assertEquals(re.getStatusCode(), HttpStatus.OK);
        assertEquals(new JSONObject(re.getBody().toString()).getJSONObject("chats").length(), cc.size());

        re = rca.getAllConversation(tid, unattachedTeacher);
        assertEquals(re.getStatusCode(), HttpStatus.OK);
        assertEquals(new JSONObject(re.getBody().toString()).length(), 0);

        re = rca.getAllConversation(tid, student);
        assertEquals(re.getStatusCode(), HttpStatus.OK);
        assertEquals(new JSONObject(re.getBody().toString()).getJSONObject("chats").length(), 2);

        re = rca.getAllConversation(tid, unattachedStudent);
        assertEquals(re.getStatusCode(), HttpStatus.OK);
        assertEquals(new JSONObject(re.getBody().toString()).length(), 0);

        rta.closeTest(tid, systemAdmin);
    }
}
