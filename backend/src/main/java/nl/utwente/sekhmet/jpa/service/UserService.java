package nl.utwente.sekhmet.jpa.service;

import nl.utwente.sekhmet.jpa.model.User;
import nl.utwente.sekhmet.jpa.repositories.UserRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class UserService {
    //POST-create
    public static Map<Long, User> postUser(JSONArray a, UserRepository userRepository) throws JSONException {
        int len = a.length();
        Map<Long, User> res = new HashMap<>();
        for (int i = 0; i < len; i ++) {
            JSONObject temp = a.getJSONObject(i);
            Long id = temp.getLong("id");
            res.put(id, UserService.postUser(temp, userRepository));
        }
        return res;
    }

    public static User postUser(JSONObject userJson, UserRepository userRepository) throws JSONException {
        /* assuming format:
         * {
         * name = "name lastname"
         * id = "12345678"
         *  (email : j@e.d  OPTIONAL)
         *  (system_admin : true OPTIONAL)
         */
        String email;
        try {
            email = userJson.getString("email");
        } catch (JSONException e) {
            email = null;
        }
        User user = new User(userJson.getLong("id"), userJson.getString("name"), email);
        boolean sysAd;
        try {
            sysAd = userJson.getBoolean("system_admin");
        } catch (JSONException e) {
            sysAd = false;
        }
        user.setSystemAdmin(sysAd);
        postUser(user, userRepository);
        return user;
    }

    //POST-retrieve

    public static void postUser(User user, UserRepository userRepository) {
        userRepository.save(user);
    }

    //GET-retrieve

    public static User getUser(Long id, UserRepository userRepository) {
        User user = userRepository.findUserById(id);
        if (user == null) {
            throw new NoSuchElementException("User_id: " + id + " ||| The user does not exists!");
        }
        return userRepository.findById(id).get();
    }

    //DELETE
    public static void deleteUser(Long id, UserRepository userRepository){
        userRepository.deleteById(id);
    }
}
