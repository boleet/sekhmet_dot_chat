<template>
  <div>
    <template
      v-if="
        !is_spectator &&
          active_chat != null &&
          (active_chat.name != CHAT_ANNOUNCEMENTS_NAME || is_supervisor)
      "
    >
      <p class="mb-0" id="users_typing">
        <small>{{ users_typing }}</small>
      </p>
      <b-form @submit.prevent="onSendMessage(active_chat)">
        <b-input-group>
          <b-form-textarea
            id="chat-message"
            v-model="message"
            placeholder="Enter message..."
            :state="message_valid"
            @keydown.enter.exact.prevent
            @keyup.enter.exact="onSendMessage(active_chat)"
            @keydown.enter.shift.exact="addNewlineToMessage"
          ></b-form-textarea>
          <b-button
            type="submit"
            variant="primary"
            class="ml-2"
            :disabled="message.length > message_max_length"
          >
            Send
          </b-button>
          <b-form-invalid-feedback :state="message_valid">
            Your message cannot be this large, please split it up and send in
            seperate messages...
          </b-form-invalid-feedback>
        </b-input-group>
      </b-form>
    </template>
  </div>
</template>

<style scoped>
#users_typing {
  min-height: 1.5em;
}
</style>

<script>
import { mapState, mapMutations, mapActions, mapGetters } from "vuex";
import _ from "lodash";

