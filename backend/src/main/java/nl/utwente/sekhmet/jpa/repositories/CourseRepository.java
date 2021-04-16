package nl.utwente.sekhmet.jpa.repositories;

import nl.utwente.sekhmet.jpa.model.Course;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends CrudRepository<Course, Long> {
//    Course findCourseById(Long canvas_id);
    List<Course> findAllByOrderByCreatedAtDesc();
    List<Course> findAllByModuleCoordinator_Id(Long id);
}
