package nl.utwente.sekhmet.jpa;
//User endpoint test already checks all methods within RestUserApi.java
//and all access for the endpoints

import nl.utwente.sekhmet.api.RestUserApiController;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.repositories.CourseRepository;
import nl.utwente.sekhmet.jpa.repositories.TestRepository;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
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

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@RunWith(SpringRunner.class)
@SpringBootTest

public class UserEndpointTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestRepository testRepository;
    @Autowired
    private CourseRepository courseRepository;

    private RestUserApiController rua;

    private UTOAuth2User student;
    private UTOAuth2User unattachedStudent;
    private UTOAuth2User unattachedTeacher;
    private UTOAuth2User teacher;
    private UTOAuth2User moduleCoordinator;
    private UTOAuth2User systemAdmin;

    private Test t;
    private Long tid;

    @Before
    public void setup() {
        moduleCoordinator = BaseTester.setUpDummyUser(9990009990L, userRepository);
        teacher = BaseTester.setUpDummyUser(9990009991L, userRepository);
        unattachedTeacher = BaseTester.setUpDummyUser(9990009992L, userRepository);
        student = BaseTester.setUpDummyUser(9990009993L, userRepository);
        unattachedStudent = BaseTester.setUpDummyUser(9990009994L, userRepository);
        systemAdmin = BaseTester.setUpDummyUser(9990009999L, userRepository);

        rua = new RestUserApiController(testRepository, userRepository);

        t = new ArrayList<>(courseRepository.findById(112233445566L).get().getTests()).get(0);
        tid = t.getId();
    }

    @After
    public void cleanUp() {

    }

    @org.junit.Test
    public void testSelf() {
        try {
            assertEquals(new JSONObject(rua.getMyUser(teacher).getBody()).getLong("user_id"), teacher.getUser().getId());
            assertEquals(new JSONObject(rua.getMyUser(unattachedStudent).getBody()).getLong("user_id"), unattachedStudent.getUser().getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @org.junit.Test
    public void testGetEnrolledUsers() {
        //get all users enrolled for a test (all my test users)
        Long id = testRepository.findTestsByNameAndAndCourse_CanvasId("$UNIQNAM1$", 1122334455L).get(0).getId();
        //TODO unfindable id returns 403 instead of 404
        assertEquals(rua.getTestUsers(-1L, teacher).getStatusCode(), HttpStatus.FORBIDDEN);
        ResponseEntity re = rua.getTestUsers(id, teacher);
        //Findable id
        assertEquals(re.getStatusCode(), HttpStatus.OK);
        JSONObject jo = null;
        try {
            jo = new JSONObject((String) re.getBody());
            jo = jo.getJSONObject("teachers");
        }catch (JSONException e) {
            e.printStackTrace();
        }
        assertEquals(jo.length(), 1);
    }

    @org.junit.Test
    public void getPartialUsers() {
        try {
            assertEquals(new JSONObject(rua.getUserByPartial("[]", teacher).getBody()).getJSONObject("users").length(), 0);
            assertEquals(new JSONObject(rua.getUserByPartial("9990", teacher).getBody()).getJSONObject("users").length(), 6);
            assertEquals(new JSONObject(rua.getUserByPartial("UNIQ", teacher).getBody()).getJSONObject("users").length(), 6);
            assertEquals(new JSONObject(rua.getUserByPartial("9991", teacher).getBody()).getJSONObject("users").length(), 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @org.junit.Test
    public void testSecurityEnrolled() {
        assertEquals(rua.getTestUsers(tid, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getTestUsers(tid, systemAdmin).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getTestUsers(tid, teacher).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getTestUsers(tid, unattachedTeacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rua.getTestUsers(tid, student).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rua.getTestUsers(tid, unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @org.junit.Test
    public void testSecurityPartial() {
        assertEquals(rua.getUserByPartial("$", moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getUserByPartial("$", systemAdmin).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getUserByPartial("$", teacher).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getUserByPartial("$", unattachedTeacher).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getUserByPartial("$", student).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rua.getUserByPartial("$", unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @org.junit.Test
    public void testSecuritySelf() {
        assertEquals(rua.getMyUser( moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getMyUser(systemAdmin).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getMyUser(teacher).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getMyUser(unattachedTeacher).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getMyUser(student).getStatusCode(), HttpStatus.OK);
        assertEquals(rua.getMyUser(unattachedStudent).getStatusCode(), HttpStatus.OK);
    }


}