export default {
  name: "ChatInput",
  data() {
    return {
      message: "",
      temp_chat: null,
      typing: false,
      message_max_length: 7500,
    };
  },
  computed: {
    ...mapGetters(["active_test", "is_supervisor", "is_spectator"]),
    ...mapState({
      active_chat: (state) => state.test.active_chat,
      current_user_id: (state) => state.app.current_user.user_id,
      CHAT_ANNOUNCEMENTS_NAME: (state) => state.test.CHAT_ANNOUNCEMENTS_NAME,
      CHAT_GROUP_NAME: (state) => state.test.CHAT_GROUP_NAME,
    }),
    // Display the list of users typing in the chat
    users_typing: function() {
      if (this.active_chat.typing) {
        let typing_users = [];
        for (const [user, typing] of Object.entries(this.active_chat.typing)) {
          if (typing && this.active_chat.people[user]) {
            let name = this.active_chat.people[user].name;
            // Change names to readable format
            if (name.indexOf("(") > -1) {
              name = name.substr(0, name.indexOf("("));
            } else if (name.indexOf(" ") > -1) {
              name = name.substr(0, name.indexOf(" "));
            }
            typing_users.push(name);
          }
        }
        if (typing_users.length !== 0) {
          return "People typing: " + typing_users.join(", ");
        } else {
          return null;
        }
      }
      return null;
    },
    // Check if input message is valid, for input validation
    message_valid: function() {
      if (this.message.length >= this.message_max_length) {
        return false;
      }
      return null;
    },
  },
  methods: {
    ...mapMutations([
      "addChatMessage",
      "setCurrentUser",
      "setChatAssigned",
      "setLatestChatMessage",
      "setLatestAnnouncementMessage",
      "setLatestAnnouncementShow",
    ]),
    ...mapActions(["sendWsMessage"]),
    addNewlineToMessage() {
      this.messagevalue = `${this.message}\n`;
    },
    // Send chat message to recipients
    onSendMessage(active_chat) {
      // Make sure active_chat is not reactive
      active_chat = JSON.parse(JSON.stringify(active_chat));

      // Don't send empty or too big messages
      if (
        this.message.length !== 0 &&
        this.message.length < this.message_max_length
      ) {
        let newMessageId = Object.keys(active_chat.messages).length;
        let message = {
          message_id: "" + newMessageId,
          sender_id: this.current_user_id,
          test_id: active_chat.test_id,
          chat_id: active_chat.chat_id,
          timestamp: Date.now(),
          message_index: 0,
          is_final: true,
          content: this.message,
          visible: true,
        };

        let people = Object.values(active_chat.people);

        let receivers = [];
        for (const recipient in people) {
          receivers.push("" + people[recipient].user_id);
        }
        let msg_wrapped = {
          messageType: "message_final",
          receiverIds: receivers,
          message: message,
        };
        // Send message over websocket
        this.sendWsMessage({
          message: msg_wrapped,
        });

        // Add message to local database
        this.addChatMessage({ message });
        this.setLatestChatMessage({ message });

        // If it's an announcement, show banner to yourself as well
        if (active_chat.name === this.CHAT_ANNOUNCEMENTS_NAME) {
          this.setLatestAnnouncementMessage({ message });
          this.setLatestAnnouncementShow({
            test_id: message.test_id,
            show: true,
          });
        }

        // Reset the message input field
        this.message = "";
      }
    },
    // Assign the active chat to yourself
    assignChatToMe() {
      // save assignment locally
      this.setChatAssigned({
        chat: this.active_chat,
        name: "you",
      });

      let people = Object.values(this.active_chat.people);

      // Add all other supervisors to the receivers list
      let receivers = [];
      for (const recipient in people) {
        if (
          this.active_test.people[people[recipient].user_id] &&
          this.active_test.people[people[recipient].user_id].is_supervisor ===
            true
        ) {
          receivers.push("" + people[recipient].user_id);
        }
      }

      let msg_wrapped = {
        messageType: "conversation_assigned",
        receiverIds: receivers,
        message: {
          test_id: this.active_test.test_id,
          chat_id: this.active_chat.chat_id,
          user_id: this.current_user_id,
        },
      };
      // Send message over websocket
      this.sendWsMessage({
        message: msg_wrapped,
      });
    },
    // Debounced function for the user typing
    debounceUserStopTyping: _.debounce(function() {
      // The user only stopped typing if the message box is empty,
      // otherwise it's just a pause
      if (this.message.length === 0) {
        // stopped typing and messagebox is empty
        this.typing = false;

        let message = {
          test_id: this.active_test.test_id,
          chat_id: this.active_chat.chat_id,
          user_id: this.current_user_id,
          typing: false,
        };

        let people = Object.values(this.active_chat.people);

        // Add other supervisors to receivers list
        let receivers = [];
        for (const recipient in people) {
          if (
            this.active_test.people[people[recipient].user_id] &&
            this.active_test.people[people[recipient].user_id].is_supervisor ===
              true
          ) {
            receivers.push("" + people[recipient].user_id);
          }
        }
        let msg_wrapped = {
          messageType: "conversation_typing",
          receiverIds: receivers,
          message: message,
        };
        // Send message over websocket
        this.sendWsMessage({
          message: msg_wrapped,
        });
      }
    }, 800),
    // Update other supervisors if I started typing in the chat
    userIsTyping() {
      if (!this.typing && this.message.length !== 0) {
        this.typing = true;

        let message = {
          test_id: this.active_test.test_id,
          chat_id: this.active_chat.chat_id,
          user_id: this.current_user_id,
          typing: true,
        };

        let people = Object.values(this.active_chat.people);

        let receivers = [];
        for (const recipient in people) {
          if (
            this.active_test.people[people[recipient].user_id] &&
            this.active_test.people[people[recipient].user_id].is_supervisor ===
              true
          ) {
            receivers.push("" + people[recipient].user_id);
          }
        }
        let msg_wrapped = {
          messageType: "conversation_typing",
          receiverIds: receivers,
          message: message,
        };
        // Send message over websocket
        this.sendWsMessage({
          message: msg_wrapped,
        });
      }
    },
  },
  watch: {
    message: function() {
      // If needed, assign chat to this teacher
      if (this.is_supervisor) {
        // Update typing notifications
        this.userIsTyping();
        this.debounceUserStopTyping();
        // Assign the chat
        if (
          this.active_chat.name != this.CHAT_ANNOUNCEMENTS_NAME &&
          this.active_chat.name != this.CHAT_GROUP_NAME &&
          !this.active_chat.assigned
        ) {
          this.assignChatToMe();
        }
      }
    },
    // If switching between chats, store typed message temporary and load
    // the one of the opened chat
    "active_chat.chat_id": function() {
      if (this.temp_chat != this.active_chat.chat_id) {
        // store the temporary message to the chat
        if (this.active_test.chats[this.temp_chat]) {
          this.active_test.chats[this.temp_chat].temp_message = this.message;
        }
        // reset the temporary message
        this.message = "";

        // restore temp message of new active chat
        if (this.active_chat.temp_message) {
          this.message = JSON.parse(
            JSON.stringify(this.active_chat.temp_message)
          );
        }
        this.temp_chat = this.active_chat.chat_id;
      }
    },
  },
};
</script>
