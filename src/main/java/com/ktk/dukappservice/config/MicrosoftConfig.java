package com.ktk.dukappservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "microsoft.app")
public class MicrosoftConfig {
    private String realm;
    private String clientId;
    private String clientSecret;
    private String scope;
    private String paidJobsId;
    private String unpaidJobsId;
    private String siteId;
    private String workUsersId;
    private String myshareMail;
}
