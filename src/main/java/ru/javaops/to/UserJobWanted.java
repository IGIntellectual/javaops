package ru.javaops.to;

import lombok.Getter;

@Getter
public class UserJobWanted extends UserJobWantedBrief {
    private final String aboutMe;
    private final String projects;

    public UserJobWanted(String fullName, String email, String location, String skype, String resumeUrl, Boolean relocationReady, String relocation, String github, String aboutMe, String projects) {
        super(fullName, email, location, skype, resumeUrl, relocationReady, relocation, github);
        this.aboutMe = aboutMe;
        this.projects = projects;
    }
}