<template>
  <div class="container">
    <h1 class="d-flex justify-content-between">
      Import course
      <b-button
        class="align-self-center"
        :to="{ name: 'canvas-token' }"
        variant="info"
      >
        Set Canvas token
      </b-button>
    </h1>

    <p v-if="canvasButton">
      You did not yet set a valid API token for Canvas.
    </p>
    <!-- <p v-if="canvasButton">
      To import a course, you need to connect to Canvas first. Please click the button below.<br><br>
      <b-button href="https://sekhmet.chat/oauth2/authorization/canvas" class="px-3" style="background-color: #cf0072;">
        Connect to Canvas
      </b-button>
    </p> -->
    <p v-else>
      Select the course you want to import from Canvas. Courses which are
      already imported in the system, won't be listed.<br />You can always add
      more tests later on. It's also possible to add and remove people from a
      specific test.
    </p>
    <div class="row" v-if="!canvasButton">
      <div class="col">
        <div class="d-flex justify-content-center mb-3" v-if="loadingCourses">
          <b-spinner label="Loading..."></b-spinner>
        </div>
        <b-list-group v-else-if="!loadingCourses && courses.length > 0">
          <button
            type="button"
            class="list-group-item list-group-item-action"
            :class="activeCourse == course.id ? 'border-info' : ''"
            v-for="course in courses"
            :key="course.id"
            @click="clickCourse(course.id)"
          >
            {{ course.name }} <br /><small>{{ course.course_code }}</small>
          </button>
        </b-list-group>
        <div v-else>
          No courses found.
        </div>
      </div>
      <div class="col">
        <div
          class="d-flex justify-content-center mb-3"
          v-if="loadingAssignmentsAndGroups"
        >
          <b-spinner label="Loading..."></b-spinner>
        </div>
        <template v-if="activeCourse > 0 && !loadingAssignmentsAndGroups">
          <b-list-group>
            <b-list-group-item
              v-for="(assignment, index) in assignments"
              :key="index"
            >
              <b-form-checkbox
                :id="'checkbox-assignment-' + index"
                v-model="assignment.selected"
                :name="'checkbox-assignment-' + index"
              >
                {{ assignment.name }}
              </b-form-checkbox>
              <b-form-group
                inline
                v-if="assignment.selected"
                class="pl-4 pt-2 mb-1"
              >
                <b-form-radio
                  v-model="assignment.all_students"
                  :name="'students-assignment-' + index"
                  :value="true"
                  >All students of this course</b-form-radio
                >
                <b-form-radio
                  v-model="assignment.all_students"
                  :name="'students-assignment-' + index"
                  :value="false"
                  >Specific groupsets</b-form-radio
                >
              </b-form-group>
              <b-form-checkbox-group
                v-if="assignment.selected && assignment.all_students === false"
                class="pl-5"
                v-model="assignment.groupsets"
                :options="groupsetOptions"
                :name="'groupsets-assignment-' + index"
                label="Select groupsets"
                stacked
              ></b-form-checkbox-group>
            </b-list-group-item>
            <b-list-group-item
              v-for="(customTest, index) in customTests"
              :key="'custom-' + index"
            >
              <b-input-group class="my-1">
                <b-form-input
                  v-model="customTest.name"
                  placeholder="Enter custom test name"
                ></b-form-input>
                <b-input-group-append>
                  <b-button size="sm" @click="deleteCustomTest(index)"
                    ><b-icon icon="trash" aria-hidden="true"></b-icon
                  ></b-button>
                </b-input-group-append>
              </b-input-group>
              <b-form-group inline class="pl-4 pt-2 mb-1">
                <b-form-radio
                  v-model="customTest.all_students"
                  :name="'students-customtest-' + index"
                  :value="true"
                  >All students of this course</b-form-radio
                >
                <b-form-radio
                  v-model="customTest.all_students"
                  :name="'students-customtest-' + index"
                  :value="false"
                  >Specific groupsets</b-form-radio
                >
              </b-form-group>
              <b-form-checkbox-group
                v-if="customTest.all_students === false"
                class="pl-5"
                v-model="customTest.groupsets"
                :options="groupsetOptions"
                :name="'groupsets-customtest-' + index"
                label="Select groupsets"
                stacked
              ></b-form-checkbox-group>
            </b-list-group-item>
            <button
              type="button"
              class="list-group-item list-group-item-action"
              @click="addCustomTest()"
            >
              <b-icon
                icon="plus-circle"
                aria-hidden="true"
                class="mr-2"
              ></b-icon
              ><span class="text-muted">Add custom test</span>
            </button>
          </b-list-group>
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
              class="mt-3"
              :disabled="
                assignments.length + customTests.length < 1 || loadingImport
              "
              @click="postCourseImport()"
              >Import course
            </b-button>
          </b-overlay>
          <span v-if="loadingImport"> (this may take a minute or so)</span>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from "vuex";

