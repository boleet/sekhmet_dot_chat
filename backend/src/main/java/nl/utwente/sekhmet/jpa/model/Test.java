package nl.utwente.sekhmet.jpa.model;

import nl.utwente.sekhmet.jpa.repositories.EnrollmentRepository;
import org.springframework.security.core.parameters.P;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "test")
public class Test {
    @Id
    @GeneratedValue
    private Long id;

    private Long startTime;
    private Long endTime;
    private String name;

    @OneToOne
    private Conversation announcements;

    @OneToOne
    private Conversation teacherConversation;


    @ManyToOne
    @JoinColumn(name="course_id", nullable = false)
    private Course course;

    @OneToMany(orphanRemoval = true, mappedBy = "test", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Enrollment> enrolled;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "test")
    private Set<Conversation> conversations;

    public Test() {
        this.enrolled = new HashSet<>();
        this.conversations = new HashSet<>();
    }

    public Test(Long startTime, Long endTime, String name, Conversation announcements, Conversation teacherConversation) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.announcements = announcements;
        this.teacherConversation = teacherConversation;
        this.enrolled = new HashSet<>();
        this.name = name;
        this.conversations = new HashSet<>();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<Enrollment> getEnrolled() {
        return enrolled;
    }

    public void setEnrolled(Set<Enrollment> enrolled) {
        this.enrolled = enrolled;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static String testToJson(Test test, List<User> students, List<User> teachers) {
        String tid = test.getId().toString();
        Long stime = test.getStartTime();
        Long etime = test.getEndTime();
        String name = test.getName();
        User mod_coord = test.getCourse().getModuleCoordinator();

        String slst = User.userToJson(students, "students");
        String tlst = User.userToJson(teachers, "teachers");

        String json = "{ \"test_id\":" + tid + ", " +
                "\"start_time\":" + stime + ", " +
                "\"end_time\":" + etime + ", " +
                "\"name\":\"" + name + "\"," +
                "\"module_coordinator\":" + User.userToJson(mod_coord, false, false) + ", ";
        json += slst + "," + tlst + "}";
        return json;
    }

    public static String testToJson(Test test) {
        String tid = test.getId().toString();
        Long stime = test.getStartTime();
        Long etime = test.getEndTime();
        String name = test.getName();

        String json = "{ \"test_id\":" + tid + ", " +
                "\"start_time\":" + stime + ", " +
                "\"end_time\":" + etime + ", " +
                "\"name\":\"" + name + "\"}";
        return json;
    }

    public static String testWithEnrollmentToJson(Test test) {
        List<Enrollment> enrolled = new ArrayList<>(test.getEnrolled());
        List<User> students = new ArrayList<>();
        List<User> teachers = new ArrayList<>();
        for (Enrollment e : enrolled) {
            if (e.getRole() == 'S') {
                students.add(e.getUser());
            } else {
                teachers.add(e.getUser());
            }
        }
        return Test.testToJson(test, students, teachers);
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Course getCourse() {
        return this.course;
    }

    public void addEnrollment(User user, char role) {
        Enrollment e = new Enrollment(user, this, role);
        //user.getEnrollments().add(e);
        this.enrolled.add(e);
    }

    public void resetEnrollments() {
//        this.enrolled = new HashSet<>();
        this.enrolled.clear();
    }

    public Conversation getAnnouncements() {
        return announcements;
    }

    public void setAnnouncements(Conversation announcements) {
        this.announcements = announcements;
    }

    public Conversation getTeacherConversation() {
        return teacherConversation;
    }

    public void setTeacherConversation(Conversation teacherConversation) {
        this.teacherConversation = teacherConversation;
    }

    public void deleteEnrollments() {
        for (Enrollment e : enrolled) {
            e.setTest(null);
        }
        enrolled.clear();
    }

    public boolean equals(Test test) {
        if (test == null) {
            return false;
        }
        if (this.id.equals(test.id)) {
            return true;
        }
        return false;
    }

    public Set<Conversation> getConversations() {
        return this.conversations;
    }

    public void addConversation(Collection<Conversation> conversations) {
        System.out.println(conversations.size());
        this.conversations.addAll(conversations);
    }

    public void addConversation(Conversation conversation) {
        this.conversations.add(conversation);
    }
}
