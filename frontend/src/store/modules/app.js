import Vue from "vue";
import axios from "axios";

const state = {
  error: null,
  current_user: {},
};

const getters = {
  has_error: (state) => (state.error ? true : false),
  name: (state) => state.current_user.name,
  is_user_id: (state) => (id) => {
    return state.current_user.user_id == id;
  },
};

const actions = {
  // Error handling, sending to the error endpoint and showing to the user
  // If payload.show is set to false, the error is not shown to the user
  // payload.error contains the error message
  // payload.extra may contain extra information about the error for logging
  async throwError({ commit }, payload) {
    // if show is explicitely set to false, don't show to the user but only log
    let show = true;
    if ("show" in payload) {
      if (payload.show === false) {
        show = false;
      }
    }

    // if extra is given, also sent to the error endpoint
    let extra = {};
    if ("extra" in payload) {
      extra = payload.extra;
    }

    // choose between the default error message or given message
    let message = "Something went wrong";
    if ("message" in payload) {
      message = payload.message;
    }

    // choose between the default error message or given message
    let errorMessage = message;
    if ("error" in payload) {
      if ("response" in payload.error) {
        // the request returned a response
        if (
          "data" in payload.error.response &&
          typeof payload.error.response.data === "object"
        ) {
          // the response contains a data object, and its probably json
          if ("response" in payload.error.response.data) {
            // the reponse should be in the response property of the data
            errorMessage += " (" + payload.error.response.data.response + ")";
          }
        } else if ("status" in payload.error.response) {
          // we received a status code
          if (payload.error.response.status == 403) {
            errorMessage += " (not authorized)";
          }
        }
      }
    }

    let sendData = {
      timestamp: Date.now(),
      error: errorMessage,
      extra,
    };

    axios
      .post("/error", sendData)
      .then(() => {
        // posted error success
      })
      .catch((error) => {
        // error on postting to error endpoint
        console.log("POST /error didn't work :(", error);
      });

    // Uncomment if you want to display a generic error to the user
    // payload.error =
    // "Something went wrong, please use the Big Blue Button chat instead";
    if (show) {
      commit("setError", errorMessage);
    }
  },
  // Request desktop notifications permission
  askDesktopNotificationPermission() {
    if (!("Notification" in window)) {
      // this browser doesn't support desktop notifications
      return;
    } else if (Notification.permission !== "denied") {
      // we don't have permissions, but we aren't denied either
      Notification.requestPermission().then(function(permission) {
        if (permission === "granted") {
          // now we have permission
        }
      });
    }
  },
  // Display a desktop notification with the given content
  showDesktopNotification({ content }) {
    if (!("Notification" in window)) {
      // this browser doesn't support desktop notifications
      return;
    } else if (Notification.permission === "granted") {
      // we have permission to show a notification
      new Notification("There is a new announcement!", { silent: true });
    } else if (Notification.permission !== "denied") {
      // we don't have permissions, but we aren't denied either
      Notification.requestPermission().then(function(permission) {
        if (permission === "granted") {
          // now we have permission
          new Notification(content, { silent: true });
        }
      });
    }
  },
};

const mutations = {
  setError(state, error) {
    state.error = error;
  },
  unsetError(state) {
    state.error = null;
  },
  setCurrentUser(state, user) {
    Vue.set(state, "current_user", user);
    // state.current_user = user;
  },
};

export default {
  state,
  getters,
  actions,
  mutations,
};
