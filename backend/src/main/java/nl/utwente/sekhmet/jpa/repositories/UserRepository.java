package nl.utwente.sekhmet.jpa.repositories;

import nl.utwente.sekhmet.jpa.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    User findUserById(Long id);
    List<User> findTop30ByEmailLikeOrNameLike(String email, String name);

    @Query("SELECT u FROM User u WHERE CAST(id AS string) LIKE CONCAT('%',:uid,'%')")
    List<User> findTop30ByIdContaining(Long uid);
}
