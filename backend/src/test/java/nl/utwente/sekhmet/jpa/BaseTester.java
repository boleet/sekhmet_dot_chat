package nl.utwente.sekhmet.jpa;

import nl.utwente.sekhmet.jpa.model.Course;
import nl.utwente.sekhmet.jpa.model.Test;
import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.*;
import nl.utwente.sekhmet.ut.UTOAuth2User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.time.Instant;
import java.util.*;

public class BaseTester {
    public static UTOAuth2User setUpDummyUser(Long id, UserRepository userRepository) {
        UTOAuth2User user;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "my-id");
        attributes.put("email", "bwatkins@test.org");
        List<GrantedAuthority> authorities = Collections.singletonList(
                new OAuth2UserAuthority("ROLE_USER", attributes));
        user = new UTOAuth2User(authorities, new OidcIdToken("1", Instant.now(), Instant.now().plusSeconds(60), attributes), null);
        user.setId(id);
        user.setUserRepository(userRepository);
        return user;
    }

    public static void setupTestDatabase(UserRepository ur, CourseRepository crr, ConversationRepository cnr, EnrollmentRepository er,
                                               TestRepository tr, MessageRepository mr) {
        List<User> uTemp =  userDataSetup(ur);
        courseDataSetup(ur, crr, uTemp);
    }

    public static void cleanupTestDatabase(UserRepository ur, CourseRepository crr, ConversationRepository cnr, EnrollmentRepository er,
                                           TestRepository tr, MessageRepository mr) {
        courseDataCleanup(crr, cnr, tr);
        userDataCleanup(ur);
    }

    public static List<User> userDataSetup(UserRepository userRepository) {
        List<User> res = new ArrayList<>();
        User u = new User(9990009990L, "$UNIQNAM MC$", null);
        u.setEmployee(true);
        res.add(u);
        userRepository.save(u);

        u = new User(9990009991L, "$UNIQNAM T$", null);
        u.setEmployee(true);
        res.add(u);
        userRepository.save(u);

        u = new User(9990009992L, "$UNIQNAM UT$", null);
        u.setEmployee(true);
        res.add(u);
        userRepository.save(u);

        u = new User(9990009993L, "$UNIQNAM S$", null);
        u.setEmployee(false);
        res.add(u);
        userRepository.save(u);

        u = new User(9990009994L, "$UNIQNAM US$", null);
        u.setEmployee(false);
        res.add(u);
        userRepository.save(u);

        u = new User(9990009999L, "$UNIQNAM SA$", null);
        u.setEmployee(true);
        u.setSystemAdmin(true);
        res.add(u);
        userRepository.save(u);


        return res;
    }

    public static void courseDataSetup(UserRepository userRepository, CourseRepository courseRepository, List<User> enrolled) {
        Course c1 = new Course(null, "$UNIQUENAME1$", 1122334455L);
        Course c2 = new Course(null, "$UNIQUENAME2$", 112233445566L);
        Test t1 = new Test(null, null, "$UNIQNAM1$", null, null);
        Test t2 = new Test(null, null, "$UNIQNAM2$", null, null);
        Test t3 = new Test(null, null, "$UNIQNAM3$", null, null);

        t1.addEnrollment(enrolled.get(1), 'T');
        t1.addEnrollment(enrolled.get(3), 'S');
        t2.addEnrollment(enrolled.get(1), 'T');
        t2.addEnrollment(enrolled.get(3), 'S');
        t3.addEnrollment(enrolled.get(1), 'T');
        t3.addEnrollment(enrolled.get(3), 'S');

        c1.addTest(t1);
        c1.addTest(t2);
        c1.setModuleCoordinator(enrolled.get(0));
        courseRepository.save(c1);
        c2.addTest(t3);
        c2.setModuleCoordinator(enrolled.get(0));
        courseRepository.save(c2);
    }

    public static void courseDataCleanup(CourseRepository courseRepository, ConversationRepository conversationRepository, TestRepository testRepository) {
        courseRepository.deleteById(1122334455L);
        courseRepository.deleteById(112233445566L);
    }

    public static void userDataCleanup(UserRepository userRepository) {
        userRepository.deleteById(9990009990L);
        userRepository.deleteById(9990009991L);
        userRepository.deleteById(9990009992L);
        userRepository.deleteById(9990009993L);
        userRepository.deleteById(9990009994L);
    }
}
