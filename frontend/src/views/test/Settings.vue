<template>
  <div class="container">
    <h1>
      Settings
      <small class="text-muted" v-if="active_test">{{
        active_test.name
      }}</small>
    </h1>
    <p>In here you can change settings for this specific test.</p>
    <div class="d-flex justify-content-center mt-2" v-if="!active_test">
      <h4 class="mr-3">Loading test</h4>
      <b-spinner label="Loading..."></b-spinner>
    </div>
    <div v-if="active_test">
      <!-- chat change name buttons -->
      <div
        v-if="
          active_test && active_test.module_coordinator == current_user.user_id
        "
      >
        <b-form @submit="onSubmit">
          <b-form-group
            id="input-group-name"
            label="Name:"
            label-for="input-name"
          >
            <b-form-input
              id="input-name"
              v-model="form.name"
              type="text"
              placeholder="Enter test name"
              required
            ></b-form-input>
          </b-form-group>
          <b-button type="submit" variant="info">
            Save
            <b-spinner
              small
              label="Loading..."
              v-if="loading.save_test"
            ></b-spinner
          ></b-button>
        </b-form>
        <hr />
      </div>
      <!-- test stop start buttons -->
      <div>
        <b-button
          block
          v-if="!test_is_open"
          variant="success"
          @click="onStartTest"
          :disabled="loading.start_test"
        >
          Start test
          <b-spinner small v-if="loading.start_test"></b-spinner
        ></b-button>
        <b-button
          block
          v-if="test_is_open"
          variant="danger"
          class="ml-2"
          @click="onStopTest"
          :disabled="loading.stop_test"
        >
          Stop test
          <b-spinner small v-if="loading.stop_test"></b-spinner
        ></b-button>
        <span v-if="loading.start_test && loading.stop_test" class="font-italic"
          >This might take a minute...</span
        >
        <br />
        <hr />
      </div>
      <!-- chat backlog buttons -->
      <div
        v-if="
          active_test && active_test.module_coordinator == current_user.user_id
        "
      >
        <b-button :href="backlog_path()" class="my-2"
          >Download chat backlog</b-button
        ><br />
        <b-button
          @click="onDeleteBacklog"
          variant="danger"
          :disabled="test_is_open || loading.delete_backlog"
          >Delete complete test log
          <b-spinner small v-if="loading.delete_backlog"></b-spinner>
        </b-button>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapMutations, mapState, mapActions } from "vuex";
export default {
  name: "TestSettings",
  data() {
    return {
      initial_form: null,
      form: {
        test_id: null,
        name: "",
      },
      loading: {
        start_test: false,
        stop_test: false,
        save_test: false,
        delete_backlog: false,
      },
    };
  },
  computed: {
    ...mapGetters(["active_test"]),
    ...mapState({
      current_user: (state) => state.app.current_user,
    }),
    // Determine if the active test is open
    test_is_open: function() {
      return (
        this.active_test &&
        this.active_test.start_time !== null &&
        this.active_test.end_time === null
      );
    },
  },
  methods: {
    ...mapMutations(["setTestName"]),
    ...mapActions(["throwError", "loadTest"]),
    // Return the path to the backlog
    backlog_path() {
      return "/api/tests/" + this.$route.params.id + "/backlog";
    },
    // Update the test information
    onSubmit(event) {
      event.preventDefault();
      this.loading.save_test = true;
      let changedData = this.formFieldsChanged();
      if (Object.keys(changedData).length !== 0) {
        this.$http
          .put("/tests/" + this.$route.params.id, this.form)
          .then(() => {
            this.loading.save_test = false;
            // If the name is changed, directly show the new test name locally
            // so there is not need to reload the whole endpoint
            if ("name" in changedData) {
              this.setTestName({
                test_id: this.form.test_id,
                name: changedData["name"],
              });
            }
          })
          .catch((error) => {
            this.loading.save_test = false;
            this.throwError({
              message: "Error while updating test",
              error,
              extra: {
                test_id: this.$route.params.id,
              },
            });
          });
      } else {
        this.loading.save_test = false;
      }
    },
    // Determine which fields changed compared to the initial form
    formFieldsChanged() {
      let k = Object.keys(this.form).filter(
        (key) => this.form[key] !== this.initial_form[key]
      );
      return k.reduce((obj, key) => {
        obj[key] = this.form[key];
        return obj;
      }, {});
    },
    // Start the test
    onStartTest() {
      this.loading.start_test = true;
      // If the test is closed, but has started before, add reopen to the request
      let reopen = "";
      if (
        this.active_test &&
        this.active_test.start_time !== null &&
        this.active_test.end_time !== null
      ) {
        reopen = "?reopen=true";
      }
      this.$http
        .put("/tests/" + this.$route.params.id + "/open" + reopen)
        .then(() => {
          this.loading.start_test = false;
          if (this.active_test) {
            this.active_test.end_time = null;
          }
          // Reload the test endpoint when the test is started
          this.loadTest({ test_id: this.$route.params.id });
        })
        .catch((error) => {
          this.loading.start_test = false;
          this.throwError({
            message: "Error while starting test",
            error,
            extra: {
              test_id: this.$route.params.id,
            },
          });
        });
    },
    // Stop the test, but show a confirmation box first
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
                  this.active_test.end_time = 0; // set to not null for now, such that locally we see the test is closed
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
    // Delete the test backlog, but ask for confirmation first
    onDeleteBacklog() {
      this.loading.delete_backlog = true;
      this.$bvModal
        .msgBoxConfirm(
          "Are you sure you want to delete the complete test backlog?",
          {
            title: "Please Confirm",
            size: "sm",
            buttonSize: "sm",
            okVariant: "danger",
            okTitle: "YES",
            cancelTitle: "NO",
            footerClass: "p-2",
            hideHeaderClose: false,
            centered: true,
          }
        )
        .then((value) => {
          if (value) {
            this.loading.stop_test = false;
            this.$http
              .delete("/tests/" + this.$route.params.id + "/backlog")
              .then(() => {
                this.loading.delete_backlog = false;
              })
              .catch((error) => {
                this.loading.delete_backlog = false;
                this.throwError({
                  message: "Error while deleting backlog",
                  error,
                  extra: {
                    test_id: this.$route.params.id,
                  },
                });
              });
          } else {
            this.loading.delete_backlog = false;
          }
        })
        .catch(() => {
          this.loading.delete_backlog = false;
        });
    },
  },
  created: function() {},
  watch: {
    // Once the active test is available, set initial fields
    active_test: function(newVal) {
      this.form.name = newVal.name;
      this.form.test_id = newVal.test_id;
      this.initial_form = JSON.parse(JSON.stringify(this.form));
    },
  },

  mounted: function() {
    // If active test is available, set initial fields
    if (this.active_test) {
      this.form.name = this.active_test.name;
      this.form.test_id = this.active_test.test_id;
      this.initial_form = JSON.parse(JSON.stringify(this.form));
    }
  },
};
</script>

<style></style>
