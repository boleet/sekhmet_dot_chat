<template>
  <div>
    <router-view />
  </div>
</template>

<script>
import { mapMutations, mapActions, mapState } from "vuex";
import * as websocket from "../../scripts/websocket.js";

export default {
  name: "Test",
  data() {
    return {
      // startedWebsocket = false,
    };
  },
  methods: {
    ...mapMutations([
      "setActiveTestId",
      "setUserConnectionById",
      "setUserChannel",
    ]),
    ...mapActions([
      "loadTest",
      "loadUsers",
      "receivedWsMessage",
      "receivedChatMessage",
    ]),
    // Start the websocket and attach the received message handler to it
    initWebSocket() {
      websocket.startSocket(this.closeSocketCallback);
      websocket.addReceiveSwitch(this.receivedWsMessage);
    },
    // Create a function that can be called once the websocket is closed
    closeSocketCallback() {
      this.loadTest({ test_id: this.active_test_id });
    },
  },
  computed: {
    ...mapState({
      current_user: (state) => state.app.current_user,
      active_test_id: (state) => state.test.active_test_id,
      tests: (state) => state.test.tests,
    }),
  },
  watch: {
    // If the id in the URL changed, change the active test
    $route: {
      immediate: true,
      handler(newRoute) {
        this.setActiveTestId(newRoute.params.id);
      },
    },
    // If the current user changes, create a new websocket.
    // "current_user.user_id": function(newVal, oldVal) {
    //   // if this page is the first one loaded, first wait until /users/me has a resposne
    //   if (newVal != oldVal && !this.startedWebsocket) {
    //     this.initWebSocket();
    //   }
    // },
  },
  mounted() {
    // Load the active test and initialise a websocket
    this.loadTest({ test_id: this.active_test_id });
    setTimeout(() => {
      this.initWebSocket();
    }, 100);
  },
};
</script>
