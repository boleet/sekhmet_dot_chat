package nl.utwente.sekhmet.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.utwente.sekhmet.TheSecurityEnforcer;
import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.auth.SupernaturalJdbcOAuth2AuthorizedClientService;
import nl.utwente.sekhmet.canvas.CanvasApiClient;
import nl.utwente.sekhmet.jpa.model.*;
import nl.utwente.sekhmet.jpa.repositories.*;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static nl.utwente.sekhmet.jpa.model.Enrollment.Role.STUDENT;
import static nl.utwente.sekhmet.jpa.model.Enrollment.Role.TEACHER;

@RestController
@RequestMapping(value = "/api/canvas")
public class CanvasController {

    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CanvasController(TestRepository testRepository, UserRepository userRepository, CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.testRepository = testRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

//    @Qualifier("oAuth2AuthorizedClientService")
//    @Autowired
//    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @GetMapping("/courses")
    public ResponseEntity<String> getCourses(HttpServletRequest request, @AuthenticationPrincipal SupernaturalAuthUser principal) {
        try {
            String token = this.getUsersCanvasToken(principal.getUser());
            if(token == null) {
                return new ResponseEntity<>("{\"response\": \"Unauthorized Canvas API token\"}", HttpStatus.UNAUTHORIZED);
            }
            CanvasApiClient cc = new CanvasApiClient(token);
            JsonArray courses = cc.getCoursesAvailableToImport();
            ArrayList<Long> ids = new ArrayList<>();
            for (JsonElement jsonCourse : courses) {
                ids.add(jsonCourse.getAsJsonObject().get("id").getAsLong());
            }

            List<Course> coursesInDb = (List<Course>) this.courseRepository.findAllById(ids);
            for (JsonElement jsonCourse : courses.deepCopy()) {
                Long courseId = jsonCourse.getAsJsonObject().get("id").getAsLong();
                if (coursesInDb.stream().anyMatch(o -> (o.getCanvasId() == courseId))) {
                    // this means the course is already in our system
                    courses.remove(jsonCourse);
                }
            }

            return new ResponseEntity<>(courses.toString(), HttpStatus.OK);
        } catch (WebClientResponseException e) {
            return new ResponseEntity<>("{\"response\": \"Error when retrieving data from Canvas\"}", e.getStatusCode());
        } catch (Exception e) {
            return new ResponseEntity<>("{\"response\": \"Sorry, something went wrong\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/courses/{canvasCourseId}/assignments_and_groups")
    public ResponseEntity<String> getAssignmentsAndGroups(@PathVariable(value="canvasCourseId") int canvasCourseId, @AuthenticationPrincipal SupernaturalAuthUser principal) {
        try {
            String token = this.getUsersCanvasToken(principal.getUser());
            if(token == null) {
                return new ResponseEntity<>("{\"response\": \"Unauthorized Canvas API token\"}", HttpStatus.UNAUTHORIZED);
            }
            CanvasApiClient cc = new CanvasApiClient(token);

            return new ResponseEntity<>(cc.getAssignmentsAndGroups(canvasCourseId).toString(), HttpStatus.OK);
        } catch (WebClientResponseException e) {
            return new ResponseEntity<>("{\"response\": \"Error when retrieving data from Canvas\"}", e.getStatusCode());
        } catch (Exception e) {
            return new ResponseEntity<>("{\"response\": \"Sorry, something went wrong\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/courses/{canvasCourseId}")
    public ResponseEntity importCourse(@PathVariable(value="canvasCourseId") int canvasCourseId, @RequestBody String jsonString, @AuthenticationPrincipal SupernaturalAuthUser principal) {
        try {
            String token = this.getUsersCanvasToken(principal.getUser());
            if (token == null) {
                return new ResponseEntity<>("{\"response\": \"Unauthorized Canvas API token\"}", HttpStatus.UNAUTHORIZED);
            }
            CanvasApiClient cc = new CanvasApiClient(token);
            JsonObject canvasCourse = cc.getFullCourseInformation(canvasCourseId).getAsJsonObject();

            JsonArray tests = JsonParser.parseString(jsonString).getAsJsonArray();

            Course course = new Course();
            course.setId(Long.valueOf(canvasCourseId));
            course.setName(canvasCourse.get("name").getAsString());
            course.setModuleCoordinator(principal.getUser());

            insertTests(canvasCourse, tests, course);

            this.courseRepository.save(course);
            return new ResponseEntity(HttpStatus.CREATED);
        } catch (WebClientResponseException e) {
            return new ResponseEntity<>("{\"response\": \"Error when retrieving data from Canvas\"}", e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("{\"response\": \"Sorry, something went wrong\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/courses/{canvasCourseId}/tests")
    public ResponseEntity importTests(@PathVariable(value="canvasCourseId") int canvasCourseId, @RequestBody String jsonString, @AuthenticationPrincipal SupernaturalAuthUser principal) {
        try {
            String token = this.getUsersCanvasToken(principal.getUser());
            if (token == null) {
                return new ResponseEntity<>("{\"response\": \"Unauthorized Canvas API token\"}", HttpStatus.UNAUTHORIZED);
            }
            CanvasApiClient cc = new CanvasApiClient(token);
            JsonObject canvasCourse = cc.getFullCourseInformation(canvasCourseId).getAsJsonObject();

            JsonArray tests = JsonParser.parseString(jsonString).getAsJsonArray();

            Course course = courseRepository.findById(Long.valueOf(canvasCourseId)).get();

            if(!TheSecurityEnforcer.isCoordinatorForCourse(principal.getId(),course.getCanvasId())){
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            insertTests(canvasCourse, tests, course);

            this.courseRepository.save(course);
            return new ResponseEntity(HttpStatus.CREATED);
        } catch (WebClientResponseException e) {
            return new ResponseEntity<>("{\"response\": \"Error when retrieving data from Canvas\"}", e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("{\"response\": \"Sorry, something went wrong\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @Transactional
    @PostMapping("/tests/{testId}/people")
    public ResponseEntity importPeople(@PathVariable(value="testId") Long testId, @RequestBody String jsonString, @AuthenticationPrincipal SupernaturalAuthUser principal) {
        try {
            String token = this.getUsersCanvasToken(principal.getUser());
            if (token == null) {
                return new ResponseEntity<>("{\"response\": \"Unauthorized Canvas API token\"}", HttpStatus.UNAUTHORIZED);
            }

            Test test = testRepository.findTestById(testId);
            Course course = test.getCourse();

            if(!TheSecurityEnforcer.isCoordinatorForCourse(principal.getId(),course.getCanvasId())){
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            CanvasApiClient cc = new CanvasApiClient(token);
            JsonObject canvasCourse = cc.getFullCourseInformation(course.getCanvasId().intValue()).getAsJsonObject();

            JsonObject jsonTest = JsonParser.parseString(jsonString).getAsJsonObject();

            if (!jsonTest.get("delete_current_enrollments").isJsonNull() && jsonTest.get("delete_current_enrollments").getAsBoolean()) {
                test.resetEnrollments();
                test = testRepository.save(test);
            }

            insertPeople(canvasCourse, test, jsonTest);

            testRepository.save(test);
            return new ResponseEntity(HttpStatus.CREATED);
        } catch (WebClientResponseException e) {
            return new ResponseEntity<>("{\"response\": \"Error when retrieving data from Canvas\"}", e.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("{\"response\": \"Sorry, something went wrong\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void insertTests(JsonObject canvasCourse, JsonArray tests, Course course) {
        for (JsonElement t : tests) {
            JsonObject jsonTest = t.getAsJsonObject();
            Test test = new Test();
            test.setName(jsonTest.get("name").getAsString());
            course.addTest(test);

            insertPeople(canvasCourse, test, jsonTest);
        }
    }

    private void insertPeople(JsonObject canvasCourse, Test test, JsonObject jsonTest) {
        for (String teacherKey : canvasCourse.get("teachers").getAsJsonObject().keySet()) {
            JsonObject jsonTeacher = canvasCourse.get("teachers").getAsJsonObject().get(teacherKey).getAsJsonObject();

            User u = userRepository.findUserById(Long.parseLong(jsonTeacher.get("sisId").getAsString().replaceAll("[^0-9]","")));
            if(u == null) {
                u = new User();
                u.setId(Long.parseLong(jsonTeacher.get("sisId").getAsString().replaceAll("[^0-9]","")));
                u.setName(jsonTeacher.get("name").getAsString());
                userRepository.save(u);
                test.addEnrollment(u, TEACHER);
            } else {
                Enrollment enrollment = u.getEnrollment(test);
                if(enrollment != null) {
                    enrollment.setRole(TEACHER);
                } else {
                    test.addEnrollment(u, TEACHER);
                }
            }
        }


        if (jsonTest.get("all_students").getAsBoolean()) {
            for (String studentKey : canvasCourse.get("students").getAsJsonObject().keySet()) {
                JsonObject jsonStudent = canvasCourse.get("students").getAsJsonObject().get(studentKey).getAsJsonObject();

                User u = userRepository.findUserById(Long.parseLong(jsonStudent.get("sisId").getAsString().replaceAll("[^0-9]","")));
                if(u == null) {
                    u = new User();
                    u.setId(Long.parseLong(jsonStudent.get("sisId").getAsString().replaceAll("[^0-9]","")));
                    u.setName(jsonStudent.get("name").getAsString());
                    userRepository.save(u);
                    test.addEnrollment(u, STUDENT);
                } else {
                    Enrollment enrollment = u.getEnrollment(test);
                    if(enrollment != null) {
                        enrollment.setRole(STUDENT);
                    } else {
                        test.addEnrollment(u, STUDENT);
                    }
                }
            }
        } else {
            for (JsonElement groupsetIdEl : jsonTest.get("groupsets").getAsJsonArray()) {
                String groupsetId = groupsetIdEl.getAsString();
                JsonArray groupsetUserids = canvasCourse.get("groups").getAsJsonObject().get(groupsetId).getAsJsonArray();
                for (JsonElement useridEl : groupsetUserids) {
                    String studentKey = useridEl.getAsString();
                    JsonObject jsonStudent = canvasCourse.get("students").getAsJsonObject().get(studentKey).getAsJsonObject();

                    User u = userRepository.findUserById(Long.parseLong(jsonStudent.get("sisId").getAsString().replaceAll("[^0-9]","")));
                    if(u == null) {
                        u = new User();
                        u.setId(Long.parseLong(jsonStudent.get("sisId").getAsString().replaceAll("[^0-9]","")));
                        u.setName(jsonStudent.get("name").getAsString());
                        userRepository.save(u);
                        test.addEnrollment(u, STUDENT);
                    } else {
                        Enrollment enrollment = u.getEnrollment(test);
                        if(enrollment != null) {
                            enrollment.setRole(STUDENT);
                        } else {
                            test.addEnrollment(u, STUDENT);
                        }
                    }
                }
            }
        }
    }

//    private String getUsersCanvasToken(User user) {
//        try {
//            SupernaturalJdbcOAuth2AuthorizedClientService jdbcOAuth2AuthorizedClientService = (SupernaturalJdbcOAuth2AuthorizedClientService) oAuth2AuthorizedClientService;
//            OAuth2AuthorizedClient authorizedClient = jdbcOAuth2AuthorizedClientService.loadAuthorizedClient("canvas", user.getId().toString());
//
//            OAuth2AuthorizedClient newAuthorizedClient = jdbcOAuth2AuthorizedClientService.renewAccessTokenIfExpired(authorizedClient);
//            return newAuthorizedClient.getAccessToken().getTokenValue();
//        } catch (Exception e) {
//            return null;
//        }
//    }

    private String getUsersCanvasToken(User user) {
        return user.getCanvasToken();
    }
}
