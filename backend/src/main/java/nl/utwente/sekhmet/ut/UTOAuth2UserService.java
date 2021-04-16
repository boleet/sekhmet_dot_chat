package nl.utwente.sekhmet.ut;

import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.TestRepository;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Deprecated
@Service
public class UTOAuth2UserService extends DefaultOAuth2UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return oAuth2User;

//        UTOAuth2User UTUser = new UTOAuth2User(oAuth2User.getAuthorities(), oAuth2User.getAttributes(), "name");
//        UTUser.setUserRepository(this.userRepository);
//
//        ClientRegistration cr = userRequest.getClientRegistration();
//        if(cr.getClientName().equals("Canvas")) {
//            // TODO: change this to the real id of the logged in user.
//            UTUser.setId(mapCanvasIdToStudentNumber(Integer.valueOf(oAuth2User.getAttribute("id"))));
//        } else {
//            // TODO: check if this actually works (i.e. attribute key is correct)
////            UTUser.setId(Long.valueOf(oAuth2User.getAttribute("uid")));
//            System.out.println("heyeye");
//            System.out.println(oAuth2User.getAttributes().toString());
//        }
//
//        return UTUser;
    }

    // TODO: remove this method when switched to UT SSO
    // note that these canvas ids are only valid at the utwente-dev Canvas environment
    private Long mapCanvasIdToStudentNumber(int canvasId) {
        switch (canvasId) {
            default:
                return null;
        }
    }

}
