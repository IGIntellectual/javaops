package ru.javaops.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

/**
 * GKislin
 * 20.08.2015.
 */
@ConfigurationProperties("app")
@Validated
@Getter
@Setter
public class AppProperties {

    /**
     * Test email
     */
    @NotNull
    private String email;

    /**
     * Interval for update templates
     */
    @NotNull
    private int cacheSeconds;

    /**
     * Secret for generate activation key
     */
    @NotNull
    private String activationSecretSalt;

    /**
     * Secret key
     */
    @NotNull
    private String secretKey;

    /**
     * Host url
     */
    @NotNull
    private String hostUrl;

    /**
     * Tinkoff TerminalKey
     */
    @NotNull
    private String terminalKey;

    /**
     * Tinkoff TerminalPass
     */
    @NotNull
    private String terminalPass;

    private Boolean testMode;

    public boolean isTestMode() {
        return testMode != null && testMode;
    }
}
