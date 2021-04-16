import Vue from "vue";
import axios from "axios";
import * as websocket from "../../scripts/websocket.js";

const state = {
  active_test_id: null,
  active_chat: null,
  tests: {},
  users: {},
  CHAT_ANNOUNCEMENTS_NAME: "Announcements",
  CHAT_ANNOUNCEMENTS: 0,
  CHAT_GROUP: 1,
  CHAT_GROUP_NAME: "Teacher chat",
  CHAT_ONETOONE: 2,
  CHAT_QUESTIONS_NAME: "Questions",
  PROPERTY_IGNORE: 0,
  PROPERTY_PRESENT: 1,
  PROPERTY_VALID: 2,
};

const getters = {
  active_test: (state) => {
    return state.tests[state.active_test_id];
  },
  // Return if the current user is not enrolled in the test (thus a spectator, i.e. system admin)
  is_spectator: (state, getters, rootState) => {
    if (!getters.active_test) {
      return false;
    }
    if (!rootState.app.current_user || !rootState.app.current_user.user_id) {
      return false;
    }
    if (!getters.active_test.people[rootState.app.current_user.user_id]) {
      return true;
    }
    return false;
  },
  // Return if the current user is a supervisor
  is_supervisor: (state, getters, rootState) => {
    if (!getters.active_test) {
      return false;
    }
    if (!rootState.app.current_user || !rootState.app.current_user.user_id) {
      return false;
    }
    if (!getters.active_test.people[rootState.app.current_user.user_id]) {
      return false;
    }
    if (
      getters.active_test.people[rootState.app.current_user.user_id]
        .is_supervisor
    ) {
      return true;
    }
    return false;
  },
};

