package nl.utwente.sekhmet.jpa.service;

import nl.utwente.sekhmet.jpa.model.Enrollment;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.EnrollmentRepository;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentService {
    //POST-create
    public static Enrollment postEnrollment(User user, Test test, char role, EnrollmentRepository enrollmentRepository) {
        Enrollment enrollment = enrollmentRepository.findEnrollmentByEnrollmentId_TestIdAndEnrollmentId_UserId(test.getId(), user.getId());
        if (enrollment != null) {
            throw new IllegalStateException("Test_id: " + test.getId() + "; User_id: " + user.getId() + " ||| This user is already enrolled in this test!");
//            if (enrollment.getRole() != role) {
//                updateEnrollment(user, test, role, enrollmentRepository);
//            }
        }
        Enrollment newEnrollment = new Enrollment(user, test, role);
        enrollmentRepository.save(newEnrollment);
        return newEnrollment;
    }

    public static List<Enrollment> postEnrollment(List<User> users, Test test, char role, EnrollmentRepository enrollmentRepository) {
        List<Enrollment> enrollmentList = new ArrayList<>();
        for (User user: users) {
            if (EnrollmentService.getEnrollment(test.getId(), user.getId(), enrollmentRepository) == null) { //bypass the exception since it fails other postEnrollment if 1 of the enrollment already exists
                enrollmentList.add(postEnrollment(user, test, role, enrollmentRepository));
            }
        }
        return enrollmentList;
    }

    //GET-retrieve

    public static List<Enrollment> getEnrollmentByTest(Long testId, EnrollmentRepository enrollmentRepository) {
        List<Enrollment> enrollments = enrollmentRepository.findByEnrollmentIdTestId(testId);
        return enrollments;
    }
    public static List<Enrollment> getEnrollmentByUser(Long userId, EnrollmentRepository enrollmentRepository) {
        List<Enrollment> enrollments = enrollmentRepository.findByEnrollmentIdUserId(userId);
        return enrollments;
    }
    public static Enrollment getEnrollment(Long testId, Long userId, EnrollmentRepository enrollmentRepository) {
        Enrollment enrollment = enrollmentRepository.findEnrollmentByEnrollmentId_TestIdAndEnrollmentId_UserId(testId, userId);
        return enrollment;
    }

    /*
    mainly for changing roles
     */

    //PUT-update
    public static void updateEnrollment(User user, Test test, char role, EnrollmentRepository enrollmentRepository){
        Enrollment enrollment = EnrollmentService.getEnrollment(test.getId(), user.getId(), enrollmentRepository);
        enrollment.setRole(role);
        enrollmentRepository.save(enrollment);
    }

    //DELETE
    public static void deleteEnrollment(Enrollment enrollment, EnrollmentRepository enrollmentRepository){
        enrollmentRepository.delete(enrollment);
    }
}
