var socket;

// Starts a websocket connection to the server
// If an error occurs, this is logged to the error endpoint
// the closeSocketCallback is called, and an attempt is made
// to create a new websocket connection
export function startSocket(closeSocketCallback) {
  socket = new WebSocket(
    location.origin.replace(/^http/, "ws") + "/socket/mainSocket"
  );

  socket.onopen = () => {
    console.log("ws onopen");
  };

  socket.onclose = (event) => {
    console.log("ws onclose");
    if (event.code != 1000) {
      var xmlhttp = new XMLHttpRequest();
      xmlhttp.open("POST", "/api/error");
      xmlhttp.setRequestHeader(
        "Content-Type",
        "application/json;charset=UTF-8"
      );
      xmlhttp.send(
        JSON.stringify({
          timestamp: Date.now(),
          error: "websocket closed for suspicous reason",
          extra: {
            code: event.code,
            reason: event.reason,
          },
        })
      );
    }
    setTimeout(function() {
      console.log("Trying to reconnect to ws after 5 seconds...");
      startSocket();
      closeSocketCallback();
    }, 5000);
  };
  socket.onerror = (error) => {
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.open("POST", "/api/error");
    xmlhttp.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    xmlhttp.send(
      JSON.stringify({
        timestamp: Date.now(),
        error: "websocket error",
        extra: error,
      })
    );
  };
}

// Allows to externally add a receive switch for the websocket
// The given filter function is called each time a message is received
export function addReceiveSwitch(filter) {
  socket.onmessage = (event) => {
    // TODO remove this log
    console.log("ws receiving:\n" + event.data);
    var json = JSON.parse(event.data);
    filter(json);
  };
}

// Sends a message over the websocket connection
export function sendToServer(message) {
  if (socket && socket.readyState === 1) {
    // TODO remove this log
    console.log("ws sending:\n" + JSON.stringify(message));
    socket.send(JSON.stringify(message));
    return true;
  }
  return false;
}