const actions = {
  // Get all information for a specific test with the given test_id
  async loadTest({ commit, dispatch }, { test_id }) {
    axios
      .get("/tests/" + test_id)
      .then(function(response) {
        // Set the type of conversation
        let open_announcements_chat = -1;
        for (const chat of Object.values(response.data.chats)) {
          if (chat.name === state.CHAT_ANNOUNCEMENTS_NAME) {
            chat.type = state.CHAT_ANNOUNCEMENTS;
            // If the announcement chat has messages, store the ID
            if (Object.keys(chat.messages).length !== 0) {
              open_announcements_chat = chat.chat_id;
            }
          } else if (chat.name === state.CHAT_GROUP_NAME) {
            chat.type = state.CHAT_GROUP;
          } else {
            chat.type = state.CHAT_ONETOONE;
          }
        }

        response.data.latest_announcement = {
          show: false,
          message: { content: "" },
        };

        // Set local test data to the response
        commit("setTest", {
          test_id: response.data.test_id,
          test: response.data,
        });
        // If there is an announcement, open announcement chat on loading chats
        if (open_announcements_chat !== -1) {
          commit("setActiveChatById", { chat_id: open_announcements_chat });
        }
      })
      .catch((error) => {
        dispatch(
          "throwError",
          {
            message: "Error while getting test info",
            error,
            extra: {
              test_id,
            },
          },
          { root: true }
        );
      });
  },
  // Get all user data for a specific test
  async loadUsers({ commit, dispatch }, { test_id }) {
    axios
      .get("/tests/" + test_id + "/users")
      .then(function(response) {
        // If there are any students, add them to the local people data
        let students = response.data.students;
        if (students) {
          if (Object.keys(students).length !== 0) {
            commit("addUsers", { users: students });
          }
        } else {
          dispatch(
            "throwError",
            {
              message: "Error while getting test users: no students object",
              extra: {
                test_id,
              },
            },
            { root: true }
          );
        }
        // If there are any supervisors, add them to the local people data
        let teachers = response.data.teachers;
        for (const teacher in teachers) {
          teachers[teacher].is_supervisor = true;
        }
        if (teachers) {
          if (Object.keys(teachers).length !== 0) {
            commit("addUsers", { users: teachers });
          }
        } else {
          dispatch(
            "throwError",
            {
              message: "Error while getting test users: no teachers object",
              extra: {
                test_id,
              },
            },
            { root: true }
          );
        }
      })
      .catch((error) => {
        dispatch(
          "throwError",
          {
            message: "Error while getting test users",
            error,
            extra: {
              test_id,
            },
          },
          { root: true }
        );
      });
  },
  // Sent a message over the websocket
  async sendWsMessage({ dispatch }, { message }) {
    let sentOverWebsocket = websocket.sendToServer(message);
    if (!sentOverWebsocket) {
      dispatch(
        "throwError",
        {
          message: "Error while sending message: sending over websocket failed",
          extra: message,
        },
        { root: true }
      );
    }
  },
  // Handler that is called when a websocket message is received
  receivedWsMessage({ dispatch, commit }, message) {
    if (!message.messageType) {
      dispatch(
        "throwError",
        {
          message:
            "Error on received websocket message: no message type specified",
          extra: message,
        },
        { root: true }
      );
    }
    if (!message.receiverIds) {
      dispatch(
        "throwError",
        {
          message:
            "Error on received websocket message: this message was not intended for you",
          extra: message,
        },
        { root: true }
      );
    }
    // Call the appropriate function, depending on the message type
    switch (message.messageType) {
      case "message_final":
        dispatch("receivedChatMessage", { message: message.message });
        break;
      case "conversation_unread":
        dispatch("receivedMessageUnread", { message: message.message });
        break;
      case "conversation_assigned":
        dispatch("receivedMessageAssigned", { message: message.message });
        break;
      case "message_delete":
        dispatch("receivedMessageDeleted", { message: message.message });
        break;
      case "message_edit":
        dispatch("receivedChatMessage", { message: message.message });
        break;
      case "conversation_typing":
        dispatch("receivedConversationTyping", { message: message.message });
        break;
      case "nack":
        if (message.message && message.message.messageType) {
          if (message.message.messageType === "message_final") {
            // A send message is nacked, so indicate in the message list
            let chatMessage = message.message.message;
            dispatch("messageHasValidProperties", {
              message: chatMessage,
              properties: {
                test_id: state.PROPERTY_VALID,
                chat_id: state.PROPERTY_VALID,
                sender_id: state.PROPERTY_VALID,
                message_id: state.PROPERTY_PRESENT,
                timestamp: state.PROPERTY_PRESENT,
              },
            }).then((isValid) => {
              if (isValid) {
                commit("setChatMessageError", {
                  message: chatMessage,
                  error: message.error,
                });
              }
            });
          }
        }
        if (message.error) {
          dispatch("throwError", {
            message: "Something went wrong with your message: " + message.error,
            extra: {
              message,
            },
          });
        }
        break;
    }
  },
  // On received a chat message
  receivedChatMessage({ commit, dispatch }, { message }) {
    // Check if the message is valid
    dispatch("messageHasValidProperties", {
      message,
      properties: {
        test_id: state.PROPERTY_VALID,
        chat_id: state.PROPERTY_VALID,
        sender_id: state.PROPERTY_VALID,
        message_id: state.PROPERTY_PRESENT,
        timestamp: state.PROPERTY_PRESENT,
        content: state.PROPERTY_PRESENT,
      },
    }).then((isValid) => {
      if (isValid) {
        // Check if chat is not active, and message is a new message, to increment chat unread
        if (
          !state.active_chat ||
          state.active_chat.chat_id != message.chat_id
        ) {
          if (
            !state.tests[message.test_id].chats[message.chat_id].messages[
              message.sender_id + ":" + message.message_id
            ]
          ) {
            // if not an update, increment unread chat
            dispatch("updateChatUnread", {
              chat: state.tests[message.test_id].chats[message.chat_id],
              number_of_unread:
                state.tests[message.test_id].chats[message.chat_id].unread + 1,
              force: true,
            });
          }
        } else {
          // or if the chat is active, maybe we should notify the other teachers
          if (
            state.active_chat.name !== state.CHAT_ANNOUNCEMENTS_NAME &&
            state.active_chat.name !== state.CHAT_GROUP_NAME
          ) {
            dispatch("updateChatUnread", {
              chat: state.tests[message.test_id].chats[message.chat_id],
              number_of_unread: 0,
              force: true,
            });
          }
        }

        // The messsage does not exist (not an edit), so update latest chat message
        if (
          !state.tests[message.test_id].chats[message.chat_id].messages[
            message.sender_id + ":" + message.message_id
          ]
        ) {
          commit("setLatestChatMessage", { message });
        }
        // Received a chat message without any errors, so add it to the state
        commit("addChatMessage", { message });

        // it's a new announcement
        if (
          state.tests[message.test_id].chats[message.chat_id].name ===
          state.CHAT_ANNOUNCEMENTS_NAME
        ) {
          // display announcement and make a notification
          commit("setLatestAnnouncementMessage", { message });
          commit("setLatestAnnouncementShow", {
            test_id: message.test_id,
            show: true,
          });
          dispatch(
            "showDesktopNotification",
            { content: "New announcement" },
            { root: true }
          );
        }
      }
    });
  },
  // On received conversation unread update
  receivedMessageUnread({ commit, dispatch }, { message }) {
    // Check if the message is valid
    dispatch("messageHasValidProperties", {
      message,
      properties: {
        test_id: state.PROPERTY_VALID,
        chat_id: state.PROPERTY_VALID,
      },
    }).then((isValid) => {
      if (isValid) {
        if (message.unread !== undefined) {
          if (
            !state.active_chat ||
            state.active_chat.chat_id != message.chat_id
          ) {
            // we don't have this chat open, so update local unread value
            commit("setChatUnread", {
              chat: state.tests[message.test_id].chats[message.chat_id],
              number_of_unread: message.unread,
            });
          } else {
            // we have the chat open, so reject if not zero
            if (message.unread != 0) {
              dispatch("updateChatUnread", {
                chat: state.tests[message.test_id].chats[message.chat_id],
                number_of_unread: 0,
                force: true,
              });
            }
          }
        }
      }
    });
  },
  // On received a conversation assigned update
  receivedMessageAssigned({ commit, dispatch }, { message }) {
    // Check if the message is valid
    dispatch("messageHasValidProperties", {
      message,
      properties: {
        test_id: state.PROPERTY_VALID,
        chat_id: state.PROPERTY_VALID,
        user_id: state.PROPERTY_VALID,
      },
    }).then((isValid) => {
      if (isValid) {
        if (message.user_id === "NULL") {
          // Removed the assignment
          commit("setChatAssigned", {
            chat: state.tests[message.test_id].chats[message.chat_id],
            name: null,
          });
        } else {
          // assign a user to the chat, with a readable name
          let name =
            state.tests[message.test_id].chats[message.chat_id].people[
              message.user_id
            ].name;
          if (name.indexOf(",") > -1) {
            name = name.substr(0, name.indexOf(","));
          } else if (name.indexOf(" ") > -1) {
            name = name.substr(0, name.indexOf(" "));
          }
          commit("setChatAssigned", {
            chat: state.tests[message.test_id].chats[message.chat_id],
            name: name,
          });
        }
      }
    });
  },
  // On received a message deleted update
  receivedMessageDeleted({ commit, dispatch }, { message }) {
    // Check if the message is valid
    dispatch("messageHasValidProperties", {
      message,
      properties: {
        test_id: state.PROPERTY_VALID,
        chat_id: state.PROPERTY_VALID,
        sender_id: state.PROPERTY_VALID,
        message_id: state.PROPERTY_VALID,
      },
    }).then((isValid) => {
      if (isValid) {
        commit("removeChatMessage", {
          message:
            state.tests[message.test_id].chats[message.chat_id].messages[
              message.sender_id + ":" + message.message_id
            ],
        });
        // If the latest announcement is deleted, hide the announcement banner
        if (
          state.tests[message.test_id].chats[message.chat_id].name ===
          state.CHAT_ANNOUNCEMENTS_NAME
        ) {
          if (
            message.message_id ===
            state.tests[message.test_id].latest_announcement.message.message_id
          ) {
            // the latest announcement has been deleted, so hide the pop-up
            commit("setLatestAnnouncementShow", {
              test_id: message.test_id,
              show: false,
            });
          }
        }
      }
    });
  },
  // Update the unread value of the given chat to number_of_unread
  // for yourself, and other supervisors if you're a supervisor
  updateChatUnread(
    { commit, dispatch, getters, rootState },
    { chat, number_of_unread, force }
  ) {
    // only update if there is something to update, or if we want to force update
    if ((chat.unread != number_of_unread) | (force === true)) {
      commit("setChatUnread", { chat, number_of_unread });

      // Announcements and teachers chat don't have shared unread state
      if (
        chat.name != state.CHAT_ANNOUNCEMENTS_NAME &&
        chat.name != state.CHAT_GROUP_NAME
      ) {
        let iAmSupervisor =
          getters.active_test.people[rootState.app.current_user.user_id]
            .is_supervisor;

        let people = Object.values(chat.people);

        let receivers = [];
        for (const recipient in people) {
          // add everyone that is the same as me (supervisor or not) to the receiers list
          if (
            getters.active_test.people[people[recipient].user_id] &&
            getters.active_test.people[people[recipient].user_id]
              .is_supervisor === iAmSupervisor
          ) {
            receivers.push("" + people[recipient].user_id);
          }
        }

        let msg_wrapped = {
          messageType: "conversation_unread",
          receiverIds: receivers,
          message: {
            test_id: chat.test_id,
            chat_id: chat.chat_id,
            unread: number_of_unread,
          },
        };
        // Send message over websocket
        dispatch("sendWsMessage", {
          message: msg_wrapped,
        });
      }
    }
  },
  // On received someone is typing in the conversation
  receivedConversationTyping({ commit, dispatch }, { message }) {
    // Check if the message is valid
    dispatch("messageHasValidProperties", {
      message,
      properties: {
        test_id: state.PROPERTY_VALID,
        chat_id: state.PROPERTY_VALID,
        user_id: state.PROPERTY_VALID,
      },
    }).then((isValid) => {
      if (isValid) {
        if (message.typing) {
          // set user typing if the user started typing
          commit("addChatTyping", {
            chat: state.tests[message.test_id].chats[message.chat_id],
            user_id: message.user_id,
          });
        } else {
          // remove user typing if the user stopped typing
          commit("removeChatTyping", {
            chat: state.tests[message.test_id].chats[message.chat_id],
            user_id: message.user_id,
          });
        }
      }
    });
  },
  // Helper function to check validity of message
  // Presents means the property is not undefined or null
  // Valid means it it's not impossible given the current local data structure
  // I.e., retrieval based on the property won't crash
  messageHasValidProperties({ dispatch, rootState }, { message, properties }) {
    // Check for the test ID
    if ("test_id" in properties) {
      // Check if the test ID exists
      if (properties.test_id >= state.PROPERTY_PRESENT) {
        if (
          typeof message.test_id === "undefined" ||
          message.test_id === null
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the test ID is not present",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
      // Check if the test ID is valid
      if (properties.test_id >= state.PROPERTY_VALID) {
        if (!state.tests[message.test_id]) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the test ID is not valid",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
    }

    // Check for chat ID
    if ("chat_id" in properties) {
      // Check if the chat ID exists
      if (properties.chat_id >= state.PROPERTY_PRESENT) {
        if (
          typeof message.chat_id === "undefined" ||
          message.chat_id === null
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the chat ID is not present",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
      // Check if the chat ID is valid
      if (properties.chat_id >= state.PROPERTY_VALID) {
        if (!state.tests[message.test_id].chats[message.chat_id]) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the chat ID is not valid",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
    }

    // Check for sender ID
    if ("sender_id" in properties) {
      // Check if the sender ID exists
      if (properties.sender_id >= state.PROPERTY_PRESENT) {
        if (
          typeof message.sender_id === "undefined" ||
          message.sender_id === null
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the sender ID is not present",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
      // Check if the sender ID is valid
      if (properties.sender_id >= state.PROPERTY_VALID) {
        // The sender ID is valid if it's your ID, or in the
        // people section of the test data
        if (
          message.sender_id != rootState.app.current_user.user_id &&
          !state.tests[message.test_id].chats[message.chat_id].people[
            message.sender_id
          ]
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the sender ID is not valid",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
    }

    // Check for user ID
    if ("user_id" in properties) {
      // Check if the user ID exists
      if (properties.user_id >= state.PROPERTY_PRESENT) {
        if (
          typeof message.user_id === "undefined" ||
          message.user_id === null
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the user ID is not present",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
      // Check if the user ID is valid
      if (properties.user_id >= state.PROPERTY_VALID) {
        // The user ID is valid if it's your ID, or string NULL, or in the
        // people section of the test data
        if (
          message.user_id != rootState.app.current_user.user_id &&
          message.user_id != "NULL" &&
          !state.tests[message.test_id].chats[message.chat_id].people[
            message.user_id
          ]
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the user ID is not valid",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
    }

    // Check for message ID
    if ("message_id" in properties) {
      // Check if the message ID exists
      if (properties.message_id >= state.PROPERTY_PRESENT) {
        if (
          typeof message.message_id === "undefined" ||
          message.message_id === null
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the message ID is not present",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
      // Check if the message ID is valid
      if (properties.message_id >= state.PROPERTY_VALID) {
        if (
          !state.tests[message.test_id].chats[message.chat_id].messages[
            message.sender_id + ":" + message.message_id
          ]
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the message ID is not valid",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
    }

    // Check for timestamp
    if ("timestamp" in properties) {
      // Check if the timestamp exists
      if (properties.timestamp >= state.PROPERTY_PRESENT) {
        if (
          typeof message.timestamp === "undefined" ||
          message.timestamp === null
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the timestamp is not present",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
      // Check if the timestamp is valid
      if (properties.timestamp >= state.PROPERTY_VALID) {
        if (new Date(message.timestamp).getTime() > 0) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the timestamp is not valid",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
    }

    // Check for content
    if ("content" in properties) {
      // Check if the content exists
      if (properties.content >= state.PROPERTY_PRESENT) {
        if (
          typeof message.content === "undefined" ||
          message.content === null
        ) {
          dispatch(
            "throwError",
            {
              message:
                "Error on received message: received a message where the content is not present",
              show: false,
              extra: message,
            },
            { root: true }
          );
          return false;
        }
      }
      // no check for content valid implemented
    }

    return true;
  },
};

const mutations = {
  setActiveTestId(state, test_id) {
    state.active_test_id = parseInt(test_id);
  },
  setActiveChatById(state, { chat_id }) {
    state.active_chat = state.tests[state.active_test_id].chats[chat_id];
  },
  setActiveChat(state, chat) {
    state.active_chat = chat;
  },

  setTest(state, { test_id, test }) {
    Vue.set(state.tests, test_id, test);
  },
  setTestName(state, { test_id, name }) {
    Vue.set(state.tests[test_id], "name", name);
  },
  setTestChats(state, { test_id, chats }) {
    Vue.set(state.tests[test_id], "chats", chats);
  },
  setUsers(state, users) {
    state.users = users;
  },

  setChatUnread(state, { chat, number_of_unread }) {
    Vue.set(chat, "unread", number_of_unread);
  },
  setChatAssigned(state, { chat, name }) {
    Vue.set(chat, "assigned", name);
  },
  incrementChatUnread(state, { chat }) {
    if (chat.number_of_unread) {
      Vue.set(chat, "unread", chat.number_of_unread + 1);
    } else {
      Vue.set(chat, "unread", 1);
    }
  },
  decrementChatUnread(state, { chat }) {
    if (chat.number_of_unread && chat.number_of_unread > 0) {
      Vue.set(chat, "unread", chat.number_of_unread - 1);
    } else {
      Vue.set(chat, "unread", 0);
    }
  },
  addChatTyping(state, { chat, user_id }) {
    if (!chat.typing) {
      Vue.set(chat, "typing", {});
    }
    Vue.set(chat.typing, user_id, true);
  },
  removeChatTyping(state, { chat, user_id }) {
    if (chat.typing && chat.typing[user_id]) {
      Vue.set(chat.typing, user_id, false);
    }
  },
  addChat(state, { chat }) {
    Vue.set(state.tests[chat.test_id].chats, chat.id, chat);
  },
  addChatMessage(state, { message }) {
    Vue.set(
      state.tests[message.test_id].chats[message.chat_id].messages,
      message.sender_id + ":" + message.message_id,
      message
    );
  },
  setLatestChatMessage(state, { message }) {
    Vue.set(
      state.tests[message.test_id].chats[message.chat_id],
      "newest_message",
      message.sender_id + ":" + message.message_id
    );
  },
  setChatMessageError(state, { message, error }) {
    Vue.set(
      state.tests[message.test_id].chats[message.chat_id].messages[
        message.sender_id + ":" + message.message_id
      ],
      "error",
      error
    );
  },
  removeChatMessage(state, { message }) {
    Vue.set(message, "visible", false);
    Vue.set(message, "content", "This message has been deleted");
  },
  addUsers(state, { users }) {
    state.users = { ...state.users, ...users };
  },
  setLatestAnnouncementMessage(state, { message }) {
    if (!state.tests[message.test_id].latest_announcement) {
      Vue.set(state.tests[message.test_id], "latest_annuncement", {
        show: false,
        message,
      });
    } else {
      Vue.set(
        state.tests[message.test_id].latest_announcement,
        "message",
        message
      );
    }
  },
  setLatestAnnouncementShow(state, { test_id, show }) {
    // Vue.set(state.latest_announcement, "show", show);
    Vue.set(state.tests[test_id].latest_announcement, "show", show);
  },
};

export default {
  state,
  getters,
  actions,
  mutations,
};
