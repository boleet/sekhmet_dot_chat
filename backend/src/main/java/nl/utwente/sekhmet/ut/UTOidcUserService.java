package nl.utwente.sekhmet.ut;

import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class UTOidcUserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to the default implementation for loading a user
        OidcUser oidcUser = super.loadUser(userRequest);
        UTOAuth2User UTuser = new UTOAuth2User(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo());

        try {
            Long userId = Long.parseLong(
                    oidcUser.getAttribute("employeeid").toString().replaceAll("[^0-9]","")
            );
            UTuser.setId(userId);
            UTuser.setUserRepository(this.userRepository);
        } catch (Exception exception) {
            // nothing.
        }

        return UTuser;
    }
}
