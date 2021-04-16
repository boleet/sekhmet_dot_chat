package nl.utwente.sekhmet.api;

import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.TheSecurityEnforcer;
import nl.utwente.sekhmet.jpa.service.TestService;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.TestRepository;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api")
public class RestUserApiController {

    private final TestRepository testRepository;
    private final UserRepository userRepository;


    public RestUserApiController(TestRepository testRepository, UserRepository userRepository) {
        this.testRepository = testRepository;
        this.userRepository = userRepository;
    }

    @GetMapping(value = "users")
    public ResponseEntity<String> getUserByPartial(@RequestParam(value = "q") String query,
                                                   @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isEmployee(uid)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        String q = "%" + query + "%";
        try {
            Long i = Long.decode(query);
            List<User> t = userRepository.findTop30ByIdContaining(i);
            List<User> t2 = userRepository.findTop30ByEmailLikeOrNameLike(q, q);
            t.addAll(t2);
            t = t.stream().limit(30).collect(Collectors.toList());
            String res = "{" + User.userToJson(t, "users", true) + "}";
            String resAfter = StringChecker.escapeString(res);
            try {
                JSONObject jo = new JSONObject(resAfter);
            } catch (JSONException e) {
                return new ResponseEntity<>(
                        "{\"response\": \"GET json format is incorrect: " + StringChecker.escapeString(e.getMessage(),true) + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(resAfter, HttpStatus.OK);
        } catch (NumberFormatException e) {
            List<User> t = userRepository.findTop30ByEmailLikeOrNameLike(q, q);

            String res = "{"+User.userToJson(t, "users", true) + "}";
            String resAfter = StringChecker.escapeString(res);
            try {
                JSONObject jo = new JSONObject(resAfter);
            } catch (JSONException e1) {
                return new ResponseEntity<>(
                        "{\"response\": \"GET json format is incorrect: " + StringChecker.escapeString(e1.getMessage(),true) + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(resAfter, HttpStatus.OK);
        }
    }


    /*
     * Get all users for a test, includes test name and students and teachers set (of users)
     * takes variable test_id as parameter
     */
    @GetMapping(value = "tests/{id}/users")
    public ResponseEntity<String> getTestUsers(@PathVariable("id") Long test_id,
                                               @AuthenticationPrincipal SupernaturalAuthUser principal) {
        long uid = principal.getUser().getId();
        if(!TheSecurityEnforcer.isTeacherForTest(uid,test_id)){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        try {
            Test test = TestService.getTest(test_id, testRepository);
            String result = Test.testWithEnrollmentToJson(test);
            String resAfter = StringChecker.escapeString(result);
            JSONObject jo = new JSONObject(resAfter);
            return ResponseEntity.ok(resAfter);
        } catch (NoSuchElementException e) {
            //check if the test exist and return exception if it doesn't
            return new ResponseEntity<>(
                    "{\"response\": \"Test " + test_id + " could not be found\"}", HttpStatus.NOT_FOUND);
        } catch (JpaObjectRetrievalFailureException e) {
            //Not needed actually but if the database is changed directly (not through repository) this exception might get caught
            return new ResponseEntity<>(
                    "{\"response\": \"Database might be fukky " + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"" + e.getMessage() + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (JSONException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"GET json format is incorrect: " + StringChecker.escapeString(e.getMessage(),true) + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // for now with @RequestParam
    //
    @GetMapping(value = "users/me")
    public ResponseEntity<String> getMyUser(@AuthenticationPrincipal SupernaturalAuthUser principal) {
        try {
            User user = principal.getUser(); //UserController.getUser(id, userRepository);
            String result = User.userToJson(user,false,false);
            String resAfter = StringChecker.escapeString(result);
            JSONObject jo = new JSONObject(resAfter);
            jo.put("system_admin", user.isSystemAdmin());
            return ResponseEntity.ok(jo.toString());
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            return new ResponseEntity<>(
//                    "user " + id + " could not be found", HttpStatus.NOT_FOUND);
                    "{\"response\": \"User could not be found\"}", HttpStatus.NOT_FOUND);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                    "{\"response\": \""+e.getMessage()+"\"}", HttpStatus.NOT_FOUND);
        } catch (JSONException e) {
            return new ResponseEntity<>(
                    "{\"response\": \"GET json format is incorrect: " + StringChecker.escapeString(e.getMessage(),true) + "\"}", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping(value = "/users/me")
    public ResponseEntity<String> updateUser(@RequestBody String json,
                                             @AuthenticationPrincipal SupernaturalAuthUser principal) {
        User user = principal.getUser();
        if(!TheSecurityEnforcer.isEmployee(user.getId())){
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        try {
            JSONObject jo = new JSONObject(json);
            user.setCanvasToken(jo.getString("canvas_token"));
            userRepository.save(user);
            return ResponseEntity.ok("\"response\": \"User Updated\"}");

        } catch (JSONException e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                    "{\"response\": \"Json format is incorrect: " + StringChecker.escapeString(e.getMessage(),true) + "\"}", HttpStatus.NOT_ACCEPTABLE);
        }
    }
}
