package nl.utwente.sekhmet.api;

import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.TheSecurityEnforcer;
import nl.utwente.sekhmet.jpa.service.*;
import nl.utwente.sekhmet.jpa.model.Enrollment;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@RestController
@RequestMapping(value = "/api")
public class RestEnrollmentApiController {
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ConversationRepository conversationRepository;

    public RestEnrollmentApiController(TestRepository testRepository, UserRepository userRepository, CourseRepository courseRepository, EnrollmentRepository enrollmentRepository, ConversationRepository conversationRepository) {
        this.testRepository = testRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.conversationRepository = conversationRepository;
    }


    //TODO consider combining put(if needed) and post enrollment? since the field needed in the json body is only "role"
    @PostMapping(value = "/tests/{tid}/users/{uid}")
    public ResponseEntity<String> postEnrollment(@PathVariable("tid") Long tid,
                                                 @PathVariable("uid") Long uid,
                                                 @RequestBody String json,
                                                 @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long pid = principal.getUser().getId();
        if(!(TheSecurityEnforcer.isEmployee(pid) && TheSecurityEnforcer.isTeacherForTest(pid, tid))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            Test test = TestService.getTest(tid, testRepository);
            User user = UserService.getUser(uid, userRepository);

            JSONObject jo = new JSONObject(json);
            String rol = jo.getString("role");
            char role = 'S';
            if (rol.equals("T")) {
                role = 'T';
            }

            EnrollmentService.postEnrollment(user, test, role, enrollmentRepository);

            //if test is active during POST enrollment and is a student create a new chat for that student
            if (!(test.getStartTime() == null || test.getEndTime() != null)) {
                if (role == 'S') {
                    if (!ConversationService.checkExistsConversation(user, null, test, conversationRepository)){
                        ConversationService.postConversationTestStart(user, 0, test, conversationRepository);
                    }
                }
            }
            return ResponseEntity.ok("{\"response\": \"Enrollment added\"}");
        } catch (JSONException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"Json format is incorrect: " + StringChecker.escapeString(e.getMessage(),true) + "\"}", HttpStatus.NOT_ACCEPTABLE);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                    "{\"response\": \""+e.getMessage()+"\"}", HttpStatus.NOT_ACCEPTABLE);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \""+e.getMessage()+"\"}", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(value = "/tests/{tid}/users/{uid}")
    public ResponseEntity<String> deleteEnrollment(@PathVariable("tid") Long tid,
                                                   @PathVariable("uid") Long uid,
                                                   @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long pid = principal.getUser().getId();
        if(!(TheSecurityEnforcer.isTeacherForTest(pid, tid) && TheSecurityEnforcer.isEmployee(pid))) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            Enrollment enroll = enrollmentRepository.findEnrollmentByEnrollmentId_TestIdAndEnrollmentId_UserId(tid, uid);
            if (enroll == null) {
                return new ResponseEntity<>("" +
                        "{\"response\": \"Enrollment is null\"}", HttpStatus.NOT_FOUND);
            }
            EnrollmentService.deleteEnrollment(enroll, enrollmentRepository);
            return ResponseEntity.ok("{\"response\": \"Enrollment Deleted\"}");
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"Test_id: " + tid + "; User_id: " + uid + " ||| " + e.getMessage() + "\"}", HttpStatus.NOT_FOUND);
        }
    }
}
