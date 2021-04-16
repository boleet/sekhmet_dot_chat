package nl.utwente.sekhmet.api;

import nl.utwente.sekhmet.TheSecurityEnforcer;
import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.jpa.service.ConversationService;
import nl.utwente.sekhmet.jpa.service.TestService;
import nl.utwente.sekhmet.jpa.model.*;
import nl.utwente.sekhmet.jpa.repositories.*;
import nl.utwente.sekhmet.webSockets.TheSecuredSessionMap;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/tests")
public class RestTestApiController {
    //Instantiated using the magic of Spring in the constructor
    private final TestRepository testRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public RestTestApiController(TestRepository testRepository, CourseRepository courseRepository, EnrollmentRepository enrollmentRepository, ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.testRepository = testRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /*
     * Opens a test with given testId
     * Returns SUCCESS if the test has not been opened OR if the test has been opened and closed and reopen=true
     */
    @PutMapping(value = "/{testId}/open")
    public ResponseEntity<String> openTest(@PathVariable(value="testId") Long testId,
                                           @RequestParam(required = false, value = "reopen" ) boolean reopen,
                                           @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isTeacherForTest(uid,testId)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        TestService.TestState t = TestService.startTest(testId, testRepository, reopen, TheSecurityEnforcer.isCoordinatorForTest(uid, testId));

        switch (t) {
            case CLOSED:
                return new ResponseEntity<>(
                        "{\"response\": \"Test " + testId + " is already opened and closed\"}", HttpStatus.NOT_ACCEPTABLE);
            case OPENED:
                return new ResponseEntity<>(
                        "{\"response\": \"Test " + testId + " is already opened\"}", HttpStatus.CONFLICT);
            case NULL:
                return new ResponseEntity<>(
                        "{\"response\": \"Test " + testId + " could not be found\"}", HttpStatus.NOT_FOUND);
            case TIMEOUT:
                return new ResponseEntity<>(
                        "{\"response\": \"Test " + testId + " is closed more than an hour ago\"}", HttpStatus.NOT_ACCEPTABLE);
            default:
                //Test test = TestController.getTest(testId, testRepository);
                Test test = testRepository.findTestByIdWithConversations(testId);
                Set<User> alreadyConv = test.getConversations().stream().map(s -> s.getUser1()).collect(Collectors.toSet());

                List<Enrollment> enrollmentList = new ArrayList<>(testRepository.findTestById(testId).getEnrolled());
                Set<User> allStudents = enrollmentList.stream().filter(s -> s.getRole() == 'S').map(s -> s.getUser()).collect(Collectors.toSet());
                if (test.getAnnouncements() == null) {
                    ConversationService.postConversationTestStart(null, 1, test, conversationRepository);
                }
                if (test.getTeacherConversation() == null) {
                    ConversationService.postConversationTestStart(null, 2, test,  conversationRepository );
                }
                Set<Conversation> conversationSet = new HashSet<>();

                allStudents.removeAll(alreadyConv);
                for (User s : allStudents) {
                    conversationSet.add(new Conversation(s, null, test));
                }
                conversationRepository.saveAll(conversationSet);
                testRepository.save(test);
                return ResponseEntity.ok("{\"response\": \"Test started\"}");
        }
    }

    /*
     * Closes a test with a given testId
     * returns SUCCESS if test was opened and not yet closed
     */
    @PutMapping("/{testId}/close")
    public ResponseEntity<String> closeTest(@PathVariable(value="testId") Long testId,
                                            @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isTeacherForTest(uid,testId)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        TestService.TestState t = TestService.endTest(testId, testRepository);

        switch (t) {
            case CLOSED:
                return new ResponseEntity<>(
                        "{\"response\": \"Test " + testId + " is not opened\"}", HttpStatus.CONFLICT);
            case NULL:
                return new ResponseEntity<>(
                        "{\"response\": \"Test " + testId + " could not be found\"}", HttpStatus.NOT_FOUND);
            default:
                TheSecuredSessionMap.clearCache(); // kind of spaghetti bullshit I know, but that's optimizations for ya
                return ResponseEntity.ok("{\"response\": \"Test closed\"}");
        }
    }

    /*
     * Change the name of a test
     */
    @PutMapping(value = "/{test_id}")
    public ResponseEntity<String> updateTest(@PathVariable("test_id") Long test_id,
                                             @RequestBody String json,
                                             @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isCoordinatorForTest(uid,test_id)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        String jsonAfter = StringChecker.escapeString(json);
        try {
            JSONObject jo = new JSONObject(jsonAfter);
            TestService.updateTest(test_id, jo, testRepository);
            return ResponseEntity.ok("{\"response\": \"Test Updated\"}");

        } catch (JSONException e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                    "{\"response\": \"Json format is incorrect: " + StringChecker.escapeString(e.getMessage(),true) + "\"}", HttpStatus.NOT_ACCEPTABLE);
        }
    }

    /*
     * create test by adding it to the correct course and saving that course
     */
    @PostMapping(value = "/test")
    public ResponseEntity<String> postTest(@RequestBody String json,
                                           @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        try {
            String jsonAfter = StringChecker.escapeString(json);
            JSONObject jo = new JSONObject(jsonAfter);
            if(!TheSecurityEnforcer.isCoordinatorForCourse(uid,jo.getLong("course_id"))){
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            TestService.postTest(jo, courseRepository);
            return ResponseEntity.ok("{\"response\": \"Successful\"}");
        } catch (JSONException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"Json format is incorrect: " + StringChecker.escapeString(e.getMessage(),true) + "\"}", HttpStatus.NOT_ACCEPTABLE);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.NOT_FOUND);
        } catch (NullPointerException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/{tid}")
    @Transactional
    public ResponseEntity<String> deleteTest(@PathVariable("tid") Long tid,
                                             @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isCoordinatorForTest(uid,tid)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            Test test = TestService.getTest(tid, testRepository);
            Course course = test.getCourse();
            course.deleteTest(test);
            courseRepository.save(course);
            return ResponseEntity.ok("{\"response\": \"Test Deleted\"}");
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
