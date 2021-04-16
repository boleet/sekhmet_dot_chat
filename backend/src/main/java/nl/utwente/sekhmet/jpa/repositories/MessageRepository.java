package nl.utwente.sekhmet.jpa.repositories;

import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Message;
import nl.utwente.sekhmet.jpa.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findMessageByMessageId_Mid(Long mid);

    List<Message> findMessageByMessageId_MidAndMessageId_Timestamp(Long mid, Long timestamp);

    List<Message> findMessageByMessageId_ConversationId(Long cid);

    List<Message> findMessagesByConversation_Test_Id (Long testId);

    List<Message> findMessagesByMessageId_ConversationIdOrderByMessageId_TimestampDesc(Long cid);

    List<Message> findMessagesByConversationTest(Test test);

    Message findMessageByMessageId_MidAndMessageId_TimestampAndMessageId_SenderIdAndMessageId_ConversationId(Long mid, Long timestamp, Long uid, Long cid);
}
