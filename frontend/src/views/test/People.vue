<template>
  <div class="container">
    <h1>
      People
      <small class="text-muted" v-if="active_test">{{
        active_test.name
      }}</small>
    </h1>
    <p>In here you can change the people and their permissions.</p>
    <div class="d-flex justify-content-center mt-2" v-if="loading.people">
      <h4 class="mr-3">Loading people</h4>
      <b-spinner label="Loading..."></b-spinner>
    </div>
    <b-container v-if="!loading.people">
      <b-row>
        <b-col>
          <b-list-group flush>
            <h4>Students</h4>
            <div class="people_list">
              <b-list-group-item
                v-for="student in students"
                :key="student.id"
                href="#"
                class="d-flex justify-content-between align-items-center"
                :variant="student.variant"
              >
                {{ student.name }}
                <b-dropdown
                  class="dropdown-dropright"
                  dropright
                  variant="link"
                  no-caret
                  v-if="current_user && current_user.is_employee"
                >
                  <template #button-content>
                    <b-icon
                      class="icon-settings"
                      icon="three-dots-vertical"
                    ></b-icon>
                  </template>
                  <b-dropdown-item
                    v-on:click.stop="onRemoveStudent(student)"
                    href="#"
                    >Remove student</b-dropdown-item
                  >
                </b-dropdown>
              </b-list-group-item>
            </div>
          </b-list-group>
        </b-col>
        <b-col>
          <b-list-group flush>
            <h4>Supervisors</h4>
            <div class="people_list">
              <b-list-group-item
                v-for="supervisor in supervisors"
                :key="supervisor.id"
                href="#"
              >
                <div class="d-flex justify-content-between align-items-center">
                  {{ supervisor.name }}
                  <b-dropdown
                    class="dropdown-dropright"
                    dropright
                    variant="link"
                    no-caret
                    v-if="current_user && current_user.is_employee"
                  >
                    <template #button-content>
                      <b-icon
                        class="icon-settings"
                        icon="three-dots-vertical"
                      ></b-icon>
                    </template>
                    <b-dropdown-item
                      v-on:click.stop="onRemoveSupervisor(supervisor)"
                      href="#"
                      >Remove supervisor</b-dropdown-item
                    >
                  </b-dropdown>
                </div>
                <small class="chat-truncate" v-if="supervisor.is_employee"
                  >(employee)</small
                >
              </b-list-group-item>
            </div>
          </b-list-group>
        </b-col>
        <b-col>
          <b-list-group flush>
            <h4>Module coordinator</h4>
            <b-list-group-item href="#">
              {{ module_coordinator.name }}
            </b-list-group-item>
          </b-list-group>
        </b-col>
      </b-row>
      <hr />
      <b-row v-if="current_user && current_user.is_employee">
        <b-col>
          <h4>Add new enrollment</h4>
          <b-form-input
            id="input-user"
            type="text"
            placeholder="Search on m-number, name or email"
            list="user-search-list"
            v-model="search"
            debounce="500"
            v-on:keyup.enter="searchUser(search)"
          ></b-form-input>

          <div class="d-flex justify-content-center mt-2" v-if="loadingSearch">
            <b-spinner label="Loading..."></b-spinner>
          </div>
          <b-list-group class="mt-2 mb-4" v-else>
            <b-list-group-item
              v-if="Object.keys(searchResults).length === 0"
              href="#"
            >
              No users found...
            </b-list-group-item>
            <b-list-group-item
              v-for="user in searchResults"
              :key="user.id"
              href="#"
            >
              {{ user.name }}
              <small>- {{ user.id }}, {{ user.email }}</small>
              <b-dropdown
                class="dropdown-dropright"
                dropright
                variant="link"
                no-caret
              >
                <template #button-content>
                  <b-icon
                    class="icon-settings"
                    icon="three-dots-vertical"
                  ></b-icon>
                </template>
                <b-dropdown-item v-on:click.stop="onAddStudent(user)" href="#"
                  >Add as student</b-dropdown-item
                >
                <b-dropdown-item
                  v-on:click.stop="onAddSupervisor(user)"
                  href="#"
                  >Add as supervisor</b-dropdown-item
                >
              </b-dropdown>
            </b-list-group-item>
          </b-list-group>
        </b-col>
        <b-col>
          {{ canvasImport.groupsetOptions }}
          <h4>Import enrollments from Canvas</h4>
          <div
            class="d-flex justify-content-center mb-3"
            v-if="loadingAssignmentsAndGroups"
          >
            <b-spinner label="Loading..."></b-spinner>
          </div>
          <p v-if="canvasButton && !loadingAssignmentsAndGroups">
            You did not yet set a valid API token for Canvas.<br />
            <b-button
              :to="{ name: 'canvas-token' }"
              class="mt-2"
              variant="info"
            >
              Set Canvas token
            </b-button>
          </p>
          <b-form-group
            v-if="!loadingAssignmentsAndGroups && !canvasButton"
            inline
            class="pt-2 mb-1"
          >
            <b-form-checkbox
              class="mb-3"
              id="checkbox-1"
              v-model="canvasImport.delete_current_enrollments"
              name="checkbox-1"
              :value="true"
              :unchecked-value="false"
            >
              Delete all current enrollments
            </b-form-checkbox>
            <b-form-radio
              v-model="canvasImport.all_students"
              name="students-assignment"
              :value="true"
              >All students of this course</b-form-radio
            >
            <b-form-radio
              v-model="canvasImport.all_students"
              name="students-assignment"
              :value="false"
              >Specific groupsets</b-form-radio
            >
            <b-form-checkbox-group
              v-if="canvasImport.all_students === false"
              class="pl-4"
              v-model="canvasImport.groupsetChoice"
              :options="groupsetOptions"
              name="Groupsets"
              label="Select groupsets"
              stacked
            ></b-form-checkbox-group>
            <b-overlay
              :show="loadingImport"
              rounded
              opacity="0.6"
              spinner-small
              spinner-variant="primary"
            >
              <b-button
                block
                variant="info"
                class="mt-3 mb-3"
                :disabled="
                  (canvasImport.all_students !== null &&
                    canvasImport.all_students === true &&
                    canvasImport.all_students === false) ||
                    loadingImport
                "
                @click="postCourseImport()"
                >Import</b-button
              >
            </b-overlay>
          </b-form-group>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>
