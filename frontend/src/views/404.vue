<template>
  <div class="container">
    <div class="login-square m-5 p-5 shadow rounded mx-auto">
      <h1>Sekhmet</h1>
      <p>This page could not be found. (Error 404).</p>
      <b-button
          class="btn btn-primary mt-3"
          variant="info"
          :to="{ name: 'Dashboard' }"
        >
          Go to homepage
        </b-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "PageNotFound",
  data() {
    return {

    }
  },
  methods: {
  },
  computed: {
  },
};
</script>
