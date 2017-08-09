package ru.javaops.web.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Validated
public class OAuth2Provider {

    @NotNull
    private String authorizeUrl;
    @NotNull
    private String accessTokenUrl;
    @NotNull
    private String userInfoUrl;
    @NotNull
    private String clientId;
    @NotNull
    private String clientSecret;
    @NotNull
    private String redirectUri;
    @NotNull
    private String name;
}
