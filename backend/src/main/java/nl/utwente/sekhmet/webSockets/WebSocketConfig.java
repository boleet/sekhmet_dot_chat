package nl.utwente.sekhmet.webSockets;

import nl.utwente.sekhmet.jpa.repositories.ConversationRepository;
import nl.utwente.sekhmet.jpa.repositories.MessageRepository;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * The type Web socket config.
 * <p>
 * i.e. spoopy spring ga-bage
 * one of those weird fancy classes you don't call but will be run anyway through devine intervention
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer { // I HATE

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
		webSocketHandlerRegistry.addHandler(mainSocketHandler(), "/socket/mainSocket").setAllowedOrigins("*")
				.addInterceptors(new HttpSessionHandshakeInterceptor());
		}

	/**
	 * Main socket handler web socket handler.
	 *
	 * @return the web socket handler
	 */
	@Bean // I think I fail to convey how much I hate
	public WebSocketHandler mainSocketHandler() {
		return new MainSocketHandler();
	}

}
