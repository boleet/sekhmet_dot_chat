import * as webSocket from './webSocket.js';
import * as p2p from './p2p_udp.js';

const name = Math.random().toString().substr(2,8);
webSocket.startSocket(name,
		      () =>{
		      p2p.values.my_id = name;
		      p2p.addChannel(0,"BenchmarkTestReceiver");
		      }		     ) // open websocket and send initmessage to server
webSocket.addReceiveSwitch(p2p.theP2pMessageSwitch) // add the p2p messages to the websocket's receive hook

var myConnection;
p2p.hooks.storeConnection = (personId,connection) => {
    myConnection =  connection};
p2p.hooks.getConnection = (personId) => {return myConnection};
var myChannel;
p2p.hooks.storeChannel = (chatId, personId, channel) => {
    myChannel =  channel;
};

function sendMessage(mindex){
    var delay = 0;
    var kindex;
    for (kindex = 0; kindex < 100; kindex++){
	setTimeout(
	    () => {
		console.log(myChannel.readyState)
		myChannel.send(Date.now());
	    }
	    ,delay);
	delay += 50 + (Math.random() * 100) // 50-150: average 10/second
    }
}

var delay = 0;
var mindex;
for(mindex = 0; mindex < 120; mindex++){
    delay += 15000 + (Math.random() * 30000) // 15000 - 45000: average 2/minute
    setTimeout(
() => {sendMessage(mindex);}
	,delay);
}