export default {
  name: "CourseImport",
  data() {
    return {
      courses: [],
      activeCourse: null,
      assignments: [],
      groupsets: [],
      selectedAssignments: [],
      selectedStudents: {},
      selected: null,
      customTests: [],
      loadingCourses: false,
      loadingAssignmentsAndGroups: false,
      loadingImport: false,
      canvasButton: false,
    };
  },
  created: function() {
    this.getCourses();
  },
  computed: {
    groupsetOptions: function() {
      var options = [];
      for (var i = 0; i < this.groupsets.length; i++) {
        options.push({
          text: this.groupsets[i].name,
          value: this.groupsets[i].canvasId,
        });
      }
      return options;
    },
  },
  methods: {
    ...mapActions(["throwError"]),
    getCourses() {
      this.loadingCourses = true;
      var self = this;
      this.$http
        .get("/canvas/courses")
        .then(function(response) {
          // handle success
          self.loadingCourses = false;
          self.courses = response.data;
        })
        .catch(function(error) {
          // handle error
          self.loadingCourses = false;
          if (error.response.status == 401) {
            self.canvasButton = true;
          } else {
            self.throwError({
              message: "Could not load courses from Canvas",
              error,
            });
          }
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
          self.assignments = self.createAssignmentList(
            response.data.assignments
          );
          self.groupsets = response.data.groupsets;
        })
        .catch(function(error) {
          // handle error
          self.loadingAssignmentsAndGroups = false;
          self.throwError({
            message: "Could not retrieve assignments and groupsets from canvas",
            error,
            extra: { courseId },
          });
        });
    },
    postCourseImport() {
      this.loadingImport = true;
      var self = this;
      var data = this.assignments
        .filter((assignment) => assignment.selected == true)
        .concat(this.customTests);
      this.$http
        .post("/canvas/courses/" + self.activeCourse, data, {
          withCredentials: true,
        })
        .then(function() {
          // handle success
          self.loadingImport = false;
          self.$router.push({
            name: "course",
            params: { id: self.activeCourse },
          });
        })
        .catch(function(error) {
          // handle error
          self.loadingImport = false;
          self.throwError({
            message: "Importing course from canvas failed",
            error,
            extra: {
              activeCourse: self.activeCourse,
            },
          });
        });
    },
    createAssignmentList(assignments) {
      var list = [];
      for (var i = 0; i < assignments.length; i++) {
        list.push({
          selected: false,
          name: assignments[i],
          all_students: null,
          groupsets: [],
        });
      }
      return list;
    },
    clickCourse(courseId) {
      // alert(courseId)
      this.getAssignmentsAndGroups(courseId);
      this.activeCourse = courseId;
    },
    addCustomTest() {
      this.customTests.push({
        name: "",
        all_students: null,
        groupsets: [],
      });
    },
    deleteCustomTest(index) {
      this.customTests.splice(index, 1);
    },
  },
};
</script>

<style></style>
