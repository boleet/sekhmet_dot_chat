package nl.utwente.sekhmet.jpa.model;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course")
public class Course {
    @Id
    @Column(name = "canvas_id")
    private Long canvasId;

    @OneToOne(cascade = CascadeType.MERGE)
    private User moduleCoordinator;

    private String name;

    @OneToMany(mappedBy="course", cascade=CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Test> tests = new HashSet<>();


//    @CreationTimestamp
//    @Column(name = "created_at")
//    public Date createdAt;

    @Column(name = "created_at")
    private Long createdAt;

    public Course() {
        this.createdAt = System.currentTimeMillis();
    }


    public Course(User moduleCoordinator, String name, Long canvasId) {
        this();
        this.moduleCoordinator = moduleCoordinator;
        this.name = name;
        this.canvasId = canvasId;
    }

    public Long getCanvasId() {
        return this.canvasId;
    }

    public void setId(Long canvasId) {
        this.canvasId = canvasId;
    }

    public User getModuleCoordinator() {
        return this.moduleCoordinator;
    }

    public void setModuleCoordinator(User moduleCoordinator) {
        this.moduleCoordinator = moduleCoordinator;
    }

    public Set<Test> getTests() {
        return this.tests;
    }

    public void addTest(Test test) {
        test.setCourse(this);
        this.tests.add(test);
    }

    public void deleteTest(Test test) {
        this.tests.remove(test);
        test.setCourse(null);
        test.deleteEnrollments();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " ; " + canvasId + " ; " + moduleCoordinator;
    }

    public static String courseToJson(Course c, boolean includeEnrollments) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"" + c.getName() + "\",");
        sb.append("\"canvas_id\":" + c.getCanvasId() + ",");
        sb.append("\"created_at\":").append(c.getCreatedAt()).append(",");
        sb.append("\"module_coordinator\":" + User.userToJson(c.getModuleCoordinator(), false, false));
        sb.append(", \"tests\":{");
        boolean first = true;
        for (Test t : c.getTests()) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append("\"" + t.getId() + "\":");
            if (includeEnrollments) {
                sb.append(Test.testWithEnrollmentToJson(t));
            } else {
                sb.append(Test.testToJson(t));
            }
        }
        sb.append("}}");
        return sb.toString();
    }

    public Long getCreatedAt() {
        return this.createdAt;
    }

}
