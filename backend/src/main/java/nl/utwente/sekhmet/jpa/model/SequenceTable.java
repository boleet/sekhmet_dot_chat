package nl.utwente.sekhmet.jpa.model;

import javax.persistence.*;

@Entity
public class SequenceTable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    public SequenceTable() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
