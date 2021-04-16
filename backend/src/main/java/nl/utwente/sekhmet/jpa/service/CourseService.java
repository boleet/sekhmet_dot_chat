package nl.utwente.sekhmet.jpa.service;

import com.google.gson.*;
import nl.utwente.sekhmet.jpa.model.Course;
import nl.utwente.sekhmet.jpa.model.Enrollment;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.*;
import org.json.JSONException;

import java.util.*;

public class CourseService {
    //POST-create
    public static final String testString = "{}";

    public static void postCanvasData(JsonObject data, UserRepository userRepository, EnrollmentRepository enrollmentRepository,
                                      CourseRepository courseRepository, TestRepository testRepository)
            throws JSONException {
        Gson g = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        JsonArray jsonTests = data.getAsJsonArray("tests");
        Test[] tests = g.fromJson(jsonTests, Test[].class);
        Course course = g.fromJson(data, Course.class);
        //course.setTestsList(new HashSet<>(Arrays.asList(tests)));
        testRepository.saveAll(Arrays.asList(tests.clone()));
        courseRepository.save(course);

        int len = jsonTests.size();
        for (int i = 0; i < len; i ++) {
            JsonElement test = jsonTests.get(i);
            User[] teachers = g.fromJson(test.getAsJsonObject().getAsJsonArray("teachers"), User[].class);
            User[] students = g.fromJson(test.getAsJsonObject().getAsJsonArray("students"), User[].class);
            userRepository.saveAll(Arrays.asList(students));
            userRepository.saveAll(Arrays.asList(teachers));
            List<Enrollment> enrollmentList = new ArrayList<>();
            Test t = tests[i];
            for (User u : teachers) {
                Enrollment e = new Enrollment(u, t, 'T');
                u.getEnrollments().add(e);
                enrollmentList.add(e);
            }
            for (User u : students) {
                Enrollment e = new Enrollment(u, t, 'S');
                u.getEnrollments().add(e);
                enrollmentList.add(e);
            }
            t.setEnrolled(new HashSet<>(enrollmentList));
            enrollmentRepository.saveAll(enrollmentList);
            testRepository.save(t);
            userRepository.saveAll(Arrays.asList(students));
            userRepository.saveAll(Arrays.asList(teachers));
        }
    }

    public static void postCanvasData(String data, UserRepository userRepository, EnrollmentRepository enrollmentRepository,
                              CourseRepository courseRepository, TestRepository testRepository)
                                throws JSONException {
        //Gson g = new Gson();
        Gson g = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        JsonObject jo = g.fromJson(CourseService.testString, JsonObject.class);
        postCanvasData(jo, userRepository, enrollmentRepository, courseRepository, testRepository);
    }

    //GET-retrieve
    public static Course getCourse(Long id, CourseRepository courseRepository) {
        try {
            Course course = courseRepository.findById(id).get();
            return course;
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Course_id: " + id + " ||| The course does not exists!");
        }

    }

    //PUT-update

    //DELETE
}
