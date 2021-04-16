package nl.utwente.sekhmet.jpa;

import nl.utwente.sekhmet.api.RestCourseApiController;
import nl.utwente.sekhmet.jpa.repositories.CourseRepository;
import nl.utwente.sekhmet.jpa.repositories.EnrollmentRepository;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import nl.utwente.sekhmet.ut.UTOAuth2User;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;


@RunWith(SpringRunner.class)
@SpringBootTest
public class CourseEndpointTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private RestCourseApiController rca;
    private UTOAuth2User student;
    private UTOAuth2User unattachedStudent;
    private UTOAuth2User unattachedTeacher;
    private UTOAuth2User teacher;
    private UTOAuth2User moduleCoordinator;
    private UTOAuth2User systemAdmin;


    @Before
    public void setup() {

        moduleCoordinator = BaseTester.setUpDummyUser(9990009990L, userRepository);
        teacher = BaseTester.setUpDummyUser(9990009991L, userRepository);
        unattachedTeacher = BaseTester.setUpDummyUser(9990009992L, userRepository);
        student = BaseTester.setUpDummyUser(9990009993L, userRepository);
        unattachedStudent = BaseTester.setUpDummyUser(9990009994L, userRepository);
        systemAdmin = BaseTester.setUpDummyUser(9990009999L, userRepository);

        rca = new RestCourseApiController(userRepository, courseRepository, null, enrollmentRepository);

    }

    @After
    public void cleanUp() {

    }

    @Test
    public void testEnrolledCourses() {
        ResponseEntity re1 =  rca.getCoursesByUser(teacher);
        ResponseEntity re2 =  rca.getCoursesByUser(student);
        assertEquals(re1.getStatusCode(), HttpStatus.OK);
        try {
            //teacher is enrolled in 2 course
            JSONObject jo1 = new JSONObject(re1.getBody().toString()).getJSONObject("courses");
            assertEquals(jo1.length(), 2);
            //student is enrolled in 2 courses
            JSONObject jo2 = new JSONObject(re2.getBody().toString()).getJSONObject("courses");
            assertEquals(jo2.length(), 2);

            //count amount of tests that the persons are enrolled in in that course
            assertEquals(jo1.getJSONObject("1122334455").getJSONObject("tests").length(), 2);
            assertEquals(jo2.getJSONObject("1122334455").getJSONObject("tests").length(), 2);
            assertEquals(jo2.getJSONObject("112233445566").getJSONObject("tests").length(), 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCourseInfo() {
        ResponseEntity re1 = rca.getCourseInfo(1122334455L, moduleCoordinator);
        ResponseEntity re2 = rca.getCourseInfo(1122334455999L, teacher);
        assertEquals(re1.getStatusCode(), HttpStatus.OK);
        assertEquals(re2.getStatusCode(), HttpStatus.FORBIDDEN);
        try {
            JSONObject jo = new JSONObject(re1.getBody().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateCourse() {
        String json1 = "{ \"name\": \"$CHANGEDNAME$\", \"module_coordinator\":9990009991}";
        String json2 = "{ \"name\": \"$UNCHANGEDNAME$\", \"module_coordinator\":99999999999999999 }";
        String json3 = "{ \"name\": \"$UNCHANGEDNAME$\", \"module_coordinator\":9990009994}";
        String json4 = "{ \"name\": \"$UNIQUENAME2$\", \"module_coordinator\":9990009990}";
        assertEquals(rca.updateCourse(-1L, "{}", systemAdmin).getStatusCode(), HttpStatus.NOT_FOUND);
        assertEquals(rca.updateCourse(112233445566L, json2, systemAdmin).getStatusCode(), HttpStatus.NOT_FOUND);
        assertEquals(rca.updateCourse(112233445566L, json3, systemAdmin).getStatusCode(), HttpStatus.NOT_ACCEPTABLE);
        assertEquals(courseRepository.findById(112233445566L).get().getModuleCoordinator().getId(), moduleCoordinator.getUser().getId());
        assertEquals(rca.updateCourse(112233445566L, json1, systemAdmin).getStatusCode(), HttpStatus.OK);
        assertEquals(courseRepository.findById(112233445566L).get().getName(), "$CHANGEDNAME$");
        assertEquals(courseRepository.findById(112233445566L).get().getModuleCoordinator().getId(), teacher.getUser().getId());
        assertEquals(rca.updateCourse(112233445566L, json4, systemAdmin).getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void testSecurityInfo() {
        assertEquals(rca.getCourseInfo(1122334455L, systemAdmin).getStatusCode(), HttpStatus.OK);
        assertEquals(rca.getCourseInfo(1122334455L, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(rca.getCourseInfo(1122334455L, teacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rca.getCourseInfo(1122334455L, unattachedTeacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rca.getCourseInfo(1122334455L, student).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rca.getCourseInfo(1122334455L, unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @Test
    public void testSecurityFindEnrolled() {
        Long l = courseRepository.count();
        try {
            JSONObject jo1 = new JSONObject(rca.getCoursesByUser(systemAdmin).getBody().toString()).getJSONObject("courses");
            assertEquals(jo1.length(), l);
            jo1 = new JSONObject(rca.getCoursesByUser(moduleCoordinator).getBody().toString()).getJSONObject("courses");
            assertEquals(jo1.length(), 2);
            jo1 = new JSONObject(rca.getCoursesByUser(teacher).getBody().toString()).getJSONObject("courses");
            assertEquals(jo1.length(), 2);
            jo1 = new JSONObject(rca.getCoursesByUser(unattachedTeacher).getBody().toString());
            assertEquals(jo1.length(), 0);
            jo1 = new JSONObject(rca.getCoursesByUser(student).getBody().toString()).getJSONObject("courses");
            assertEquals(jo1.length(), 2);
            jo1 = new JSONObject(rca.getCoursesByUser(unattachedStudent).getBody().toString());
            assertEquals(jo1.length(), 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSecurityUpdate() {
        String json = "{ \"canvas_id\":112233445566, \"module_coordinator\": 9990009990, \"name\":\"$UNIQUENAME2$\"}";
        assertEquals(rca.updateCourse(112233445566L, json, systemAdmin).getStatusCode(), HttpStatus.OK);
        assertEquals(rca.updateCourse(112233445566L, json, moduleCoordinator).getStatusCode(), HttpStatus.OK);
        assertEquals(rca.updateCourse(112233445566L, json, teacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rca.updateCourse(112233445566L, json, unattachedTeacher).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rca.updateCourse(112233445566L, json, student).getStatusCode(), HttpStatus.FORBIDDEN);
        assertEquals(rca.updateCourse(112233445566L, json, unattachedStudent).getStatusCode(), HttpStatus.FORBIDDEN);
    }
}
