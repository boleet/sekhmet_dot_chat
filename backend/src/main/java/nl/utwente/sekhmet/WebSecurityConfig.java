package nl.utwente.sekhmet;

import nl.utwente.sekhmet.auth.SupernaturalJdbcOAuth2AuthorizedClientService;
import nl.utwente.sekhmet.canvas.CanvasOAuth2UserService;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import nl.utwente.sekhmet.ut.UTOAuth2SuccessHandler;
import nl.utwente.sekhmet.ut.UTOAuth2User;
import nl.utwente.sekhmet.ut.UTOAuth2UserService;
import nl.utwente.sekhmet.ut.UTOidcUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/login");
    }

    @Autowired
    private UTOidcUserService UtOidcUserService;

    @Autowired
    private CanvasOAuth2UserService canvasUserService;

    @Autowired
    private UserRepository userRepository;

//    @Bean
//    public OAuth2AuthorizedClientService oAuth2AuthorizedClientService
//            (JdbcOperations jdbcOperations, ClientRegistrationRepository clientRegistrationRepository) {
//        return new SupernaturalJdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
//    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().ignoringAntMatchers("/api/error")
                .and()
//                .csrf().disable()
                .logout()
                .and()
                .authorizeRequests()
                .antMatchers("/login")
                .permitAll()
                .anyRequest().authenticated()
                .and()
                .oauth2Login()
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                        .oidcUserService(this.UtOidcUserService)
                        .userService(this.canvasUserService)
                )
                .successHandler(new UTOAuth2SuccessHandler())
                ;
    }
}
