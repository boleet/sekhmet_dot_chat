package nl.utwente.sekhmet.jpa.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static nl.utwente.sekhmet.jpa.model.Enrollment.Role.STUDENT;
import static nl.utwente.sekhmet.jpa.model.Enrollment.Role.TEACHER;

@Entity
@Table(name="user")
public class User {
    @Id
    @Column(name = "id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "email")
    private String email;
    @Column(name = "system_admin", columnDefinition="BOOLEAN DEFAULT false", nullable = false)
    private Boolean systemAdmin;
    @Column(name = "employee", columnDefinition="BOOLEAN DEFAULT false", nullable = false)
    private Boolean employee;
    @Column(name = "canvas_token")
    private String canvasToken;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private Set<Message> messages;

    @OneToMany(mappedBy = "user1", cascade = CascadeType.ALL)
    private Set<Conversation> conversations;

    @OneToMany(orphanRemoval = true, mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<Enrollment> enrollments;


    public User() {
        this.enrollments = new HashSet<>();
        this.systemAdmin = false;
        this.employee = false;
        this.canvasToken = null;
    }

    public User(Long id, String name, String email) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.enrollments = new HashSet<>();
        //dont forget to set field System admin if it needs to be true
        this.systemAdmin = false;
        this.employee = false;
        this.canvasToken = null;
    }


    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Enrollment> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(Set<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCanvasToken() {
        return canvasToken;
    }

    public void setCanvasToken(String canvasToken) {
        this.canvasToken = canvasToken;
    }

    @Override
    public boolean equals(Object user) {
        if (user == null) {
            return false;
        } else if (! ((User) user).getId().equals(this.id)) {
            return false;
        }
        return true;
    }

    @Override
    public final int hashCode() {
        int i = 17 * this.id.intValue();
        return i;
    }

    public static String userToJson(User user) {
        return userToJson(user, false);
    }

    public static String userToJson(User user, boolean email) {
        return User.userToJson(user, email, true);
    }

    public static JsonObject usersToJsonObject(List<User> users, boolean email, List<Character> roles){

        JsonObject res = new JsonObject();

        for (int i = 0; i < users.size(); i ++) {
            User u = users.get(i);
            JsonObject up = (JsonObject) User.userToJsonObject(u, email);
            //add roles by inserting into json string
            if (roles != null) {
                up.addProperty("is_supervisor",roles.get(i).equals('T'));
            }
            res.add(u.getId().toString(),up);

        }
        return res;

    }

    public static JsonElement userToJsonObject(User user, boolean email){
        if (user == null) {
            return null;
        }
        JsonObject res = new JsonObject();
        res.addProperty("name",user.getName());
        if(email) {
            res.addProperty("email", user.getEmail());
        }
        res.addProperty("is_employee",user.isEmployee());
        res.addProperty("user_id",user.getId());
        res.addProperty("has_canvas_token",user.getCanvasToken()!=null);
        return res;
    }

    public static String userToJson(User user, boolean email, boolean key) {
        if (user == null) {
            return "null";
        }
        String res = "";
        if(key) {
            res += "\"" + user.getId() + "\":";
        }
        res += "{\"name\":\"" + user.getName() + "\",";
        if (email) {
            res += "\"email\":\"" + user.getEmail() + "\",";
        }
        res += "\"is_employee\":" + user.isEmployee() + ",";
        res += "\"user_id\":" + user.getId() + ",";
        res += "\"has_canvas_token\":" + (user.getCanvasToken()!=null) + "}";
        return res;
    }

    public static String userToJson(List<User> users, String title, boolean email, List<Character> roles) {
        StringBuilder sb = new StringBuilder("\"" + title + "\":{");
        boolean first = true;
        for (int i = 0; i < users.size(); i ++) {
            User u = users.get(i);
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            String r = User.userToJson(u, email);
            //add roles by inserting into json string
            if (roles != null) {
                sb.append(r.substring(0, r.length()-1)).append(",\"is_supervisor\":");
                sb.append(roles.get(i).equals('T')).append("}");
            } else {
                sb.append(r);
            }

        }
        String res = sb.toString();
        res += "}";
        return res;
    }

    public static String userToJson(List<User> users, String title) {
        return userToJson(users, title, false, null);
    }

    public static String userToJson(List<User> users, String title, List<Character> roles) {
        return userToJson(users, title, false, roles);
    }

    public static String userToJson(List<User> users, String title, boolean email) {
        return userToJson(users, title, email, null);
    }

    public Boolean isSystemAdmin() {
        return systemAdmin;
    }

    public void setSystemAdmin(boolean admin) {
        this.systemAdmin = admin;
    }

    public Boolean isEmployee() {
        return systemAdmin || this.employee;
    }

    public void setEmployee(boolean e) {
        this.employee = e;
    }

    public Enrollment getEnrollment(Long testId) {
        Optional<Enrollment> enrollmentOptional = this.enrollments.stream()
                .filter(e -> e.getTest().getId() == testId)
                .findAny();
        if(enrollmentOptional.isPresent()) {
            return enrollmentOptional.get();
        } else {
            return null;
        }
    }

    public Enrollment getEnrollment(Test test) {
        return this.getEnrollment(test.getId());
    }

    public boolean hasRoleInTest(Long testId, char role) {
        Enrollment enrollment = this.getEnrollment(testId);
        if(enrollment != null) {
            return enrollment.getRole() == role;
        }
        return false;
    }

    public boolean hasRoleInTest(Test test, char role) {
        return this.hasRoleInTest(test.getId(), role);
    }

    public boolean isCoordinatorOfCourse(Course course) {
        return this.isSystemAdmin() || course.getModuleCoordinator().getId() == this.getId();
    }

    public boolean isStudentInTest(Test test) {
        return this.isStudentInTest(test.getId());
    }

    public boolean isStudentInTest(Long testId) {
        return this.hasRoleInTest(testId, STUDENT);
    }

    public boolean isTeacherInTest(Long testId) {
        return this.hasRoleInTest(testId, TEACHER);
    }

    public boolean isTeacherInTest(Test test) {
        return this.isTeacherInTest(test.getId());
    }

    public boolean isCoordinatorOfTest(Test test) {
        return this.isCoordinatorOfCourse(test.getCourse());
    }
}
