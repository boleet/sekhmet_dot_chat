package nl.utwente.sekhmet.canvas;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CanvasOAuth2SuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {



    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        // TODO: when logged in, update user's information in our database.
        // although, we need a unique identifier for this...

        // attributes from the user-info endpoint are available by:
        // String name = user.getAttributes().get("name")

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
