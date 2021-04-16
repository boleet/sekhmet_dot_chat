var socket; 

export function startSocket(name,onReadyCallback = () => {}){
    socket = new WebSocket("wss://sekhmet.chat/socket/mainSocket");
    socket.onopen = (event) =>
    {
	sendToServer({
	    messageType: "init",
	    receiverId: 0,
	    message: name
	});
	onReadyCallback();
    }
    socket.onclose = (event) =>
    {
	if (event.wasClean) {
	    console.log("websocket closed cleanly")
	} else {
	    console.log("websocket died: " + event.code)
	    console.log("reason: " + event.reason)
	}
    }
    socket.onerror = (error) =>
    {
	console.log("websocket error: " + error.message);
    }
}

export function addReceiveSwitch(filter){
    socket.onmessage = (event) =>
    {
	//console.log("receiving:\n"+ event.data)
        var json = JSON.parse(event.data);
	filter(json);
	//socket.onmessage(event);
    }
}

export function sendToServer(message){
    //console.log("sending:\n" + JSON.stringify(message));
    socket.send(JSON.stringify(message));
}
