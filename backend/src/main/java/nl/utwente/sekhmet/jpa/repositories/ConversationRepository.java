package nl.utwente.sekhmet.jpa.repositories;

import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.model.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ConversationRepository extends CrudRepository<Conversation, Long> {
    Conversation findConversationById(Long id);

    @Query("SELECT DISTINCT c FROM Conversation c LEFT JOIN FETCH c.messages m LEFT JOIN FETCH m.sender LEFT JOIN FETCH c.user1  WHERE c.test.id= :tid")
    List<Conversation> findConversationByTest_Id(Long tid);

    List<Conversation> findConversationByTest(Test test);

    Conversation findConversationByTest_IdAndAndUser1(Long testId, User user);

    Conversation findConversationByTest_IdAndUser1AndUser2(Long testId, User user, User user1);

    @Transactional
    @Modifying
    @Query("DELETE FROM Conversation c WHERE c.test.id= :tid AND c NOT IN (SELECT cc FROM Conversation cc JOIN cc.messages)")
    void deleteEmptyByTest_Id(Long tid);
}
