package nl.utwente.sekhmet.auth;

import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

public class SupernaturalJdbcOAuth2AuthorizedClientService extends JdbcOAuth2AuthorizedClientService {

    public SupernaturalJdbcOAuth2AuthorizedClientService(JdbcOperations jdbcOperations, ClientRegistrationRepository clientRegistrationRepository) {
        super(jdbcOperations, clientRegistrationRepository);
    }

    public OAuth2AuthorizedClient renewAccessTokenIfExpired(OAuth2AuthorizedClient authorizedClient) {
        if(authorizedClient.getAccessToken().getExpiresAt().isBefore(Instant.now())) {
            return this.renewAccessToken(authorizedClient);
        }
        return authorizedClient;
    }

    public OAuth2AuthorizedClient renewAccessToken(OAuth2AuthorizedClient authorizedClient) {
        DefaultRefreshTokenTokenResponseClient x = new DefaultRefreshTokenTokenResponseClient();
        OAuth2AccessTokenResponse response = x.getTokenResponse(new OAuth2RefreshTokenGrantRequest(
                authorizedClient.getClientRegistration(), authorizedClient.getAccessToken(), authorizedClient.getRefreshToken()
        ));

//        authorizedClient = new OAuth2AuthorizedClient(authorizedClient.getClientRegistration(), authorizedClient.getPrincipalName(), response.getAccessToken());

        SqlParameterValue[] parameters = new SqlParameterValue[]{
                new SqlParameterValue(Types.BLOB, response.getAccessToken().getTokenValue()), // access_token_value
                new SqlParameterValue(Types.TIMESTAMP, Timestamp.from(response.getAccessToken().getIssuedAt())), // access_token_issued_at
                new SqlParameterValue(Types.TIMESTAMP, Timestamp.from(response.getAccessToken().getExpiresAt())), // access_token_expires_at
                new SqlParameterValue(Types.VARCHAR, authorizedClient.getClientRegistration().getRegistrationId()), // client_registration_id
                new SqlParameterValue(Types.VARCHAR, authorizedClient.getPrincipalName()) // principal_name
        };

        PreparedStatementSetter pss = new ArgumentPreparedStatementSetter(parameters);
        this.jdbcOperations.update("UPDATE oauth2_authorized_client SET access_token_value = ?, access_token_issued_at = ?, access_token_expires_at = ? WHERE client_registration_id = ? AND principal_name = ?", pss);

        return new OAuth2AuthorizedClient(authorizedClient.getClientRegistration(), authorizedClient.getPrincipalName(), response.getAccessToken());
    }
}
