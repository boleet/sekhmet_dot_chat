package nl.utwente.sekhmet.jpa.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Table;
import java.io.Serializable;

@Embeddable
@Table(name = "enrollment_id")
public class EnrollmentId implements Serializable {
    @Column(name = "test_id")
    private Long testId;

    @Column(name = "user_id")
    private Long userId;

    protected EnrollmentId() {}

    public EnrollmentId(Long testId, Long userId) {
        this.testId = testId;
        this.userId = userId;
    }
}
