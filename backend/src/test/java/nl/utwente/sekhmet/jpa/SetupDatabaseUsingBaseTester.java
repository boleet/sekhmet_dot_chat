package nl.utwente.sekhmet.jpa;

import nl.utwente.sekhmet.jpa.repositories.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SetupDatabaseUsingBaseTester {
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

    @Test
    public void nothing() {
        //set the @Test annotation above setup for setup, cleanup for cleanup
    }


    public void setup() {
        BaseTester.setupTestDatabase(userRepository, courseRepository, conversationRepository, enrollmentRepository, testRepository, messageRepository);
    }


    @Transactional
    public void cleanup() {
        BaseTester.cleanupTestDatabase(userRepository, courseRepository, conversationRepository, enrollmentRepository, testRepository, messageRepository);
    }



}
