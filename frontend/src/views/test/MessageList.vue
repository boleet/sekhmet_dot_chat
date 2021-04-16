<template>
  <div>
    <b-modal
      id="edit-message-modal"
      ref="edit-message-modal"
      title="Edit chat message"
      @ok="modalEditMessageOk"
    >
      <form ref="form" @submit.stop.prevent="handleSubmit">
        <b-form-group
          label="Message"
          label-for="message-input"
          invalid-feedback="Message may not be empty"
          :state="message_content_new_state"
        >
          <b-form-textarea
            id="message-input"
            v-model="message_content_new"
            required
            :state="message_content_new_state"
          ></b-form-textarea>
        </b-form-group>
      </form>
    </b-modal>
    <template v-if="active_chat !== null">
      <h4>{{ active_chat.name }}</h4>
      <div class="message_list" id="message_list">
        <div
          class="message received mb-2 clearfix"
          v-for="(message, index) in active_chat_messages"
          :key="index"
          v-bind:class="{
            sent: chat_message_of_ours(message),
            deleted: message.visible === false,
          }"
        >
          <div class="message-inner p-2" :class="{ error: message.error }">
            <p class="mb-0 pre-formatted" v-if="message.visible !== false">
              {{ message.content }}
            </p>
            <p class="mb-0 pre-formatted" v-if="message.visible === false">
              This message has been deleted
            </p>
            <div class="d-flex justify-content-between align-items-center">
              <small class="text-muted"
                >{{ timestamp_display(message) }}
                {{ sender_display(message) }}</small
              >
              <b-dropdown
                class="dropdown-dropright"
                dropright
                variant="link"
                no-caret
                v-if="canUseMessageOptions(message)"
              >
                <template #button-content>
                  <b-icon
                    class="icon-settings"
                    icon="three-dots-vertical"
                  ></b-icon>
                </template>
                <b-dropdown-item
                  v-if="canUseMessageEdit(message)"
                  v-on:click.stop="onEditMessage(message)"
                  href="#"
                  >Edit</b-dropdown-item
                >
                <b-dropdown-item
                  v-if="canUseMessageDelete(message)"
                  v-on:click.stop="onRemoveMessage(message)"
                  href="#"
                  >Remove</b-dropdown-item
                >
              </b-dropdown>
              <b-icon
                class="ml-1"
                icon="exclamation-circle-fill"
                variant="secondary"
                v-if="message.error"
                v-b-popover.hover.top="message.error"
                title="Message error"
              ></b-icon>
            </div>
          </div>
        </div>
        <br />
      </div>
    </template>
    <template v-if="active_chat === null">
      <h4>Welcome!</h4>
      <p>
        If you have any questions during your exam, click on the Questions chat
        at the left. Announcements will be posted to the Announcements chat.
      </p>
    </template>
  </div>
</template>

<style scoped>
.icon-settings {
  width: 15px;
  max-width: 0;
  transition: max-width 0.3s;
}

.pre-formatted {
  white-space: pre-line;
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
}

