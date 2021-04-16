<template>
  <b-container>
    <b-alert variant="info" :show="is_spectator === true">
      <h4 class="alert-heading">Spectator mode</h4>
      <p>
        You're not enrolled in this test. As a module coordinator or system
        admin you can still see existing messages, but sending or receiving new
        messages is not possible. For an updated state of the test, please
        reload the page.
      </p>
    </b-alert>
    <h1>
      Test chat
      <small class="text-muted" v-if="active_test">{{
        active_test.name
      }}</small>
      <b-button
        variant="success"
        v-if="active_test && !test_is_open && is_supervisor"
        class="ml-2"
        @click="onStartTest()"
      >
        Start test
        <span class="font-italic" v-if="loading.start_test"
          >(this might take a minute)</span
        >
        <b-spinner small v-if="loading.start_test"></b-spinner
      ></b-button>
      <b-button
        variant="danger"
        v-if="test_is_open && is_supervisor"
        class="ml-2"
        @click="onStopTest()"
      >
        Stop test
        <span class="font-italic" v-if="loading.stop_test"
          >(this might take a minute)</span
        >
        <b-spinner small v-if="loading.stop_test"></b-spinner
      ></b-button>
    </h1>
    <div class="d-flex justify-content-center mt-2" v-if="!active_test">
      <h4 class="mr-3">Loading test</h4>
      <b-spinner label="Loading..."></b-spinner>
    </div>
    <b-alert
      dismissible
      variant="success"
      :show="latest_announcement && latest_announcement.show"
      @dismissed="hideLatestAnnouncement()"
    >
      <h4 class="alert-heading">New announcement!</h4>
      <p
        class="announcement_value"
        v-if="latest_announcement && latest_announcement.message"
      >
        {{ latest_announcement.message.content }}
      </p>
    </b-alert>
    <b-row v-if="active_test">
      <b-col cols="4">
        <ChatList />
      </b-col>
      <b-col>
        <MessageList />
        <ChatInput />
      </b-col>
    </b-row>
  </b-container>
</template>

<style scoped>
.announcement_value {
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
  white-space: pre-line;
}
</style>

<script>
import { mapState, mapMutations, mapGetters, mapActions } from "vuex";
import ChatList from "./ChatList.vue";
import ChatInput from "./ChatInput.vue";
import MessageList from "./MessageList.vue";

export default {
  name: "TestChat",
  data() {
    return {
      loading: {
        test: true,
        start_test: false,
        stop_test: false,
      },
    };
  },
  components: { ChatList, ChatInput, MessageList },
  computed: {
    ...mapState({
      latest_announcement: (state) => {
        if (state.test.tests[state.test.active_test_id]) {
          return state.test.tests[state.test.active_test_id]
            .latest_announcement;
        }
        return null;
      },
      tests: (state) => state.test.tests,
    }),
    ...mapGetters(["active_test", "is_spectator", "is_supervisor"]),
    // Return if a test is open
    test_is_open: function() {
      return (
        this.active_test &&
        this.active_test.start_time !== null &&
        this.active_test.end_time === null
      );
    },
  },
  methods: {
    ...mapMutations(["setLatestAnnouncementShow", "decrementChatUnread"]),
    ...mapActions(["loadTest"]),
    // Hide the latest announcement banner
    hideLatestAnnouncement() {
      let test = this.tests[this.latest_announcement.message.test_id];
      if (test) {
        let chat = test.chats[this.latest_announcement.message.chat_id];
        if (chat) {
          this.decrementChatUnread({ chat });
        }
      }
      this.setLatestAnnouncementShow({
        test_id: this.latest_announcement.message.test_id,
        show: false,
      });
    },
    // Open or reopen a test
    onStartTest() {
      this.loading.start_test = true;
      let reopen = "";
      // If test has been opened before, add reopen to the request
      if (
        this.active_test &&
        this.active_test.start_time !== null &&
        this.active_test.end_time !== null
      ) {
        reopen = "?reopen=true";
      }
      let test_id = this.$route.params.id;
      this.$http
        .put("/tests/" + test_id + "/open" + reopen)
        .then(() => {
          this.loading.start_test = false;
          if (this.active_test) {
            this.active_test.end_time = null;
          }
          this.loadTest({ test_id });
        })
        .catch((error) => {
          this.loading.start_test = false;
          this.throwError({
            message: "Error while starting test",
            error,
            extra: {
              test_id,
            },
          });
        });
    },
    // Close the test, but ask for a confirmation first
    onStopTest() {
      this.loading.stop_test = true;
      this.$bvModal
        .msgBoxConfirm("Are you sure you want to stop this test?", {
          title: "Please Confirm",
          size: "sm",
          buttonSize: "sm",
          okVariant: "danger",
          okTitle: "YES",
          cancelTitle: "NO",
          footerClass: "p-2",
          hideHeaderClose: false,
          centered: true,
        })
        .then((value) => {
          if (value) {
            this.loading.stop_test = false;
            this.$http
              .put("/tests/" + this.$route.params.id + "/close")
              .then(() => {
                this.loading.stop_test = false;
                if (this.active_test) {
                  this.active_test.end_time = 0; // set to not null for now
                }
              })
              .catch((error) => {
                this.loading.stop_test = false;
                this.throwError({
                  message: "Error while stopping test",
                  error,
                  extra: {
                    test_id: this.$route.params.id,
                  },
                });
              });
          } else {
            this.loading.stop_test = false;
          }
        })
        .catch(() => {
          this.loading.stop_test = false;
        });
    },
  },
};
</script>

<style></style>
