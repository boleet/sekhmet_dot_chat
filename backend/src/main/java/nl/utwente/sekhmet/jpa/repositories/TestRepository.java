package nl.utwente.sekhmet.jpa.repositories;

import nl.utwente.sekhmet.jpa.model.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TestRepository extends CrudRepository<Test, Long> {

    @Query("SELECT t FROM Test t LEFT JOIN FETCH t.conversations WHERE t.id= :tid")
    Test findTestByIdWithConversations(Long tid);

    @Query("SELECT DISTINCT t FROM Test t LEFT JOIN FETCH t.enrolled e LEFT JOIN FETCH e.user AS u WHERE t.id= :tid")
    Test findTestById(Long tid);

    @Query("SELECT t FROM Test t WHERE t.id= :tid")
    Test findOnlyTest(Long tid);

    @Query("SELECT DISTINCT t FROM Test t LEFT JOIN t.enrolled AS e ON t = e.test LEFT JOIN e.user WHERE t.id= :tid")
    Test findTestWithEnrolled(Long tid);

    List<Test> findTestsByNameAndAndCourse_CanvasId(String name, Long canvasId);


    //DEREFERENCE
    @Transactional
    @Modifying
    @Query("UPDATE Test t SET t.announcements=NULL WHERE t.id= :tid AND t.announcements NOT IN " +
            "(SELECT dd FROM Conversation dd JOIN dd.messages) ")
    void deleteReferenceAnnouncement(Long tid);

    //DEREFERENCE
    @Transactional
    @Modifying
    @Query("UPDATE Test t SET t.teacherConversation=NULL WHERE t.id= :tid AND t.teacherConversation NOT IN " +
            "(SELECT dd FROM Conversation dd JOIN dd.messages) ")
    void deleteReferenceTeacherChat(Long tid);

}
