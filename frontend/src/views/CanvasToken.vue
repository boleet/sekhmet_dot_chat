<template>
  <div class="container">
    <h1>
      Canvas API token settings
    </h1>
    <p>
      Go to <a href="https://canvas.utwente.nl" target="_blank">Canvas</a>, log
      in and go to <i>Account</i> > <i>Settings</i>. Scroll to
      <i>Approved integrations</i>, and click the pink button
      <i>New access token</i>. Follow the instructions and copy the token. Paste
      it here and save. You can close Canvas.
    </p>
    <p v-if="has_canvas_token">
      You currently have a token set. It's not shown because of security. If you
      save a new token, the old one will be overwritten.
    </p>
    <p v-else>You currently have no token set.</p>
    <b-form @submit="onSubmit">
      <b-form-group
        id="input-group-name"
        label="Canvas API token:"
        label-for="input-name"
      >
        <b-form-input
          id="input-name"
          v-model="canvas_token"
          type="text"
          placeholder="Paste your API token"
          required
        ></b-form-input>
      </b-form-group>
      <b-button type="submit" variant="info" class="mt-3">Save</b-button>
    </b-form>
  </div>
</template>

<style scoped>
.dropdown-dropleft button svg {
  color: #fff;
}
</style>

<script>
import { mapState, mapActions } from "vuex";

export default {
  name: "Courses",
  components: {},
  data() {
    return {
      module_fields: ["name", "open"],
      courses: {},
      has_canvas_token: false,
      canvas_token: null,
    };
  },
  methods: {
    ...mapActions(["throwError"]),
    // Load the current user info (based on session token)
    loadUser: function() {
      this.$http
        .get("/users/me")
        .then((response) => {
          if (response.data.has_canvas_token) {
            this.has_canvas_token = response.data.has_canvas_token;
          }
        })
        .catch((error) => {
          this.throwError({
            message: "Error while loading current user data",
            error,
          });
        });
    },
    // On submitting the form, save the data
    onSubmit(event) {
      event.preventDefault();
      this.saveUser();
    },
    // Save the canvas token of the current user
    saveUser: function() {
      this.$http
        .put("/users/me", { canvas_token: this.canvas_token })
        .then(() => {
          this.$router.push({ name: "courses" });
        })
        .catch((error) => {
          this.throwError({
            message: "Error while saving canvas token",
            error,
          });
        });
    },
  },
  computed: {
    ...mapState({
      current_user: (state) => state.app.current_user,
    }),
  },
  created: function() {
    this.loadUser();
  },
};
</script>
