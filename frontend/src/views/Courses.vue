<template>
  <div class="container">
    <div class="d-flex justify-content-center mt-2" v-if="loading.courses">
      <h4 class="mr-3">Loading courses</h4>
      <b-spinner label="Loading..."></b-spinner>
    </div>
    <p
      v-if="Object.keys(courses).length == 0 && this.loading.courses === false"
    >
      <b-alert show variant="danger">
        <h4 class="alert-heading">No tests found...</h4>
        <p>
          There are no courses with tests found for you. If you expect to see a
          test here, you are either not enrolled or the test is not started by
          the supervisors yet.
        </p>
      </b-alert>
    </p>
    <div
      class="accordion"
      role="tablist"
      v-if="Object.keys(courses).length !== 0"
    >
      <b-card
        no-body
        class="mb-1"
        v-for="course in sorted_courses"
        :key="course.canvas_id"
      >
        <b-card-header header-tag="header" class="p-1" role="tab">
          <b-button
            block
            v-b-toggle="'course-' + course.canvas_id"
            variant="info"
            >{{ course.name }}
            <b-dropdown
              class="dropdown-dropleft float-right"
              dropleft
              variant="link"
              no-caret
              v-if="current_user.is_employee"
            >
              <template #button-content>
                <b-icon
                  class="icon-settings"
                  icon="three-dots-vertical"
                ></b-icon>
              </template>
              <b-dropdown-item
                v-on:click.stop="onCourseDetails(course)"
                href="#"
                >Course details</b-dropdown-item
              >
            </b-dropdown>
          </b-button>
        </b-card-header>
        <b-collapse
          v-bind:id="'course-' + course.canvas_id"
          visible
          accordion="courses-accordion"
          role="tabpanel"
        >
          <b-card-body>
            <b-table
              striped
              hover
              :items="Object.values(course.tests)"
              :fields="course_table_fields"
              @row-clicked="testClickedHandler"
            >
              <template #cell(activate)="data">
                <b-button
                  :disabled="test_started(data.item)"
                  size="sm"
                  v-on:click.stop="testStartHandler(data.item)"
                >
                  Start
                  <b-icon
                    class="icon-play"
                    icon="play-fill"
                    aria-hidden="true"
                  ></b-icon>
                  <b-spinner
                    v-if="loading.start_request.includes(data.item.test_id)"
                    small
                  ></b-spinner>
                </b-button>
                <span
                  class="font-italic"
                  v-if="loading.start_request.includes(data.item.test_id)"
                  ><br />This might take a minute...</span
                >
              </template>
              <template #cell(settings)="data">
                <b-button
                  size="sm"
                  v-on:click.stop="testSettingsHandler(data.item)"
                >
                  <b-icon
                    class="icon-play"
                    icon="gear-fill"
                    aria-hidden="true"
                  ></b-icon>
                </b-button>
              </template>
              <template #cell(open)="data">
                <b-button
                  size="sm"
                  v-on:click.stop="testClickedHandler(data.item)"
                >
                  Go to test
                  <b-icon
                    class="icon-play"
                    icon="play-fill"
                    aria-hidden="true"
                  ></b-icon>
                </b-button>
              </template>
            </b-table>
          </b-card-body>
        </b-collapse>
      </b-card>
    </div>
    <hr />
    <b-button
      block
      v-if="current_user.is_employee"
      :to="{ name: 'course-import' }"
      >New course</b-button
    >
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
      courses: {},
      loading: {
        courses: true,
        start_request: [],
      },
    };
  },
  methods: {
    ...mapActions(["throwError"]),
    // On clicking a test, open the chat page of that test
    testClickedHandler: function(item) {
      this.$router.push({ name: "test-chat", params: { id: item.test_id } });
    },
    // On click the settings icon of a test, open the settings page of that test
    testSettingsHandler: function(item) {
      this.$router.push({
        name: "test-settings",
        params: { id: item.test_id },
      });
    },
    // On click the test start button, open that test
    testStartHandler: function(item) {
      this.loading.start_request.push(item.test_id);

      let reopen = "";
      if (item.start_time !== null && item.end_time !== null) {
        reopen = "?reopen=true";
      }
      this.$http
        .put("/tests/" + item.test_id + "/open" + reopen)
        .then(() => {
          // remove loading state for this start test
          if (this.loading.start_request.indexOf(item.test_id) != -1) {
            this.loading.start_request.splice(
              this.loading.start_request.indexOf(item.test_id),
              1
            );
          }
          // If the test is opened, open the chat page of that test
          this.$router.push({
            name: "test-chat",
            params: { id: item.test_id },
          });
          item.start_time = 0; // for now send start time to not null
        })
        .catch((error) => {
          item.start_time = null;
          // remove loading state from this start test
          if (this.loading.start_request.indexOf(item.test_id) != -1) {
            this.loading.start_request.splice(
              this.loading.start_request.indexOf(item.test_id),
              1
            );
          }
          this.throwError({
            message: "Error while starting test",
            error,
            extra: item,
          });
        });
    },
    // Return if the given test has been started
    test_started: function(item) {
      return item.start_time !== null && item.end_time === null;
    },
    // Load all courses for the current user
    loadCourses: function() {
      this.loading.courses = true;
      this.$http
        .get("/courses")
        .then((response) => {
          this.loading.courses = false;
          if (response.data.courses) {
            this.courses = response.data.courses;
            // if you're not an employee, check if we should redirect you now
            if (!this.current_user.is_employee) {
              if (Object.values(this.courses).length === 1) {
                // only one active course for you
                if (
                  Object.values(Object.values(this.courses)[0].tests).length ===
                  1
                ) {
                  // and that course has only one test, so redirect
                  this.$router.push({
                    name: "test-chat",
                    params: {
                      id: Object.values(Object.values(this.courses)[0].tests)[0]
                        .test_id,
                    },
                  });
                }
              }
            }
          } else {
            this.courses = {};
          }
        })
        .catch((error) => {
          this.loading.courses = false;
          this.throwError({
            message: "Error while loading courses",
            error,
          });
        });
    },
    // Open the course details page
    onCourseDetails: function(item) {
      this.$router.push({ name: "course", params: { id: item.canvas_id } });
    },
  },
  computed: {
    ...mapState({
      current_user: (state) => state.app.current_user,
      sorted_courses: function() {
        let list = Object.values(this.courses).sort(function(a, b) {
          return a.created_at - b.created_at;
        });
        return list;
      },
    }),
    // Return the fields to display in the courses table
    // depends on if you're an employee or not
    course_table_fields: function() {
      if (this.current_user && this.current_user.is_employee) {
        return ["name", "settings", "activate", "open"];
      } else {
        return ["name", "open"];
      }
    },
  },
  created: function() {
    this.loadCourses();
  },
};
</script>
