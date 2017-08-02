package ru.javaops.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import ru.javaops.to.UserMail;
import ru.javaops.util.PartnerUtil;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static ru.javaops.util.PartnerUtil.hasPartnerFlag;

/**
 * User: gkislin
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity implements UserMail, Serializable {

    @Column(name = "email", nullable = false, unique = true)
    @Email
    @NotEmpty
    private String email;

    @Column(name = "full_name", length = 50)
    private String fullName;

    @Column(name = "password")
    @Length(min = 5)
    private String password;

    @Email
    @Size(max = 100)
    @Column(length = 100, unique = true)
    private String gmail;

    @Size(max = 50)
    private String location;

    @Size(max = 50)
    private String phone;

    @Size(max = 100)
    @Column(name = "info_source", length = 100)
    private String infoSource;

    @Column(name = "about_me")
    private String aboutMe;

    @Column(name = "source")
    private String source;

    @Column(name = "stats_agree")
    private boolean statsAgree;

    @Column(name = "consider_job_offers")
    private Boolean considerJobOffers;

    @Column(name = "relocation_ready")
    private Boolean relocationReady;

    @Column(name = "job_through_topjava")
    private Boolean jobThroughTopjava;

    @Column(name = "under_recruitment")
    private Boolean underRecruitment;

    @URL
    @Column(name = "resume_url")
    private String resumeUrl;

    @URL
    @Size
    @Column
    private String website;

    @Column
    private String company;

    @Size(max = 50)
    @Column
    private String skype;

    @Column
    private String github;

    @Size(max = 100)
    @Column
    private String vk;

    @Column(name = "active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean active = true;

    @Column(name = "registered_date", columnDefinition = "TIMESTAMP DEFAULT NOW()")
    private Date registeredDate = new Date();

    @Column
    private String relocation;

    @Column
    private String mark;

    @Column(name = "bonus", columnDefinition = "INT DEFAULT 0")
    private int bonus = 0;

    @Column(name = "activated_date")
    private Date activatedDate;

    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @ElementCollection(fetch = FetchType.LAZY)
    private Set<Role> roles;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserGroup> userGroups;

    private LocalDate hrUpdate;

    private String comment;

    @Column(name = "partner_flag", columnDefinition = "bigint default 0")
    private long partnerFlag;

    @Transient
    private boolean newCandidate = false;

    public User() {
    }

    public User(String email, String nameSurname, String location, String skype) {
        this(null, email, nameSurname, location, skype);
    }

    public User(Integer id, String email, String fullName, String location, String skype) {
        super(id);
        this.email = email;
        this.fullName = fullName;
        this.location = location;
        this.skype = skype;
    }

    // Exception evaluating SpringEL expression: "user.firstName" for default method in UserMail
    public String getFirstName() {
        return fullName == null ? "" : (substringBefore(capitalize(fullName), " "));
    }

    public void setActivatedDate(Date activatedDate) {
        this.activatedDate = activatedDate;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Role> getRoles() {
        if (roles == null) {
            roles = EnumSet.noneOf(Role.class);
        }
        return roles;
    }

    public boolean isPartner() {
        return hasRole(Role.ROLE_PARTNER);
    }

    public boolean isAdmin() {
        return hasRole(Role.ROLE_ADMIN);
    }

    public boolean isPartnerCandidateNotify() {
        return hasPartnerFlag(partnerFlag, PartnerUtil.CANDIDATE_NOTIFY);
    }

    public boolean isPartnerCorporateStudy() {
        return hasPartnerFlag(partnerFlag, PartnerUtil.CORPORATE_STUDY);
    }

    public boolean isMember() {
        return hasRole(Role.ROLE_MEMBER);
    }

    private boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public void setPartnerCandidateNotify(boolean flag) {
        partnerFlag = PartnerUtil.setPartnerFlag(partnerFlag, PartnerUtil.CANDIDATE_NOTIFY, flag);
    }

    public void setPartnerCorporateStudy(boolean flag) {
        partnerFlag = PartnerUtil.setPartnerFlag(partnerFlag, PartnerUtil.CORPORATE_STUDY, flag);
    }

    public int addBonus(int bonus) {
        this.bonus += bonus;
        return bonus;
    }

    @Override
    public String toString() {
        return "User (" +
                "id=" + getId() +
                ", email=" + email +
                ", fullName='" + fullName + '\'' +
                ", location=" + location +
                ", infoSource=" + infoSource +
                ')';
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && email.equals(((User) o).email);
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }
}
