//package nl.utwente.sekhmet.api;
//
//import nl.utwente.sekhmet.auth.SupernaturalJdbcOAuth2AuthorizedClientService;
//import nl.utwente.sekhmet.canvas.CanvasApiClient;
//import nl.utwente.sekhmet.jpa.model.User;
//import nl.utwente.sekhmet.jpa.repositories.UserRepository;
//import nl.utwente.sekhmet.auth.SupernaturalAuthUser;
//import nl.utwente.sekhmet.ut.UTOAuth2User;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.client.*;
//import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
//import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
//import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
//import org.springframework.web.bind.annotation.*;
//
//import javax.servlet.http.HttpServletRequest;
//
//@RestController
//@RequestMapping(value = "/api/try")
//public class TryNewThingsController {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Qualifier("oAuth2AuthorizedClientService")
//    @Autowired
//    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
//
////    @Autowired
////    private DefaultTokenServices tokenServices;
//
//    @GetMapping("/auth-user")
//    public String xxx(HttpServletRequest request, @AuthenticationPrincipal SupernaturalAuthUser principal) {
//        User user = principal.getUser();
//
////        CanvasApiClient canvasClient = new CanvasApiClient(user);
////        return canvasClient.getCoursesAvailableToImport().toString();
//
////        System.out.println(oAuth2AuthorizedClientService);
//
////        SupernaturalJdbcOAuth2AuthorizedClientService jdbcOAuth2AuthorizedClientService = (SupernaturalJdbcOAuth2AuthorizedClientService) oAuth2AuthorizedClientService;
////        OAuth2AuthorizedClient authorizedClient = jdbcOAuth2AuthorizedClientService.loadAuthorizedClient("canvas", principal.getId().toString());
////
////        jdbcOAuth2AuthorizedClientService.renewAccessTokenIfExpired(authorizedClient);
//
//        return "Canvasclient? Ben je daar? Nee.";
//    }
//
//    @GetMapping("/canvas-login")
//    public String canvaslogin(HttpServletRequest request, @AuthenticationPrincipal SupernaturalAuthUser principal) {
//        return "<a href='/oauth2/token/canvas'>Canvas login</a>";
//    }
//
//}