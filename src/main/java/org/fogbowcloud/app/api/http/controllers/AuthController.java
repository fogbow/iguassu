package org.fogbowcloud.app.api.http.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.PathParam;
import org.apache.log4j.Logger;
import org.fogbowcloud.app.api.constants.ApiDocumentation;
import org.fogbowcloud.app.api.http.services.AuthService;
import org.fogbowcloud.app.core.constants.IguassuPropertiesConstants;
import org.fogbowcloud.app.core.datastore.OAuthToken;
import org.fogbowcloud.app.core.dto.AuthDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = ApiDocumentation.Endpoint.AUTH_ENDPOINT)
@Api(ApiDocumentation.Auth.API_DESCRIPTION)
public class AuthController {
    private final Logger LOGGER = Logger.getLogger(AuthController.class);

    @Lazy
    private AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = ApiDocumentation.Endpoint.OAUTH2_ENDPOINT)
    @ApiOperation(value = ApiDocumentation.Auth.AUTHENTICATE_USER)
    public ResponseEntity<?> authenticate(
            @ApiParam(value = ApiDocumentation.Auth.AUTHORIZATION_CODE)
            @RequestBody String authorizationCode,
            @ApiParam(value = ApiDocumentation.CommonParameters.OAUTH_CREDENTIALS)
            @RequestHeader(value = IguassuPropertiesConstants.X_IDENTIFIERS) String applicationIdentifiers) {

        try {
            if (authorizationCode != null && !authorizationCode.trim().isEmpty()) {
                final AuthDTO userAuthenticatedInfo = this.authService
                        .authenticateWithOAuth2(authorizationCode, applicationIdentifiers);

                LOGGER.info("Authenticated successfully.");

                return new ResponseEntity<>(userAuthenticatedInfo, HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>("The authorization code is invalid.",
                        HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            return new ResponseEntity<>("The authorization failed with error [" + e.getMessage() +
                    "]", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping(ApiDocumentation.Endpoint.OAUTH2_REFRESH_TOKEN_ENDPOINT)
    public ResponseEntity<?> refreshToken(@PathVariable String accessToken){
        try {
            OAuthToken authDTO = this.authService.refreshToken(accessToken);
            return new ResponseEntity<>(authDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
