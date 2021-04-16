package nl.utwente.sekhmet.canvas;

import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.TestRepository;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class CanvasOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        CanvasOAuth2User cUser = new CanvasOAuth2User(oAuth2User.getAuthorities(), oAuth2User.getAttributes(), "login_id");

        try {
            Long userId = Long.parseLong(
                    cUser.getAttribute("login_id").toString().replaceAll("[^0-9]","")
            );
            cUser.setId(userId);
            cUser.setUserRepository(this.userRepository);
        } catch (Exception exception) {
            // nothing.
        }

        return cUser;
    }

}