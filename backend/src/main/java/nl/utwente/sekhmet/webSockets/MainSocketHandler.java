package nl.utwente.sekhmet.webSockets;

import com.google.gson.*;
import nl.utwente.sekhmet.TheSecurityEnforcer;
import nl.utwente.sekhmet.jpa.model.Conversation;
import nl.utwente.sekhmet.jpa.model.Enrollment;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.ut.UTOAuth2User;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Main socket handler.
 * <p>
 * The handler for the main web socket. Parses incoming messages & decides what to do
 */
public class MainSocketHandler extends TextWebSocketHandler {

	private static Gson gson = new GsonBuilder().create();

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) {
		System.out.println(message.getPayload());
		long myId = ((UTOAuth2User) ((SecurityContext) session.getAttributes().get("SPRING_SECURITY_CONTEXT")).getAuthentication().getPrincipal()).getUser().getId();
		WebSocketMessage webSocketMessage = gson.fromJson(message.getPayload(), WebSocketMessage.class);
		switch (webSocketMessage.getMessageType()) {
			case "conversation_unread":
				try {
					forwardMessage(webSocketMessage,myId, session);
					WebSocketMessage.UpdateReadMessage updateReadMessage = gson.fromJson(webSocketMessage.getMessage(), WebSocketMessage.UpdateReadMessage.class);
					if (!TheSecurityEnforcer.isUserForConversation(myId,updateReadMessage.getChat_id())){return;}
					if (TheSecurityEnforcer.isTeacherForTest(myId, updateReadMessage.getTest_id())) {
						TheWebSocketDatabaseAccessHolder.setUnreadCounter(Enrollment.Role.TEACHER, updateReadMessage);
					}else {
						TheWebSocketDatabaseAccessHolder.setUnreadCounter(Enrollment.Role.STUDENT, updateReadMessage); // grrr database go update already
					}
				} catch (Exception e) {
					e.printStackTrace();
					nonAcknowledgementReturn(message.getPayload(), session, e.getMessage());
				}
				break;
			case "message_delete":
				try {
					forwardMessage(webSocketMessage,myId, session);
					WebSocketMessage.DeleteMessage deleteMessage = gson.fromJson(webSocketMessage.getMessage(), WebSocketMessage.DeleteMessage.class);
//				if (!SecurityEnforcer.isUserForConversation(myId,deleteMessage.getChat_id())){return;} // implied by isTeacherForTest
					if (TheSecurityEnforcer.isTeacherForTest(myId, deleteMessage.getTest_id())) {
						TheWebSocketDatabaseAccessHolder.deleteMessage(deleteMessage);
					}
				} catch (Exception e) {
					e.printStackTrace();
					nonAcknowledgementReturn(message.getPayload(), session, e.getMessage());
				}
				break;
			case "message_final":
				try {
					WebSocketMessage.ChatMessage chatMessage = gson.fromJson(webSocketMessage.getMessage(), WebSocketMessage.ChatMessage.class);
					if (!TheSecurityEnforcer.isUserForConversation(myId,chatMessage.getChat_id())){return;}
					Conversation c = TheWebSocketDatabaseAccessHolder.saveMessage(myId, chatMessage);
					int sent = forwardMessage(webSocketMessage,myId, session, c.getUser1());
					if (sent != 1) {
						//conversation is a student chat by definition
						String error = "Your message was not received by ";
						if (sent == 2) {
							//intended receiver is student
							error += c.getUser1().getName();
						} else if (sent == 3) {
							//intended receiver is any teacher
							error += "any teacher";
						}
						nonAcknowledgementReturn(message.getPayload(), session, error);
					}
				} catch (Exception e) {
					e.printStackTrace();
					nonAcknowledgementReturn(message.getPayload(), session, e.getMessage());
				}
				break;
			default:
				forwardMessage(webSocketMessage,myId, session);
		}
	}

	private int forwardMessage(WebSocketMessage webSocketMessage, long myId, WebSocketSession session, User goal) {
		List<String> actualReceived = forwardMessage(webSocketMessage, myId, session);
		if (goal != null) {
			//student chat
			JsonArray intendedReceivers = webSocketMessage.getReceiverIds();
			for (int i = 0; i < intendedReceivers.size(); i ++) {
				if (intendedReceivers.get(i).getAsString().equals(goal.getId().toString())) {
					//student was intended receiver
					if (actualReceived.contains(goal.getId().toString())) {
						//intended receiver is student.
						//actual received contains that student, so success
						return 1;
					} else {
						//intended receiver is student
						//but student did not get it
						return 2;
					}
				}
			}
			//you are a student, so at least one teacher should receive it
			return actualReceived.size() > 0 ? 1 : 3;
		} else {
			//teacher or announcement chat, dont care
			return 1;
		}
	}

	private List<String> forwardMessage(WebSocketMessage webSocketMessage, long myId, WebSocketSession session) {
		List<String> receivers = new ArrayList<>();
		for (JsonElement id : webSocketMessage.getReceiverIds()) {
			WebSocketSession receiver = TheSecuredSessionMap.get(id.getAsString(), myId);
			if (receiver != null) {
				try {
					WebSocketMessage sendMessage = webSocketMessage.deepCopy();
					JsonArray newReceiverList = new JsonArray();
					newReceiverList.add(id);
					sendMessage.setReceiverIds(newReceiverList);
					receiver.sendMessage(new TextMessage(gson.toJson(sendMessage)));
					receivers.add(id.getAsString());
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return receivers;
	}

	public static void nonAcknowledgementReturn(String message, WebSocketSession sender, String errorMessage) {
		try {
			JSONObject json = new JSONObject();
			json.put("messageType", "nack");
			JsonArray receivers = new JsonArray();
			json.put("receiverIds", receivers);
			json.put( "message", new JSONObject(message));
			json.put("error", errorMessage);
			sender.sendMessage(new TextMessage(json.toString()));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		long myId = ((UTOAuth2User) ((SecurityContext) session.getAttributes().get("SPRING_SECURITY_CONTEXT")).getAuthentication().getPrincipal()).getUser().getId();
		TheSecuredSessionMap.put(myId, new ConcurrentWebSocketSessionDecorator(session, 30000, 128000)); // 30 seconds & 128kb send timeout and buffer limits
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		long myId = ((UTOAuth2User) ((SecurityContext) session.getAttributes().get("SPRING_SECURITY_CONTEXT")).getAuthentication().getPrincipal()).getUser().getId();
		TheSecuredSessionMap.remove(myId);
	}

}