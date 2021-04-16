module.exports = {
  //...
  devServer: {
    allowedHosts: ["sekhmet.chat"],
    proxy: {
      "/api": {
        target: "https://localhost",
        ws: true,
      },
      "/socket": {
        target: "https://localhost",
        ws: true,
      },
    },
  },
};
