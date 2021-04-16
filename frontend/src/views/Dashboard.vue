<template>
  <div>
    <!-- navbar and sidebar -->
    <b-navbar toggleable="sm" type="dark" variant="info" fixed="top">
      <b-navbar-brand :to="{ name: 'Dashboard' }">Sekhmet</b-navbar-brand>
      <b-navbar-toggle target="nav-collapse"></b-navbar-toggle>
      <b-collapse id="nav-collapse" is-nav>
        <b-navbar-nav>
          <b-nav-text>{{ name }}</b-nav-text>
        </b-navbar-nav>
        <b-navbar-nav class="d-block d-sm-none">
          <hr />
          <b-nav-item
            :to="{ name: link.path }"
            v-for="link in variableLinksToShow"
            :key="link.path"
            append
            >{{ link.name }}</b-nav-item
          >
          <hr />
        </b-navbar-nav>
        <b-navbar-nav class="ml-auto">
          <b-nav-item
            :href="link.href ? link.href : null"
            :to="!link.href ? { name: link.path } : null"
            v-for="link in constantLinks"
            :key="link.path"
            append
            >{{ link.name }}</b-nav-item
          >
        </b-navbar-nav>
      </b-collapse>
    </b-navbar>
    <div
      class="sidebar bg-info d-none d-sm-block"
      v-if="variableLinksToShow.length > 0"
    >
      <b-nav vertical class="w-25 nav-dark">
        <b-nav-item
          :to="{ name: link.path }"
          v-for="link in variableLinksToShow"
          :key="link.path"
          append
          class="sidebar_item"
          :class="{ sidebar_item_active: link.active }"
          >{{ link.name }}
        </b-nav-item>
      </b-nav>
    </div>
    <!-- content -->
    <div
      class="wrapper"
      :class="{ wrapper_left_space: variableLinksToShow.length > 0 }"
    >
      <router-view />
    </div>

    <b-modal id="error-modal" centered @hidden="unsetError()">
      <template #modal-header>
        <h5 class="p-0 m-0" text-danger>An error occured</h5>
      </template>
      <p class="my-4">{{ error }}</p>
      <template #modal-footer>
        <b-button type="submit" variant="info" @click="unsetError()"
          >Close</b-button
        >
      </template>
    </b-modal>
  </div>
</template>

<style>
.wrapper {
  padding-top: 65px;
}

.sidebar {
  width: 100px;
  height: 100%;
  position: fixed;
  left: 0;
  top: 50px;
}

.sidebar-menu-top {
  height: 100%;
}

.nav-dark .nav-link {
  color: #fff;
}
.sidebar_item {
  border-left: 3px solid rgba(0, 0, 0, 0);
}
.sidebar_item_active {
  border-left: 3px solid white;
}

@media (min-width: 576px) {
  .wrapper_left_space {
    margin-left: 100px;
  }
}
</style>

<script>
import { mapMutations, mapState, mapActions, mapGetters } from "vuex";

export default {
  name: "Dashboard",
  data() {
    return {
      variableLinksToShow: [],
      variableLinks: [
        {
          name: "Chat",
          path: "test-chat",
          whitelist: ["test-chat", "test-people", "test-settings"],
          need_supervisor: true,
          active: false,
        },
        {
          name: "People",
          path: "test-people",
          whitelist: ["test-chat", "test-people", "test-settings"],
          need_supervisor: true,
          active: false,
        },
        {
          name: "Settings",
          path: "test-settings",
          whitelist: ["test-chat", "test-people", "test-settings"],
          need_supervisor: true,
          active: false,
        },
      ],
      constantLinks: [
        {
          name: "Overview",
          path: "courses",
        },
        {
          name: "Logout",
          href: "/login?logout",
        },
      ],
    };
  },
  created: function() {
    this.loadCurrentUser();
  },
  watch: {
    // If an error is set, show the error modal
    // and hide if it is unset
    error: function(newVal) {
      if (newVal != null) {
        // open the modal
        this.$bvModal.show("error-modal");
      } else {
        // close the modal
        this.$bvModal.hide("error-modal");
      }
    },
    // If we go to another page, check which variable links we should show
    $route: {
      immediate: true,
      handler() {
        this.updateVariableLinksToShow();
      },
    },
    // If another test becomes active, check which variable links we should show
    active_test: function() {
      this.updateVariableLinksToShow();
    },
    // If the current user changes, check which variable links we should show
    current_user_id: function() {
      this.updateVariableLinksToShow();
    },
  },
  computed: {
    ...mapState({
      error: (state) => state.app.error,
      current_user_id: (state) => state.app.current_user.user_id,
    }),
    ...mapGetters(["name", "active_test", "is_supervisor", "is_spectator"]),
  },
  mounted: function() {
    // Add CSRF token to every request
    this.$http.defaults.headers.common["X-CSRF-TOKEN"] = document.querySelector(
      'meta[name="_csrf"]'
    ).content;
    // Request desktop notification permission
    this.askDesktopNotificationPermission();
  },
  methods: {
    ...mapMutations(["unsetError", "setCurrentUser"]),
    ...mapActions(["askDesktopNotificationPermission", "throwError"]),
    // Load the current user information
    loadCurrentUser() {
      this.axios
        .get("/users/me")
        .then((response) => {
          let user = response.data;
          user.user_id = "" + user.user_id;
          this.setCurrentUser(user);
        })
        .catch((error) =>
          this.throwError({
            message: "Error while getting current user data",
            error,
          })
        );
    },
    // Determine which variable links to show, based on permission and current page
    updateVariableLinksToShow() {
      this.variableLinksToShow = this.variableLinks.filter((item) => {
        if (item.path === this.$route.name) {
          item.active = true;
        } else {
          item.active = false;
        }
        // TODO
        return (
          item.whitelist.includes(this.$route.name) &&
          ((this.current_user && this.current_user.system_admin) ||
            this.is_spectator ||
            item.need_supervisor !== true ||
            this.is_supervisor)
        );
      });
    },
  },
};
</script>
