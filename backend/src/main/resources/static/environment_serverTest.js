import * as webSocket from './webSocket.js';

const name = Math.random().toString().substr(2,8);

function send(message){
    webSocket.sendToServer(message);
}

webSocket.startSocket(name); // open websocket and send initmessage to server

function sendMessage(mindex){
    var delay = 0;
    var kindex;
    for (kindex = 0; kindex < 100; kindex++){
	setTimeout(
	    () => {
		send({
		    messageType: "benchmark",
		    receiverId: "BenchmarkTestReceiver",
		    message: Date.now()
		    });
	    }
	    ,delay);
	delay += 50 + (Math.random() * 100) // 50-150: average 10/second
    }
}
var delay = 0;
var mindex;
for(mindex = 0; mindex < 120; mindex++){
    delay += 15000 + (Math.random() * 30000) // 15000 - 45000: average 2/minute
    console.log(delay);
    setTimeout(
	() => {	sendMessage(mindex)}
	,delay);
}
