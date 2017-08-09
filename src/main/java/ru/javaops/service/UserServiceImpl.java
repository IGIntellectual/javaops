package ru.javaops.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.javaops.AuthorizedUser;
import ru.javaops.config.AppConfig;
import ru.javaops.model.User;
import ru.javaops.repository.UserRepository;
import ru.javaops.to.UserMail;
import ru.javaops.to.UserTo;
import ru.javaops.to.UserToExt;
import ru.javaops.util.RefUtil;
import ru.javaops.util.UserUtil;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Authenticate a user from the database.
 */
@Service("userDetailsService")
@Slf4j
public class UserServiceImpl implements UserService, org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public AuthorizedUser loadUserByUsername(final String email) {
        String lowerEmail = email.toLowerCase();
        log.info("Authenticating {}", lowerEmail);
        return new AuthorizedUser(findExistedByEmail(lowerEmail));
    }

    @Override
    @Transactional
    public String deleteByEmail(String email) {
        log.debug("Delete user " + email);
        User user = userRepository.findByEmail(email);
        if (user != null) {
            if (user.isMember()) {
                user.setActive(false);
                userRepository.save(user);
                return "User " + email + " is Member,  have been deactivated";
            } else {
                userRepository.delete(user);
                return "User " + email + " have been deleted";
            }
        } else {
            return "User " + email + " is not found";
        }
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    @Override
    public User findByEmailOrGmail(String email) {
        return userRepository.findByEmailOrGmail(email.toLowerCase());
    }

    @Override
    public User findExistedByEmail(String email) {
        return checkExist(findByEmail(email), email);
    }

    private User checkExist(User user, String email) {
        if (user == null) {
            throw new UsernameNotFoundException("Пользователь c почтой <b>" + email + "</b> не найден");
        }
        return user;
    }

    @Override
    public Set<UserMail> findByGroupName(String groupName) {
        return userRepository.findByGroupName(groupName);
    }

    @Override
    public User save(User u) {
        return userRepository.save(u);
    }

    @Override
    @Transactional
    public User update(UserToExt userToExt) {
        User user = checkNotNull(userRepository.findOne(userToExt.getId()));
        UserUtil.updateFromToExt(user, userToExt);
        return userRepository.save(user);
    }

    @Override
    public User findByEmailAndGroupName(String email, String groupName) {
        return userRepository.findByEmailAndGroupName(email, groupName);
    }

    @Override
    public User get(int id) {
        return userRepository.findOne(id);
    }

    @Override
    public User create(UserTo userTo, String channel) {
        User user = userTo.toUser();
        if (channel != null) {
            user.setSource(channel);
            String infoSource = AppConfig.infoSource.getProperty(RefUtil.isRef(channel) ? "ref" : channel);
            if (infoSource == null) {
                log.warn("??? InfoSource for '{}' not found", channel);
            }
            user.setInfoSource(infoSource);
        }
        return save(user);
    }
}
