import Vue from "vue";
import App from "./App.vue";
import store from "./store";
import axios from "axios";
import VueAxios from "vue-axios";
import router from "./router";
import Vuex from "vuex";

import { BootstrapVue, IconsPlugin } from "bootstrap-vue";

// Import Bootstrap an BootstrapVue CSS files (order is important)
import "bootstrap/dist/css/bootstrap.css";
import "bootstrap-vue/dist/bootstrap-vue.css";

Vue.config.productionTip = false;

// Make BootstrapVue available throughout your project
Vue.use(BootstrapVue);
// Optionally install the BootstrapVue icon components plugin
Vue.use(IconsPlugin);

// Make Vuex available throughout the project
Vue.use(Vuex);

axios.defaults.baseURL = "/api";
axios.defaults.headers.get["Accept"] = "application/json"; // default header for all get request
axios.defaults.headers.post["Accept"] = "application/json"; // default header for all POST request
axios.defaults.headers.put["Accept"] = "application/json"; // default header for all POST request
axios.defaults.headers.delete["Accept"] = "application/json"; // default header for all POST request
axios.defaults.withCredentials = true;

Vue.use(VueAxios, axios);

new Vue({
  store,
  router,
  render: (h) => h(App),
}).$mount("#app");
