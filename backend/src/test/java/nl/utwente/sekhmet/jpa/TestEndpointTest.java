package nl.utwente.sekhmet.jpa;


import nl.utwente.sekhmet.api.RestTestApiController;
import nl.utwente.sekhmet.jpa.model.Course;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.repositories.*;
import nl.utwente.sekhmet.ut.UTOAuth2User;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Transactional
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestEndpointTest {
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

    private RestTestApiController rta;

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

        rta = new RestTestApiController(testRepository, courseRepository, enrollmentRepository, conversationRepository, messageRepository);

        t = new ArrayList<>(courseRepository.findById(112233445566L).get().getTests()).get(0);
        tid = t.getId();

        cleanUp();
    }

    public void cleanUp() {
        t = testRepository.findTestById(tid);
        t.setEndTime(null);
        t.setStartTime(null);
        t = testRepository.save(t);
        //t=testRepository.findTestById(tid);
    }

    @org.junit.Test
    public void testOpen() {
        Test TEST_TEST = t;
        //open test for first time
        assertEquals(HttpStatus.OK, rta.openTest( TEST_TEST.getId(), false, teacher).getStatusCode());
        //open test while already opened
        assertEquals(HttpStatus.CONFLICT, rta.openTest( TEST_TEST.getId(), false, teacher).getStatusCode());
        //can't reopen opened test
        assertEquals(HttpStatus.CONFLICT, rta.openTest( TEST_TEST.getId(), true, teacher).getStatusCode());

        rta.closeTest( TEST_TEST.getId(), teacher);
        //cant open a closed test
        assertEquals(HttpStatus.NOT_ACCEPTABLE, rta.openTest( TEST_TEST.getId(), false, teacher).getStatusCode());
        //reopen closed test
        assertEquals(HttpStatus.OK, rta.openTest( TEST_TEST.getId(), true, teacher).getStatusCode());

        t = testRepository.findTestById(tid);
        t.setEndTime(0L);
        t = testRepository.save(t);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, rta.openTest( tid, true, teacher).getStatusCode());
        assertEquals(HttpStatus.OK, rta.openTest( tid, true, moduleCoordinator).getStatusCode());
    }

    @org.junit.Test
    public void testClosed() {
        Test TEST_TEST = t;
        //cant close a test not yet opened
        assertEquals(HttpStatus.CONFLICT , rta.closeTest( TEST_TEST.getId(), teacher).getStatusCode());
        rta.openTest( TEST_TEST.getId(), false, teacher);
        //close an opened test
        assertEquals(HttpStatus.OK , rta.closeTest( TEST_TEST.getId(), teacher).getStatusCode());
        //cant close a closed test
        assertEquals(HttpStatus.CONFLICT , rta.closeTest( TEST_TEST.getId(), teacher).getStatusCode());
        rta.openTest( TEST_TEST.getId(), true, teacher);
        //test has been reopened, can be closed
        assertEquals(HttpStatus.OK , rta.closeTest( TEST_TEST.getId(), teacher).getStatusCode());
    }

    @org.junit.Test
    public void testUpdate() {
        Test TEST_TEST = t;
        String json = "{" +
                "\"test_id\":" + TEST_TEST.getId() + ", " +
                "\"name\": \"" + TEST_TEST.getName() + "alt" + "\"" +
                "}";

        String json2 = "{" +
                "\"test_id\":" + TEST_TEST.getId() + ", " +
                "\"name\": \"$UNIQNAM3$\"" +
                "}";
        assertEquals(TEST_TEST.getName(), "$UNIQNAM3$");
        //only Module Coordinator can change tests
        assertEquals(rta.updateTest(t.getId(), json, teacher).getStatusCode(), HttpStatus.FORBIDDEN);
        //module coordinator can change tests
        assertEquals(rta.updateTest(t.getId(), json, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        //this test does not exist under this name anymore
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAM3$", 1122334455L).size(), 0);
        //this test has the new name
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAM3$alt", 112233445566L).size(), 1);
        rta.updateTest(t.getId(), json2, moduleCoordinator);
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAM3alt$", 112233445566L).size(), 0);
    }

    @org.junit.Test
    public void testCreate() {
        Test TEST_TEST = t;
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAM99$", 112233445566L).size(), 0);
        String json = "{" +
                "\"course_id\":" + 112233445566L + ", " +
                "\"name\": \"$UNIQNAM99$\"" +
                "}";
        assertEquals(TEST_TEST.getName(), "$UNIQNAM3$");
        assertEquals(rta.postTest(json, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        //this test has a duplicate with a new key but the same name under the same course
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAM99$", 112233445566L).size(), 1);

        ResponseEntity re1 = rta.deleteTest(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAM99$", 112233445566L).get(0).getId(), systemAdmin);
        assertEquals(re1.getStatusCode(), HttpStatus.OK);
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L).size(), 0);
        assertEquals(courseRepository.findById(112233445566L).get().getTests().size(), 1);
    }

    @org.junit.Test
    public void testDelete() {
        Course c = courseRepository.findById(112233445566L).get();
        Test t = new Test(null, null, "$UNIQNAME99$", null, null);
        c.addTest(t);
        courseRepository.save(c);
        Long id = testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L).get(0).getId();
        assertEquals(testRepository.findTestById(id).getName(), "$UNIQNAME99$");
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L).size(), 1);
        assertEquals(rta.deleteTest(id, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L).size(), 0);
        assertEquals(courseRepository.findById(112233445566L).get().getTests().size(), 1);
    }

    @org.junit.Test
    public void testSecurityCreate() {
        String json = "{" +
                "\"course_id\":112233445566, " +
                "\"name\": \"$UNIQNAME99$\"" +
                "}";
        //create: only module coordinator or assigned teacher
        assertEquals(rta.postTest(json, systemAdmin).getStatusCode(), HttpStatus.OK);
        assertEquals(rta.postTest(json, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(rta.postTest(json, teacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.postTest(json, unattachedTeacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.postTest(json, student).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.postTest(json, unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
        for (Test t : testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L)) {
            rta.deleteTest(t.getId(), systemAdmin);
        }
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L).size(), 0);
    }

    @org.junit.Test
    public void testSecurityUpdate() {
        String json = "{" +
                "\"test_id\":" + tid + ", " +
                "\"name\": \"$UNIQNAM3$\"" +
                "}";
        assertEquals(rta.updateTest(tid, json, systemAdmin).getStatusCode(), HttpStatus.OK);
        assertEquals(rta.updateTest(tid, json, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(rta.updateTest(tid, json, teacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.updateTest(tid, json, unattachedTeacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.updateTest(tid, json, unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.updateTest(tid, json, student).getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @org.junit.Test
    public void testSecurityOpenClose() {
        assertEquals(rta.openTest( tid, true, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(rta.closeTest( tid, moduleCoordinator).getStatusCode(), HttpStatus.OK);

        assertEquals(rta.openTest( tid, true, teacher).getStatusCode(), HttpStatus.OK);
        assertEquals(rta.closeTest( tid, teacher).getStatusCode(), HttpStatus.OK);

        assertEquals(rta.openTest( tid, true, unattachedTeacher).getStatusCode(), HttpStatus.FORBIDDEN);
        rta.openTest( tid, true, moduleCoordinator);
        assertEquals(rta.closeTest( tid, unattachedTeacher).getStatusCode(), HttpStatus.FORBIDDEN);
        rta.closeTest( tid, moduleCoordinator);

        assertEquals(rta.openTest( tid, true, student).getStatusCode(), HttpStatus.FORBIDDEN);
        rta.openTest( tid, true, moduleCoordinator);
        assertEquals(rta.closeTest( tid, student).getStatusCode(), HttpStatus.FORBIDDEN);
        rta.closeTest( tid, moduleCoordinator);

        assertEquals(rta.openTest( tid, true, unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
        rta.openTest( tid, true, moduleCoordinator);
        assertEquals(rta.closeTest( tid, unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
        rta.closeTest( tid, moduleCoordinator);
    }

    @org.junit.Test
    public void testSecurityDelete() {
        Course c = courseRepository.findById(112233445566L).get();
        Test test = new Test(null, null, "$UNIQNAME99$", null, null);
        c.addTest(test);
        test.addEnrollment(teacher.getUser(), 'T');
        test.addEnrollment(student.getUser(), 'S');
        courseRepository.save(c);
        Long id = testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L).get(0).getId();
        assertEquals(rta.deleteTest(id, teacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.deleteTest(id, unattachedTeacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.deleteTest(id, unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rta.deleteTest(id, student).getStatusCode(), HttpStatus.FORBIDDEN);

        assertEquals(rta.deleteTest(id, systemAdmin).getStatusCode(), HttpStatus.OK);

        c = courseRepository.findById(112233445566L).get();
        test = new Test(null, null, "$UNIQNAME99$", null, null);
        c.addTest(test);
        courseRepository.save(c);
        id = testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L).get(0).getId();
        assertEquals(rta.deleteTest(id, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAME99$", 112233445566L).size(), 0);
    }
}
