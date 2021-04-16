package nl.utwente.sekhmet.jpa.repositories;

import nl.utwente.sekhmet.jpa.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Query("FROM Enrollment e JOIN FETCH e.test WHERE e.user.id= :userId")
    List<Enrollment> findByEnrollmentIdUserId(Long userId);

    @Query("FROM Enrollment e JOIN FETCH e.test JOIN FETCH e.user WHERE e.test.id= :testId")
    List<Enrollment> findByEnrollmentIdTestId(Long testId);

    @Query("FROM Enrollment e JOIN FETCH e.test JOIN FETCH e.user WHERE e.test.id= :testId AND e.user.id= :userId")
    Enrollment findEnrollmentByEnrollmentId_TestIdAndEnrollmentId_UserId(Long testId, Long userId);

    List<Enrollment> findEnrollmentsByEnrollmentId_TestIdAndRole(Long testId, char role);
}
