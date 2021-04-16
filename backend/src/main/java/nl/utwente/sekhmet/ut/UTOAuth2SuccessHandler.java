package nl.utwente.sekhmet.ut;

import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UTOAuth2SuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if(authentication.getPrincipal().getClass() == UTOAuth2User.class) {
            UTOAuth2User OAuth2user = (UTOAuth2User) authentication.getPrincipal();

            User user = null;
            try {
                user = OAuth2user.getUserRepository().findById(OAuth2user.getId()).get();
            } catch (Exception e) {
                // triggered when user not found / not yet in the db
                user = new User();
                user.setSystemAdmin(false);
                user.setEmployee(OAuth2user.getAttributes().get("employeeid").toString().contains("m"));
            }
            user.setId(OAuth2user.getId());
            user.setName(OAuth2user.getAttributes().get("name").toString());
            user.setEmployee(user.isEmployee() || OAuth2user.getAttributes().get("employeeid").toString().contains("m"));
            if (user.isEmployee()) {
                user.setEmail(OAuth2user.getAttributes().get("email").toString());
            }
            OAuth2user.getUserRepository().save(user);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
