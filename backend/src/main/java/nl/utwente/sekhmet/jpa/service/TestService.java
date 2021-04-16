package nl.utwente.sekhmet.jpa.service;


import nl.utwente.sekhmet.jpa.model.*;
import nl.utwente.sekhmet.jpa.repositories.CourseRepository;
import nl.utwente.sekhmet.jpa.repositories.TestRepository;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.NoSuchElementException;
import java.util.Set;


public class TestService {
    //SUCCESS = opened or closed successfully
    //CLOSED = trying to open or close a closed test without correct parameter reopen
    //OPENED = trying to open an already opened test
    //NULL = test not found
    public enum TestState {SUCCESS, CLOSED, OPENED, NULL, TIMEOUT}

    //POST-create
    public static Test postTest(JSONObject jo, CourseRepository courseRepository)
            throws JSONException, NoSuchElementException, NullPointerException {

        Test t = new Test(null, null, jo.getString("name"), null, null);
        Course c = courseRepository.findById(jo.getLong("course_id")).get();
        c.addTest(t);
        courseRepository.save(c);
        return t;
    }

    public static void postTest(Test test, TestRepository testRepository) {
        testRepository.save(test);
    }

    //GET-retrieve

    public static Test getTest(Long id, TestRepository testRepository) {
        Test test = testRepository.findTestById(id);
        if (test == null) {
            throw new NoSuchElementException("Test_id: " + id + " ||| The test does not exists!");
        }
        return test;
    }

    //PUT-update
    /*
    Change Enrollment info of a test
    tid = test id
    enrollmentSet = the new set of enrollments (to change individual role use the postEnrollment and call the new set with getEnrollment(test_id))
     */
    public static Test updateTest(Long tid, Set<Enrollment> enrollmentSet, TestRepository testRepository){
        Test testToChange = getTest(tid, testRepository);
        testToChange.setEnrolled(enrollmentSet);
        return testToChange;
    }

    //overload
    public static TestState startTest(Long id, TestRepository testRepository, boolean reopen, boolean force) throws NoSuchElementException {
        Test t = testRepository.findOnlyTest(id);
        if (t == null) {
            //test does not exist
            return TestState.NULL;
        } else if (t.getEndTime() != null) {
            //test is already closed
            if (reopen && (System.currentTimeMillis()-t.getEndTime() < 3600000)) {
                //parameter reopen has been set, dont change starttime, remove endtime
                t.setEndTime(null);
                testRepository.save(t);
                return TestState.SUCCESS;
            } else if ( reopen && force){
                t.setEndTime(null);
                testRepository.save(t);
                return TestState.SUCCESS;
            } else if (reopen) {
                //parameter reopen has not been set
                return TestState.TIMEOUT;
            } else {
                return TestState.CLOSED;
            }
        } else if (t.getStartTime() != null) {
            //test is already open
            return TestState.OPENED;
        } else {
            //test is not opened, so open it now
            t.setStartTime(System.currentTimeMillis());
            testRepository.save(t);
            return TestState.SUCCESS;
        }
    }

    //overload
    public static TestState startTest(Long id, TestRepository testRepository) throws NoSuchElementException {
        return startTest(id, testRepository, false);
    }

    /*
     * Open a test. if a test is already open, but not closed, return OPENED
     *              if a test is already opened and closed, and reopen=false, return CLOSED
     *              if a test is not opened, or opened and closed with reopen=true, return SUCCESS (and open the test)
     */
    public static TestState startTest(Long id, TestRepository testRepository, boolean reopen) throws NoSuchElementException {
        return startTest(id, testRepository, reopen, false);
    }

    public static TestState endTest(Long id, TestRepository testRepository) throws NoSuchElementException {
        Test t = testRepository.findTestById(id);
        if (t == null) {
            //test does not exist
            return TestState.NULL;
        } else if (t.getEndTime() != null) {
            //test is already closed
            return TestState.CLOSED;
        } else if (t.getStartTime() == null) {
            //test is not opened
            return TestState.CLOSED;
        } else {
            //test is opened and not closed, so close now
            t.setEndTime(System.currentTimeMillis());
            testRepository.save(t);
            return TestState.SUCCESS;
        }
    }

    /*
    Change basic info (name) of a test
     */
    public static Test updateTest(Long id, JSONObject jo, TestRepository testRepository)
            throws JSONException, NumberFormatException, NoSuchElementException{

        Test test = TestService.getTest(id, testRepository);
        String name = jo.getString("name");
        test.setName(name);
        testRepository.save(test);
        return test;
    }

    //DELETE

    public static void deleteTest(Long id, TestRepository testRepository){
        testRepository.deleteById(id);
    }
}
