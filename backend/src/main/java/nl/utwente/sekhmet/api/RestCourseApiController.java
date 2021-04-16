package nl.utwente.sekhmet.api;

import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.TheSecurityEnforcer;
import nl.utwente.sekhmet.jpa.service.CourseService;
import nl.utwente.sekhmet.jpa.service.EnrollmentService;
import nl.utwente.sekhmet.jpa.service.UserService;
import nl.utwente.sekhmet.jpa.model.*;
import nl.utwente.sekhmet.jpa.repositories.CourseRepository;
import nl.utwente.sekhmet.jpa.repositories.EnrollmentRepository;
import nl.utwente.sekhmet.jpa.repositories.TestRepository;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin(origins = "/**")
@RestController
@RequestMapping(value = "/api")
public class RestCourseApiController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TestRepository testRepository;
    private final EnrollmentRepository enrollmentRepository;


    public RestCourseApiController(UserRepository userRepository, CourseRepository courseRepository, TestRepository testRepository, EnrollmentRepository enrollmentRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.testRepository = testRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    // GET MAPPINGS //

    /*
    For now the course retrieval process works bottom-up:
        1. Need user id to access the tests enrolled into and make a TestList
        2. Need to collect all the Courses
        3. Check 1 by 1 which test(from the TestList) belongs to which course
        4. Map each course to the tests and return the JSON form
     */
    //removed parameter uid, as it is now retreived from session
    @GetMapping(value = "/courses")
    public ResponseEntity<String> getCoursesByUser(@AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        try {
            User user = UserService.getUser(uid, userRepository);
            if (user == null) {
                return new ResponseEntity<>(
                        "{\"response\": \"User="+String.valueOf(uid) +" could not be found\"}", HttpStatus.NOT_FOUND);
            }
//            Test r = testRepository.findTestWithEnrolled(1L);
//            if (enrollmentRepository.findEnrollmentByEnrollmentId_TestIdAndEnrollmentId_UserId(1L, user.getId()) == null) {
//                r.addEnrollment(user, user.isEmployee() ? 'T' : 'S');
//                testRepository.save(r);
//            }

            List<Enrollment> enrollments = EnrollmentService.getEnrollmentByUser(uid, enrollmentRepository);

            Set<Test> testSet = new HashSet<>();
            for (Enrollment f: enrollments) {
                if (f.getRole() == Enrollment.Role.TEACHER || (f.getTest().getEndTime() == null && f.getTest().getStartTime() != null)) {
                    //test is not active
                    testSet.add(f.getTest());
                }
            }

            //TODO streamline this, collects way too much info
            Map<Course, List<Test>> courseTestMap = new HashMap<Course, List<Test>>();

            List<Course> coursesThatYouAreModuleCoordinatorFor = courseRepository.findAllByModuleCoordinator_Id(uid);
            if(user.isSystemAdmin()){
                coursesThatYouAreModuleCoordinatorFor = (List<Course>) courseRepository.findAll();
            }

            for (Course c : coursesThatYouAreModuleCoordinatorFor) {
                courseTestMap.put(c, new ArrayList<>(c.getTests()));
            }

            for (Test t : testSet) {
                Course c = t.getCourse();
                if (! courseTestMap.containsKey(c)) {
                    List<Test> testTemp = new ArrayList<>();
                    testTemp.add(t);
                    courseTestMap.put(c, testTemp);
                } else {
                    courseTestMap.get(c).add(t);
                }
            }
            String json = "{ \"user_id\":" + uid + ", " +
                    " \"courses\": { ";

            if (courseTestMap.isEmpty()) {
                return ResponseEntity.ok("{\"response\": \"CourseTestMap is empty\"}");
            }

            json += courseTestMapToJSON(courseTestMap);
            json += "}}";

            String jsonAfter = StringChecker.escapeString(json);
            JSONObject jo = new JSONObject(jsonAfter);
            return ResponseEntity.ok(jsonAfter);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (JpaObjectRetrievalFailureException e) {
            //Not needed actually but if the database is changed directly (not through repository) this exception might get caught
            return new ResponseEntity<>(
                    "{\"response\": \"Database might be fukky, " + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NullPointerException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.NOT_FOUND);
        } catch (JSONException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"GET json format is incorrect: " + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/courses/{cid}")
    public ResponseEntity<String> getCourseInfo(@PathVariable(value = "cid") Long courseId,
                                                @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isCoordinatorForCourse(uid,courseId)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            Course c = courseRepository.findById(courseId).get();
            List<Test> tests = new ArrayList<>(c.getTests());
            Map<Course, List<Test>> m = new HashMap<>();
            m.put(c, tests);
            StringBuilder res = new StringBuilder("{").append(courseTestMapToJSON(m));
            Set<User> teachers = new HashSet<>();
            Set<User> students = new HashSet<>();

            for (Test t : tests) {
                List<Enrollment> temp = enrollmentRepository.findByEnrollmentIdTestId(t.getId());
                for (Enrollment e : temp) {
                    if (e.getRole() == 'S') {
                        students.add(e.getUser());
                    } else {
                        teachers.add(e.getUser());
                    }
                }
            }
            String teacherJson = User.userToJson(new ArrayList<>(teachers), "teachers");
            String studentJson = User.userToJson(new ArrayList<>(students), "students");
            res.delete(res.length()-1, res.length());
            //res.append(res.substring(0, res.length()-1));
            res .append( ", " + teacherJson);
            res .append(  ", " + studentJson);
            res .append( "}");  //compensate deleted accolade in res.delete
            res .append( "}" ); //end object

            String resAfter = StringChecker.escapeString(res.toString());
            JSONObject jo = new JSONObject(resAfter);
            return ResponseEntity.ok(resAfter);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.NOT_FOUND);
        } catch (JSONException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"GET json format is incorrect: " + e.getMessage() + "\"}" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT MAPPINGS //
    @PutMapping(value = "/courses/{id}")
    public ResponseEntity<String> updateCourse(@PathVariable(value = "id") Long id,  @RequestBody String json,
                                               @AuthenticationPrincipal SupernaturalAuthUser principal) {
        System.out.println(json);
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isCoordinatorForCourse(uid,id)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {

            String jsonAfter = StringChecker.escapeString(json);
            JSONObject jo = new JSONObject(jsonAfter);

            Long canvas_id = id;
            Course course = CourseService.getCourse(canvas_id, courseRepository);
            if (course == null) {
                return new ResponseEntity<>(
                        "{\"response\": \"course="+canvas_id.toString() +" could not be found\"}", HttpStatus.NOT_FOUND);
            }
            Long mc = null;
            if (jo.has("module_coordinator")){
                mc = jo.getLong("module_coordinator");
            }
            User moduleCoordinator;
            if (mc != null) {
                moduleCoordinator = userRepository.findUserById(mc);
                if (moduleCoordinator == null) {
                    return new ResponseEntity<>(
                            "{\"response\": \"module_coordinator="+mc.toString() +" could not be found\"}", HttpStatus.NOT_FOUND);
                } else if (!moduleCoordinator.isEmployee()) {
                    System.out.println("no coordinator");
                    return new ResponseEntity<>(
                            "{\"response\": \"module_coordinator="+mc.toString() +" is not an employee\"}", HttpStatus.NOT_ACCEPTABLE);
                } else {
                    course.setModuleCoordinator(moduleCoordinator);
                }
            }
            if (jo.has("name")) {
                course.setName(jo.getString("name"));
            } else if (mc == null) {
                return new ResponseEntity<>(
                        "{\"response\": \"JSON body is empty\"}", HttpStatus.NOT_ACCEPTABLE);
            }
            courseRepository.save(course);
            return ResponseEntity.ok("{\"response\": \"course updated\"}");
        } catch (JSONException e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                    "{\"response\": \"json format is incorrect: " + e.getMessage() + "\"}", HttpStatus.NOT_ACCEPTABLE);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \""+e.getMessage()+"\"}", HttpStatus.NOT_FOUND);
        }
    }


    // Additional functions //
    public static String courseTestMapToJSON(Map<Course, List<Test>> map){
        StringBuilder json = new StringBuilder("");
        Iterator <Course> it = map.keySet().iterator();
        boolean first_course = true;
        while(it.hasNext()) {
            Course course = it.next();
            if (!first_course) {
                json.append(", ");
            }
            first_course = false;

            json.append("\"" + course.getCanvasId() + "\":{ \"canvas_id\": " + course.getCanvasId() + "," +
                    "\"name\" : \"" + course.getName() + "\"," +
                    "\"created_at\":" + course.getCreatedAt() + "," +
                    "\"module_coordinator\" : " + User.userToJson(course.getModuleCoordinator(), false, false) + "," +
                    "\"tests\" : { ");

            if (!(map.get(course).isEmpty())){
                boolean first_test = true;
                for (Test test: map.get(course)){
                    if (!first_test) {
                        json.append(",");
                    }
                    first_test = false;
                    json.append("\"" + test.getId() + "\":" + "{\"test_id\" : " + test.getId() + "," +
                            "\"name\" : \"" + test.getName() + "\"," +
                            "\"start_time\" : " + test.getStartTime() + "," +
                            "\"end_time\" : " + test.getEndTime() + "}");

                }
            }
            json.append("}}");
        }
        return json.toString();
    }
}
