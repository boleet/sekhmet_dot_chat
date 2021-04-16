package nl.utwente.sekhmet.canvas;

import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

public class CanvasOAuth2User extends DefaultOAuth2User implements SupernaturalAuthUser {

    private UserRepository userRepository;
    private Long userId;

    public CanvasOAuth2User(Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes, String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
    }

    public authProvider getAuthProvider() {
        return SupernaturalAuthUser.authProvider.CANVAS;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser() {
        return this.userRepository.findById(this.getId()).get();
    }

    public void setId(Long id) {
        this.userId = id;
    }

    public Long getId() {
        return this.userId;
    }

    @Override
    public String getName() {
        return this.getId().toString();
    }
}
