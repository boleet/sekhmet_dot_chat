import * as webSocket from './webSocket.js';
import * as p2p from './p2p.js';

const resultsField = document.getElementById('results');

function collectBenchmark (message,time){
    resultsField.textContent = resultsField.textContent + "," + (time - message);
}


const name = "BenchmarkTestReceiver"

webSocket.startSocket(name); // open websocket and send initmessage to server
webSocket.addReceiveSwitch(
    (message) => {
	if (message.messageType == "benchmark"){
	    collectBenchmark(message.message,Date.now());
	}else{
	    p2p.theP2pMessageSwitch(message)
	}
    }

)
p2p.values.my_id = name; // set my own id in the p2p
p2p.hooks.collectMessage = (message) => {collectBenchmark(message,Date.now())};
export var connections = new Map();
p2p.hooks.storeConnection = (personId,connection) => {
    connections.set(personId, connection)};
p2p.hooks.getConnection = (personId) => {return connections.get(personId)};
export var channels = new Map();
p2p.hooks.storeChannel = (chatId, personId, channel) => {
    channels.set(personId, channel);
};
