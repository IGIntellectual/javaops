package ru.javaops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.javaops.model.User;
import ru.javaops.to.UserJobWantedBrief;
import ru.javaops.to.UserMail;
import ru.javaops.to.UserStat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("SELECT u FROM User u " +
            "  LEFT JOIN FETCH u.roles WHERE u.id=:id")
    User get(@Param("id") Integer integer);

    @Override
    User getOne(Integer integer);

    @Query("SELECT u FROM User u " +
            "  LEFT JOIN FETCH u.roles WHERE u.email=:email")
    User findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u " +
            "  LEFT JOIN FETCH u.roles WHERE u.email=:email OR u.gmail=:email")
    User findByEmailOrGmail(@Param("email") String email);

    @Query("SELECT new ru.javaops.to.UserMailImpl(ug.user) FROM UserGroup ug " +
            " WHERE ug.group.name=:groupName AND ug.user.active=TRUE")
    Set<UserMail> findByGroupName(@Param("groupName") String groupName);

    @Query("SELECT new ru.javaops.to.UserStat(u.fullName, u.email, u.location, u.aboutMe, u.skype) FROM User u " +
            "WHERE u.statsAgree=TRUE " +
            "AND u.fullName IS NOT NULL " +
            "AND u.location IS NOT NULL " +
            "ORDER BY LOWER(u.location)")
    List<UserStat> findAllForStats();

    @Query("SELECT ug.user FROM UserGroup ug WHERE ug.user.email=:email AND ug.group.name=:groupName")
    User findByEmailAndGroupName(@Param("email") String email, @Param("groupName") String groupName);

    @Override
    @Transactional
    User save(User entity);

    //    https://jira.spring.io/browse/DATAJPA-1103
//    @Query("UPDATE User u SET u.comment = :#{#uaInfo.comment}, u.mark=:#{#uaInfo.mark}, u.bonus=:#{#uaInfo.bonus} WHERE u.email=:email")
//    void saveAdminInfo(@Param("email") String email, @Param("uaInfo") UserAdminsInfo uaInfo);
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.comment = :comment, u.mark=:mark, u.bonus=:bonus WHERE u.email=:email")
    void saveAdminInfo(@Param("email") String email, @Param("comment") String comment, @Param("mark") String mark, @Param("bonus") Integer bonus);

    @Query(value = "SELECT new ru.javaops.to.UserJobWantedBrief(u.fullName, u.email, u.location, u.skype, u.resumeUrl, u.relocationReady, u.relocation, u.github) FROM User u " +
            "WHERE u.considerJobOffers = TRUE AND u.resumeUrl IS NOT NULL AND u.hrUpdate >= :fromDate")
    List<UserJobWantedBrief> findAllJobWanted(@Param("fromDate") LocalDate fromDate);

    @Query(value = "SELECT new ru.javaops.to.UserJobWantedBrief(ug.user.fullName, ug.user.email, ug.user.location, ug.user.skype, ug.user.resumeUrl, ug.user.relocationReady, ug.user.relocation, ug.user.github) " +
            "FROM UserGroup ug " +
            "WHERE ug.user.considerJobOffers = TRUE AND ug.user.resumeUrl IS NOT NULL AND ug.user.hrUpdate >= :fromDate AND ug.group.project.id=:projectId")
    List<UserJobWantedBrief> findProjectJobWanted(@Param("fromDate") LocalDate fromDate, @Param("projectId") int projectId);

    @Query("SELECT u FROM User u " +
            "  LEFT JOIN FETCH u.userGroups WHERE u.email=:email")
    User findByEmailWithGroup(@Param("email") String email);

    @Query("SELECT u FROM User u " +
            "  LEFT JOIN FETCH u.userGroups WHERE u.github=:github")
    User findByGitHubWithGroup(@Param("github") String github);
}