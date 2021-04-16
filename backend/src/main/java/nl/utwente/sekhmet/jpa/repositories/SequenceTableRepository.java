package nl.utwente.sekhmet.jpa.repositories;

import nl.utwente.sekhmet.jpa.model.SequenceTable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SequenceTableRepository extends CrudRepository<SequenceTable, Long> {

}
