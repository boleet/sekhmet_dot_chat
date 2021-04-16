package nl.utwente.sekhmet.ut;

import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

public class UTOAuth2User extends DefaultOidcUser implements SupernaturalAuthUser {

    private UserRepository userRepository;
    private Long id;

    public UTOAuth2User(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken, OidcUserInfo userInfo) {
        super(authorities, idToken, userInfo);
    }

    public authProvider getAuthProvider() {
        return SupernaturalAuthUser.authProvider.UTWENTE;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser() {
        return this.userRepository.findById(this.getId()).get();
    }

    @Override
    public String getName() {
        return this.getId().toString();
    }
}
