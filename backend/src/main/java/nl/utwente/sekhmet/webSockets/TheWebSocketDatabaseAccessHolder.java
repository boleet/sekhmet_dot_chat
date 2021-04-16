package nl.utwente.sekhmet.webSockets;

import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Message;
import nl.utwente.sekhmet.jpa.repositories.ConversationRepository;
import nl.utwente.sekhmet.jpa.repositories.MessageRepository;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.stereotype.Component;

/**
 * The "singleton" The web socket database access holder.
 *
 * "singleton" since it's not a proper singleton, but a set of statefull static functions that update the database based on new incoming data from the web socket.
 */
@Component
public class TheWebSocketDatabaseAccessHolder {

	private static MessageRepository messageRepository;
	private static ConversationRepository conversationRepository;
	private static UserRepository userRepository;

	/**
	 * Instantiates a new The web socket database access holder.
	 * <p>
	 * Called exclusively by spoopy spring garbage
	 *
	 * @param messageRepository      the message repository
	 * @param conversationRepository the conversation repository
	 * @param userRepository         the user repository
	 */
	public TheWebSocketDatabaseAccessHolder(MessageRepository messageRepository, ConversationRepository conversationRepository, UserRepository userRepository) {
		this.messageRepository = messageRepository;
		this.conversationRepository = conversationRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Set unread counter.
	 *
	 * @param role    the role
	 * @param message the message
	 */
	public static void setUnreadCounter(char role, WebSocketMessage.UpdateReadMessage message){
		Conversation conversation = conversationRepository.findConversationById(message.getChat_id());
		conversation.setRead(role, message.getUnreadCount());
		conversationRepository.save(conversation);
	}

	/**
	 * "Delete" message.
	 *
	 * @param message the message
	 */
	public static void deleteMessage(WebSocketMessage.DeleteMessage message){
		Message dbMessage = messageRepository.findMessageByMessageId_MidAndMessageId_TimestampAndMessageId_SenderIdAndMessageId_ConversationId(
				message.getMessage_id(),
				message.getTimestamp(),
				message.getSender_id(),
				message.getChat_id());
		dbMessage.setVisible(false);
		messageRepository.save(dbMessage);
	}

	/**
	 * Save message.
	 *
	 * @param senderId the sender id
	 * @param message  the message
	 */
	public static Conversation saveMessage(long senderId, WebSocketMessage.ChatMessage message){
		Conversation c = conversationRepository.findConversationById(message.getChat_id());
		messageRepository.save(message.toMessage(
				userRepository.findUserById(senderId),
				c));
		return c;
	}
}
