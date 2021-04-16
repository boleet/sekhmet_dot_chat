package nl.utwente.sekhmet.auth;

import nl.utwente.sekhmet.jpa.model.User;

public interface SupernaturalAuthUser {
    enum authProvider {
        UTWENTE,
        CANVAS
    }

    Long getId();
    User getUser();
    authProvider getAuthProvider();
}
