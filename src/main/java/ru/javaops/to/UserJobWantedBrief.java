package ru.javaops.to;

import lombok.Getter;
import org.apache.commons.lang3.text.WordUtils;

import java.time.LocalDate;

@Getter
public class UserJobWantedBrief {
    private final String fullName;
    private final String email;
    private final String location;
    private final String skype;
    private final String resumeUrl;
    private final Boolean relocationReady;
    private final String relocation;
    private final String github;
    private final LocalDate hrUpdate;

    public UserJobWantedBrief(String fullName, String email, String location, String skype, String resumeUrl,
                              Boolean relocationReady, String relocation, String github, LocalDate hrUpdate) {
        this.fullName = WordUtils.capitalize(fullName);
        this.email = email;
        this.location = WordUtils.capitalize(location, ' ', '-', '.', '/', ',');
        this.skype = skype;
        this.resumeUrl = resumeUrl;
        this.relocationReady = relocationReady;
        this.relocation = relocation;
        this.github = github;
        this.hrUpdate = hrUpdate;
    }
}