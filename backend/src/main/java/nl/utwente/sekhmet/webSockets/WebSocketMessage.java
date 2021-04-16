package nl.utwente.sekhmet.webSockets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Message;
import nl.utwente.sekhmet.jpa.model.MessageId;
import nl.utwente.sekhmet.jpa.model.User;

/**
 * The type Web socket message.
 * <p>
 * The java interpretation of a raw message as received over a web socket
 */
public class WebSocketMessage {
	@Expose
	private String messageType;
	@Expose
	private JsonArray receiverIds;
	@Expose
	private JsonElement message;

	private WebSocketMessage(String messageType, JsonArray receiverIds, JsonElement message) {
		this.messageType = messageType;
		this.receiverIds = receiverIds;
		this.message = message;
	}

	/**
	 * Gets message type.
	 *
	 * @return the message type
	 */
	public String getMessageType() {
		return messageType;
	}

	/**
	 * Gets receiver ids.
	 *
	 * @return the receiver ids
	 */
	public JsonArray getReceiverIds() {
		return receiverIds;
	}

	/**
	 * Gets message.
	 * <p>
	 * The actual object type the message corresponds to can be inferred from getMessageType()
	 *
	 * @return the message
	 */
	public JsonElement getMessage() {
		return message;
	}

	/**
	 * Set receiver ids.
	 *
	 * @param receiverIds the receiver ids
	 */
	public void setReceiverIds(JsonArray receiverIds){
		this.receiverIds = receiverIds;
	}

	/**
	 * Deep copy web socket message.
	 *
	 * @return the web socket message
	 */
	public WebSocketMessage deepCopy(){
		return new WebSocketMessage(messageType,receiverIds,message);
	}

	// type possibilites for message that are intercepted by the mainSocket


	/**
	 * The message type Chat message.
	 * <p>
	 * corresponds with messageType value "message_final"
	 * (and possibly others, but those are not intercepted)
	 */
	public static class ChatMessage {
		private Long message_id;
		private Long test_id;
		private Long chat_id;
		private Long timestamp;
		private Long message_index; // for update-counting in front-end
		private boolean is_final; // presumed true
		private String content;

		private ChatMessage(Long message_id, Long test_id, Long chat_id, Long timestamp, Long message_index, boolean is_final, String content) {
			this.message_id = message_id;
			this.test_id = test_id;
			this.chat_id = chat_id;
			this.timestamp = timestamp;
			this.message_index = message_index;
			this.is_final = is_final;
			this.content = content;
		}

		/**
		 * Gets test id.
		 *
		 * @return the test id
		 */
		public Long getTest_id() {
			return test_id;
		}

		/**
		 * Gets chat id.
		 *
		 * @return the chat id
		 */
		public Long getChat_id() {
			return chat_id;
		}

		/**
		 * To database model message.
		 *
		 * @param sender       the database user object for the sender
		 * @param conversation the database object for the conversation
		 * @return the message
		 */
		public Message toMessage(User sender, Conversation conversation) {
			Message ret = new Message();
			ret.setMessageId(new MessageId());
			ret.setMid(message_id);
			ret.setVisible(true);
			ret.setConversation(conversation);
			ret.setSender(sender);
			ret.setContent(content);
			ret.setTimestamp(timestamp);
			return ret;
		}
	}

	/**
	 * The message type Delete message.
	 * <p>
	 * corresponds with messageType value "message_delete"
	 * (and possibly others, but those are not intercepted)
	 */
	public static class DeleteMessage {
		private Long message_id;
		private Long test_id;
		private Long chat_id;
		private Long timestamp;
		private Long sender_id;

		private DeleteMessage(Long message_id, Long test_id, Long chat_id, Long timestamp, Long sender_id) {
			this.message_id = message_id;
			this.test_id = test_id;
			this.chat_id = chat_id;
			this.timestamp = timestamp;
			this.sender_id = sender_id;
		}

		/**
		 * Gets message id.
		 *
		 * @return the message id
		 */
		public Long getMessage_id() {
			return message_id;
		}

		/**
		 * Gets test id.
		 *
		 * @return the test id
		 */
		public Long getTest_id() {
			return test_id;
		}

		/**
		 * Gets chat id.
		 *
		 * @return the chat id
		 */
		public Long getChat_id() {
			return chat_id;
		}

		public Long getTimestamp() {
			return timestamp;
		}

		public Long getSender_id() {
			return sender_id;
		}
	}

	/**
	 * The  message type Update read message.
	 * <p>
	 * corresponds with messageType value "conversation_unread"
	 * (and possibly others, but those are not intercepted)
	 */
	public static class UpdateReadMessage {
		private Long test_id;
		private Long chat_id;
		private Long unread;

		private UpdateReadMessage(Long test_id, Long chat_id, Long unread) {
			this.test_id = test_id;
			this.chat_id = chat_id;
			this.unread = unread;
		}

		/**
		 * Gets test id.
		 *
		 * @return the test id
		 */
		public Long getTest_id() {
			return test_id;
		}

		/**
		 * Gets chat id.
		 *
		 * @return the chat id
		 */
		public Long getChat_id() {
			return chat_id;
		}

		/**
		 * Gets unread count.
		 *
		 * @return the unread count
		 */
		public Long getUnreadCount() {
			return unread;
		}
	}
}
