<template>
  <div class="container">
    <h1>
      Settings
      <small class="text-muted" v-if="course.name">{{ course.name }}</small>
    </h1>
    <p>In here you can change settings for this specific course.</p>

    <b-button
      :to="{ name: 'test-import', params: { id: $route.params.id } }"
      class="my-1"
      variant="info"
      >Import new test</b-button
    ><br />

    <hr />
    <b-form @submit="onSubmit">
      <b-form-group id="input-group-name" label="Name:" label-for="input-name">
        <b-form-input
          id="input-name"
          v-model="form.name"
          type="text"
          placeholder="Enter test name"
          required
        ></b-form-input>
      </b-form-group>
      <b-row class="mt-4">
        <b-col>
          Module coordinator:
          <b-card class="p-0" bg-variant="light">
            {{ form.coordinator.name }}<br />
            <small
              >{{ form.coordinator.id }}<br />
              {{ form.coordinator.email }}</small
            >
          </b-card>
          <div v-if="form.coordinator.id != course.coordinator.id">
            <small class="text-warning"
              >If you change the coordinator, you loose access to this
              module.</small
            >
          </div>
          <b-button type="submit" variant="info" class="mt-3">Save</b-button>
          <p class="mb-0" id="users_typing">
            <small>{{ courseSettingsFeedback }}</small>
          </p>
        </b-col>
        <b-col>
          Search new coordinator
          <b-form-input
            id="input-coordinator"
            type="text"
            placeholder="Search on m-number, name or email"
            list="user-search-list"
            v-model="form.search"
            debounce="500"
          ></b-form-input>

          <div class="d-flex justify-content-center mt-2" v-if="loadingSearch">
            <b-spinner label="Loading..."></b-spinner>
          </div>
          <b-list-group class="mt-2 mb-4" v-else>
            <b-list-group-item
              v-for="user in searchResults"
              :key="user.id"
              @click="selectCoordinator(user)"
              href="#"
            >
              {{ user.name }}
              <small>- {{ user.id }}, {{ user.email }}</small>
            </b-list-group-item>
          </b-list-group>
        </b-col>
      </b-row>
    </b-form>
  </div>
</template>

<script>
import { mapState, mapActions, mapMutations } from "vuex";
export default {
  name: "CourseSettings",
  data() {
    return {
      course: {
        name: null,
        coordinator: {
          id: null,
        },
      },
      initial_form: null,
      form: {
        search: null,
        name: "",
        coordinator: {
          id: null,
          name: null,
          email: null,
        },
      },
      searchResults: [],
      loadingSearch: false,
      courseSettingsFeedback: "",
    };
  },
  methods: {
    ...mapActions(["throwError"]),
    ...mapMutations(["setTestName"]),
    // Handle the course info form update request
    onSubmit(event) {
      this.courseSettingsFeedback = "Saving...";
      event.preventDefault();
      let changedData = this.formFieldsChanged();
      delete changedData.search;
      if (changedData.length !== 0) {
        let putData = changedData;
        if (changedData.coordinator) {
          putData.module_coordinator = putData.coordinator.user_id;
          delete putData.coordinator;
        }
        this.$http
          .put("/courses/" + this.$route.params.id, putData)
          .then(() => {
            this.courseSettingsFeedback = "Saved changes!";
          })
          .catch((error) => {
            this.courseSettingsFeedback = "Something went wrong...";
            this.throwError({
              message: "Error while updating course",
              error,
              extra: {
                course_id: this.$route.params.id,
                putData,
              },
            });
          });
      }
    },
    // Check which fields in the form changed, thus are different from the initial state
    formFieldsChanged() {
      let k = Object.keys(this.form).filter(
        (key) => this.form[key] !== this.initial_form[key]
      );
      return k.reduce((obj, key) => {
        obj[key] = this.form[key];
        return obj;
      }, {});
    },
    // Retrieve the course information of the current course
    getCourse() {
      var course_id = this.$route.params.id;
      this.$http
        .get("/courses/" + course_id, this.form)
        .then((response) => {
          if (response.data[course_id]) {
            let data = response.data[course_id];
            this.form.name = data.name;
            this.form.coordinator = data.module_coordinator;
            this.initial_form = JSON.parse(JSON.stringify(this.form));
          } else {
            this.throwError({
              message:
                "Error while getting course info: couldn't extract data from json",
              extra: { course_id },
            });
          }
        })
        .catch((error) => {
          this.throwError({
            message: "Error while getting course info",
            error,
            extra: { course_id },
          });
        });
    },
    // Search for a user and display
    searchUser(val) {
      this.loadingSearch = true;

      this.$http
        .get("/users/?q=" + val, this.form)
        .then((response) => {
          this.loadingSearch = false;
          if (response.data.users) {
            this.searchResults = response.data.users;
          } else {
            this.throwError({
              message:
                "Error while getting users: cannot extract data from response",
              extra: { response },
            });
          }
        })
        .catch((error) => {
          this.loadingSearch = false;
          this.throwError({
            message: "Error while getting users: ",
            error,
            extra: {
              searchQ: val,
            },
          });
        });
    },
    // Set the coordinator in the form
    selectCoordinator(user) {
      this.form.coordinator = user;
      this.form.search = null;
      this.searchResults = [];
    },
  },
  created: function() {
    this.getCourse();
  },
  watch: {
    // If typed more than 4 charcters in the search field, auto search
    "form.search": function(newVal) {
      if (!newVal || newVal.length < 4) {
        return null;
      }
      this.searchUser(newVal);
    },
  },
  computed: {
    ...mapState({
      current_user: (state) => state.app.current_user,
    }),
  },
};
</script>

<style></style>