<style scoped>
.people_list {
  height: 500px;
  overflow-y: scroll;
}
</style>
<script>
import { mapGetters, mapActions, mapState } from "vuex";
export default {
  name: "TestPeople",
  data() {
    return {
      students: {},
      supervisors: {},
      module_coordinator: {},
      search: "",
      loadingSearch: false,
      searchResults: [],
      loading: {
        people: true,
      },
      canvasImport: {
        delete_current_enrollments: false,
        all_students: null,
        groupsets: [],
        groupsetChoice: [],
      },
      loadingImport: false,
      loadingAssignmentsAndGroups: false,
      canvasButton: false,
    };
  },
  methods: {
    ...mapActions(["throwError", "loadTest"]),
    // Adding a student enrollment
    onAddStudent(user) {
      if (user.user_id) {
        let putData = { role: "S" };
        this.$http
          .post(
            "/tests/" + this.$route.params.id + "/users/" + user.user_id,
            putData
          )
          .then(() => {
            // Locally add the student to the view
            this.$set(this.students, user.user_id, user);
            // Note that the added user is only visible for people who load the test endpoint after the user is added to the test
            // is not really an error, but we let the user know via this way
            this.throwError({
              message:
                "Added this user to the test, but be aware that other users only receive this update on refresh",
            });
            // Reload the test endpoint to get the newest information
            this.loadTest({ test_id: this.$route.params.id });
          })
          .catch((error) => {
            this.throwError({
              message: "Error while adding student",
              error,
              extra: {
                user,
              },
            });
          });
      } else {
        this.throwError({
          message: "Error while adding student: invalid user",
          extra: {
            user,
          },
        });
      }
    },
    // Adding a supervisor enrollment
    onAddSupervisor(user) {
      if (user.user_id) {
        let putData = { role: "T" };
        this.$http
          .post(
            "/tests/" + this.$route.params.id + "/users/" + user.user_id,
            putData
          )
          .then(() => {
            // Locally add the student to the view
            this.$set(this.supervisors, user.user_id, user);
            // Note that the added user is only visible for people who load the test endpoint after the user is added to the test
            // is not really an error, but we let the user know via this way
            this.throwError({
              message:
                "Added this supervisor to the test, but be aware that other users only receive this update on refresh",
              extra: {
                user,
              },
            });
            // Reload the test endpoint to get the newest information
            this.loadTest({ test_id: this.$route.params.id });
          })
          .catch((error) => {
            this.throwError({
              message: "Error while adding student",
              error,
              extra: {
                user,
              },
            });
          });
      } else {
        this.throwError({
          message: "Error while adding student: invalid user",
          extra: {
            user,
          },
        });
      }
    },
    // Remove a student enrollment
    onRemoveStudent(student) {
      this.$http
        .delete("/tests/" + this.$route.params.id + "/users/" + student.user_id)
        .then(() => {
          this.$delete(this.students, student.user_id);
        })
        .catch((error) => {
          this.throwError({
            message: "Error while removing student enrollment",
            error,
            extra: {
              student,
            },
          });
        });
    },
    // Remove a supervisor enrollment
    onRemoveSupervisor(supervisor) {
      this.$http
        .delete(
          "/tests/" + this.$route.params.id + "/users/" + supervisor.user_id
        )
        .then(() => {
          this.$delete(this.supervisors, supervisor.user_id);
        })
        .catch((error) => {
          this.throwError({
            message: "Error while removing supervisor enrollment",
            error,
            extra: {
              supervisor,
            },
          });
        });
    },
    // Get the people information of the current test
    getPeople() {
      this.loading.people = true;
      this.$http
        .get("/tests/" + this.$route.params.id + "/users")
        .then((response) => {
          this.loading.people = false;
          // If there are students, show them
          if (response.data.students) {
            this.students = response.data.students;
          } else {
            this.throwError({
              message:
                "Error while getting test people: students object not present",
              extra: {
                test_id: this.$route.params.id,
                response,
              },
            });
          }
          // If there are supervisors, show them
          if (response.data.teachers) {
            this.supervisors = response.data.teachers;
          } else {
            this.throwError({
              message:
                "Error while getting test people: teachers object not present",
              extra: {
                test_id: this.$route.params.id,
                response,
              },
            });
          }
          // If there is a module coordinator, show it
          if (response.data.module_coordinator) {
            this.module_coordinator = response.data.module_coordinator;
          } else {
            this.throwError({
              message:
                "Error while getting test people: module_coordinator object not present",
              extra: {
                test_id: this.$route.params.id,
                response,
              },
            });
          }
        })
        .catch((error) => {
          this.loading.people = false;
          this.throwError({
            message: "Error while getting test people",
            error,
            extra: {
              test_id: this.$route.params.id,
            },
          });
        });
    },
    getAssignmentsAndGroups(courseId) {
      this.loadingAssignmentsAndGroups = true;
      var self = this;
      this.$http
        .get("/canvas/courses/" + courseId + "/assignments_and_groups")
        .then(function(response) {
          // handle success
          self.loadingAssignmentsAndGroups = false;
          self.canvasImport.groupsets = response.data.groupsets;
        })
        .catch(function(error) {
          // handle error
          self.loadingAssignmentsAndGroups = false;

          if (error.response.status == 401) {
            self.canvasButton = true;
          } else {
            self.throwError({
              message:
                "Could not retrieve assignments and groupsets from Canvas. Please try again.",
              error,
              extra: {
                error,
              },
            });
          }
        });
    },
    postCourseImport() {
      this.loadingImport = true;
      var self = this;
      var data = {
        delete_current_enrollments: this.canvasImport
          .delete_current_enrollments,
        all_students: this.canvasImport.all_students,
        groupsets: this.canvasImport.groupsets,
      };
      this.$http
        .post("/canvas/tests/" + self.$route.params.id + "/people", data, {
          withCredentials: true,
        })
        .then(function() {
          // handle success
          self.loadingImport = false;
          alert("Done!");
          self.$router.push({
            name: "test-people",
            params: { id: self.$route.params.id },
          });
        })
        .catch(function(error) {
          // handle error
          self.loadingImport = false;

          self.throwError({
            message: "Importing people from Canvas failed. Please try again.",
            error,
            extra: {
              error,
            },
          });
        });
    },
    // Search for a user in the system and display in the search results list
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
              extra: {
                searchQ: val,
                response,
              },
            });
          }
        })
        .catch((error) => {
          this.loadingSearch = false;
          this.throwError({
            message: "Error while getting users",
            error,
            extra: {
              searchQ: val,
            },
          });
        });
    },
  },
  computed: {
    ...mapGetters(["active_test"]),
    ...mapState({
      current_user: (state) => state.app.current_user,
    }),
    groupsetOptions: function() {
      var options = [];
      for (var i = 0; i < this.canvasImport.groupsets.length; i++) {
        options.push({
          text: this.canvasImport.groupsets[i].name,
          value: this.canvasImport.groupsets[i].canvasId,
        });
      }
      return options;
    },
  },
  watch: {
    // If typed at least for characters in the search field, auto search
    search: function(newVal) {
      if (!newVal || newVal.length < 4) {
        return null;
      }
      this.searchUser(newVal);
    },
  },
  mounted: function() {
    this.getPeople();
    if (this.active_test) {
      this.getAssignmentsAndGroups(this.active_test.course_id);
    } else {
      console.log(
        "oh oh nou is het mis, want active test is nog niet hier terwijl ik de course id al nodig heb sad"
      );
    }
  },
};
</script>

<style></style>