.message:hover .icon-settings {
  max-width: 15px;
}
.message_list {
  height: 60vh;
  overflow-y: scroll;
}
.message {
  text-align: left;
  width: auto;
  margin-right: 50px;
}
.message.sent {
  text-align: right;
  margin-right: 0px;
  margin-left: 50px;
}
.message.deleted {
  font-style: italic;
  color: grey;
}
.message-inner {
  width: fit-content;
  background-color: rgba(23, 162, 184, 0.1);
}
.message-inner.error {
  background-color: rgba(186, 56, 23, 0.1);
}
.message.sent .message-inner {
  float: right;
}
</style>
<script>
import { mapGetters, mapState, mapActions, mapMutations } from "vuex";
export default {
  name: "MessageList",
  data() {
    return {
      message_content_new: "",
      message_content_new_state: null,
      message_editing: {},
    };
  },
  computed: {
    ...mapGetters(["is_user_id", "active_test", "is_supervisor"]),
    ...mapState({
      active_chat: (state) => state.test.active_chat,
      current_user_id: (state) => state.app.current_user.user_id,
      tests: (state) => state.test.tests,
      CHAT_GROUP_NAME: (state) => state.test.CHAT_GROUP_NAME,
    }),
    active_chat_messages: function() {
      return this.active_chat.messages;
    },
  },
  methods: {
    ...mapActions(["sendWsMessage"]),
    ...mapMutations(["removeChatMessage"]),
    // Check if the chat message is ours, or at least the same side of the conversation
    // i.e. another supervisor in a one-to-one chat
    chat_message_of_ours(message) {
      return (
        this.is_user_id(message.sender_id) ||
        (this.is_supervisor &&
          this.active_test.people[message.sender_id].is_supervisor &&
          this.active_test.chats[message.chat_id].name !== this.CHAT_GROUP_NAME)
      );
    },
    // Scroll the message list to the bottom
    async scrollMessageList() {
      let div_message_list = document.getElementById("message_list");
      if (div_message_list) {
        div_message_list.scrollTop = div_message_list.scrollHeight;
      }
    },
    // On removing a message, update the other people in the conversation
    onRemoveMessage(message) {
      let people = Object.values(this.active_chat.people);

      let receivers = [];
      for (const recipient in people) {
        receivers.push("" + people[recipient].user_id);
      }

      let msg_wrapped = {
        messageType: "message_delete",
        receiverIds: receivers,
        message: {
          test_id: "" + message.test_id,
          chat_id: "" + message.chat_id,
          message_id: "" + message.message_id,
          timestamp: "" + message.timestamp,
          sender_id: "" + message.sender_id,
        },
      };
      // Send message over websocket
      this.sendWsMessage({
        message: msg_wrapped,
      });
      // Remove message locally
      this.removeChatMessage({ message });
    },
    // Show modal to edit a message
    onEditMessage(message) {
      this.message_editing = message;
      this.message_content_new = this.message_editing.content;
      this.$refs["edit-message-modal"].show();
    },
    // Reformat timestamp to hours and minutes for display
    timestamp_display(message) {
      const date = new Date(message.timestamp);
      return (
        date.getHours() +
        ":" +
        (date.getMinutes() < 10 ? "0" : "") +
        date.getMinutes()
      );
    },
    // Reformat sender name for display
    sender_display(message) {
      if (this.is_supervisor) {
        let name = this.active_test.people[message.sender_id].name;
        if (name.indexOf("(") > -1) {
          return " - " + name.substr(0, name.indexOf("("));
        } else if (name.indexOf(" ") > -1) {
          return " - " + name.substr(0, name.indexOf(" "));
        } else {
          return name;
        }
      } else {
        return "";
      }
    },
    // On sending edited message, update other people in conversation
    modalEditMessageOk(bvModalEvt) {
      bvModalEvt.preventDefault();
      // check if the message is different from the original emssage
      if (this.message_content_new) {
        // Update timestamp and message content of local object
        this.message_editing.content = this.message_content_new;
        this.message_editing.timestamp = Date.now();

        let people = Object.values(this.active_chat.people);

        let receivers = [];
        for (const recipient in people) {
          receivers.push("" + people[recipient].user_id);
        }

        let msg_wrapped = {
          messageType: "message_final",
          receiverIds: receivers,
          message: this.message_editing,
        };
        // Send message over websocket
        this.sendWsMessage({
          message: msg_wrapped,
        });
        // Hide the edit modal
        this.$nextTick(() => {
          this.$bvModal.hide("edit-message-modal");
        });
      } else {
        this.message_content_new_state = false;
      }
    },
    // Determine if the current user can use any message option
    canUseMessageOptions(message) {
      return (
        this.canUseMessageDelete(message) || this.canUseMessageEdit(message)
      );
    },
    // Determine if the current user may delete the given message
    canUseMessageDelete(message) {
      if (message.visible === false) {
        return false;
      }
      if (!this.active_test) {
        return false;
      }
      // If supervisor, may delete every message
      if (this.is_supervisor) {
        return true;
      }
      return false;
    },
    // Determine if the current user may edit the given message
    canUseMessageEdit(message) {
      if (message.visible === false) {
        return false;
      }
      if (!this.active_test) {
        return false;
      }
      // you  may edit your own message
      if (message.sender_id == this.current_user_id) {
        return true;
      }
      return false;
    },
  },
  watch: {
    // If a new message arrives, scroll message list to bottom
    "active_chat.newest_message": function() {
      // Delay to allow adding message to the view before calculating
      // new scroll height
      setTimeout(() => {
        this.scrollMessageList();
      }, 200);
    },
  },
};
</script>
