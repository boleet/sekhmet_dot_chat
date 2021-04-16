package nl.utwente.sekhmet.jpa.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;

@Entity
@Table(name = "enrollment")
public class Enrollment implements Serializable {
    @EmbeddedId
    private EnrollmentId enrollmentId;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @MapsId("userId")
    @JsonBackReference
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("testId")
    @JsonBackReference
    private Test test;

    @Column(name="role")
    private char role;

    protected Enrollment() {}
    public Enrollment(User user, Test test, char role) {
        setUser(user);
        setTest(test);
        setRole(role);
        this.enrollmentId = new EnrollmentId(test.getId(), user.getId());
    }

    public class Role {
        public static final char STUDENT = 'S';
        public static final char TEACHER = 'T';
    }

    public char getRole() {
        return role;
    }

    public void setRole(char role) {
        this.role = role;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public EnrollmentId getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(EnrollmentId enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public static String enrollmentToJson(Enrollment enrollment) {
        String user = enrollment.getUser().getId().toString();
        String test = Test.testToJson(enrollment.test);
        test = test.substring(1, test.length()-2); //remove accolades
        String role = Character.toString(enrollment.getRole());
        String json = "\"" + user + "\": { \"user_id\":" + user + ", " +
                test +
                "\"role\":\"" + role + "\"}";

        return json;
    }

    public static String enrollmentToJson(Collection<Enrollment> enrollments) {
        StringBuilder sb = new StringBuilder("\"enrollments\":{");
        boolean first = true;
        for (Enrollment e : enrollments) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(Enrollment.enrollmentToJson(e));
        }
        return sb.append("}").toString();
    }
}
