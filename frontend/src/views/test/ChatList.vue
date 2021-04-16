<template>
  <b-list-group>
    <h5>Unread chats: {{ unreadChatsCount }}</h5>
    <div id="chat_list">
      <b-list-group-item
        v-for="chat in orderedChats"
        :key="chat.chat_id"
        class="chat flex-column align-items-start"
        @click="onChangeChat(chat)"
        href="#"
        :variant="
          active_chat && active_chat.chat_id == chat.chat_id ? 'info' : ''
        "
      >
        <div class="d-flex">
          <h5 class="mb-1 chat-tr chat-truncate">{{ chat.name }}</h5>
          <small>? min</small>
        </div>
        <div class="d-flex justify-content-between align-items-center">
          <small class="chat-truncate">{{ lastMessageOfChat(chat) }}</small>
          <b-badge variant="info" class="mr-1" v-if="chat.assigned">{{
            chat.assigned
          }}</b-badge>
          <b-badge
            class="align-self-center"
            v-if="chat.unread && chat.unread !== 0"
            variant="primary"
            pill
            >{{ chat.unread }}
          </b-badge>
          <b-dropdown
            class="dropdown-dropright"
            dropright
            variant="link"
            no-caret
          >
            <template #button-content>
              <b-icon class="icon-settings" icon="three-dots-vertical"></b-icon>
            </template>
            <b-dropdown-item
              v-if="!chat.unread || chat.unread === 0"
              v-on:click.stop="onMarkUnread(chat)"
              href="#"
              >Mark as unread</b-dropdown-item
            >
            <b-dropdown-item
              v-if="chat.unread && chat.unread !== 0"
              v-on:click.stop="onMarkRead(chat)"
              href="#"
              >Mark as read</b-dropdown-item
            >
            <b-dropdown-item
              v-if="chat.assigned === 'you'"
              v-on:click.stop="onRemoveAssignment(chat)"
              href="#"
              >Remove assignment</b-dropdown-item
            >
          </b-dropdown>
        </div>
      </b-list-group-item>
    </div>
  </b-list-group>
</template>
<style>
#chat_list {
  height: 75vh;
  overflow-y: scroll;
}
.dropdown .btn {
  padding: 0 !important;
  color: grey;
}
</style>
<style scoped>
.icon-settings {
  max-width: 0;
  transition: max-width 0.3s;
}
.chat:hover .icon-settings {
  max-width: 15px;
}
.chat-truncate {
  flex: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  margin-right: 10px;
}
</style>
<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
// import _ from "lodash";
export default {
  name: "ChatList",
  computed: {
    ...mapGetters(["active_test"]),
    ...mapState({
      active_chat: (state) => state.test.active_chat,
      CHAT_ANNOUNCEMENTS_NAME: (state) => state.test.CHAT_ANNOUNCEMENTS_NAME,
      CHAT_GROUP_NAME: (state) => state.test.CHAT_GROUP_NAME,
      CHAT_QUESTIONS_NAME: (state) => state.test.CHAT_QUESTIONS_NAME,
    }),
    // Return the chats of this test
    chats: function() {
      if (this.active_test) {
        let chats = this.active_test.chats;
        if (chats.length === 0) {
          this.throwError({
            message:
              "Error while loading chats: there are not chats for this test.",
            extra: {
              active_test: this.active_test,
            },
          });
        }
        return chats;
      } else {
        // no active test
        return [];
      }
    },
    // Return an ordered list of chats, but announcements and teacher chat at top
    orderedChats: function() {
      let chats = Object.values(this.chats);
      // Hide one-to-one chats without any messages
      chats = chats.filter((chat) => {
        return (
          Object.keys(chat.messages).length > 0 ||
          chat.name === this.CHAT_ANNOUNCEMENTS_NAME ||
          chat.name === this.CHAT_GROUP_NAME ||
          chat.name === this.CHAT_QUESTIONS_NAME
        );
      });

      // Sort chats based on timestamp of the newest message
      let list = chats.sort(function(a, b) {
        function dateSorter(a, b) {
          const aHas = typeof a.messages[a.newest_message] !== "undefined";
          const bHas = typeof b.messages[b.newest_message] !== "undefined";
          return (
            bHas - aHas ||
            (aHas === true &&
              new Date(b.messages[b.newest_message].timestamp) -
                new Date(a.messages[a.newest_message].timestamp)) ||
            0
          );
        }
        return a.type - b.type || dateSorter(a, b);
      });
      return list;
    },
    // Count the number of chats with at least one unread message
    unreadChatsCount: function() {
      return Object.values(this.chats).filter((x) => x.unread > 0).length;
    },
  },
  methods: {
    ...mapActions([
      "informOthersChatUnread",
      "updateChatUnread",
      "sendWsMessage",
      "throwError",
    ]),
    ...mapMutations(["setChatUnread", "setActiveChat", "setChatAssigned"]),
    onChangeChat(chat) {
      this.setActiveChat(chat);
      this.updateChatUnread({ chat, number_of_unread: 0 });
    },
    onMarkRead(chat) {
      this.updateChatUnread({ chat, number_of_unread: 0 });
    },
    onMarkUnread(chat) {
      this.updateChatUnread({ chat, number_of_unread: 1 });
    },
    // If manually removed assignment of chat, notify other supervisors
    onRemoveAssignment(chat) {
      this.setChatAssigned({
        chat,
        name: null,
      });

      let people = Object.values(chat.people);

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
        messageType: "conversation_assigned",
        receiverIds: receivers,
        message: {
          test_id: chat.test_id,
          chat_id: chat.chat_id,
          user_id: "NULL",
        },
      };
      // Send message over websocket
      this.sendWsMessage({
        message: msg_wrapped,
      });
    },
    lastMessageOfChat(chat) {
      if (chat.messages[chat.newest_message]) {
        if (chat.messages[chat.newest_message].visible === false) {
          return "This message has been deleted";
        } else {
          return chat.messages[chat.newest_message].content;
        }
      } else {
        return "";
      }
    },
  },
};
</script>
