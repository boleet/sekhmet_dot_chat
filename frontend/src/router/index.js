import Vue from "vue";
import VueRouter from "vue-router";
import Dashboard from "../views/Dashboard.vue";
import NotFoundPage from "../views/404.vue";
import Courses from "../views/Courses.vue";
import Course from "../views/course/Course.vue";
import CourseImport from "../views/course/Import.vue";
import TestImport from "../views/course/TestImport.vue";
import Test from "../views/test/Test.vue";
import TestChat from "../views/test/Chat.vue";
import TestPeople from "../views/test/People.vue";
import TestSettings from "../views/test/Settings.vue";
import CanvasToken from "../views/CanvasToken.vue";

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    name: "Dashboard",
    // TODO take a look at lazy loading & spliting code
    // component: () =>
    //   import(/* webpackChunkName: 'dashboard' */ '../views/Dashboard.vue'),
    component: Dashboard,
    redirect: "courses",
    children: [
      {
        path: "/canvas-token",
        name: "canvas-token",
        component: CanvasToken,
      },
      {
        name: "courses",
        path: "/courses",
        component: Courses,
      },
      {
        name: "course-import",
        path: "/courses/import",
        component: CourseImport,
      },
      {
        name: "course",
        path: "/courses/:id",
        component: Course,
      },
      {
        name: "test-import",
        path: "/courses/:id/tests/import",
        component: TestImport,
      },
      {
        name: "tests",
        path: "/tests/",
        component: Test,
        redirect: "courses",
        children: [
          {
            name: "test-chat",
            path: "/tests/:id/chats",
            component: TestChat,
          },
          {
            name: "test-people",
            path: "/tests/:id/people",
            component: TestPeople,
          },
          {
            name: "test-settings",
            path: "/tests/:id/settings",
            component: TestSettings,
          },
        ],
      },
    ],
  },
  {
    path: "*",
    name: "PageNotFound",
    component: NotFoundPage,
  },
];

const router = new VueRouter({
  mode: "history",
  base: process.env.BASE_URL,
  routes,
});

export default router;
