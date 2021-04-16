import * as webSocket from './webSocket.js';
import * as p2p from './p2p.js';

export function start(name){
    webSocket.startSocket(name); // open websocket and send initmessage to server
    webSocket.addReceiveSwitch(p2p.theP2pMessageSwitch) // add the p2p messages to the websocket's receive hook
    p2p.values.my_id = name; // set my own id in the p2p
}

// just call p2p.addChannel(testId,personId);
// testId is used for storeChannel, nothing else.

export var connections = new Map();
p2p.hooks.storeConnection = (personId,connection) => {
    connections.set(personId, connection)};
p2p.hooks.getConnection = (personId) => {return connections.get(personId)};
export var channels = new Map();
p2p.hooks.storeChannel = (chatId, personId, channel) => {
    channels.set(personId, channel);
};

const myIdField = document.querySelector('textarea#my_id');
const startButton = document.querySelector('button#start');
const destIdField = document.querySelector('textarea#other_id');
const chatIdField = document.querySelector('textarea#chat_id');
const testIdField = document.querySelector('textarea#test_id');
const connectButton = document.querySelector('button#connect');
const sendTextField = document.querySelector('textarea#text');
const sendButton = document.querySelector('button#send');
const receiveField = document.querySelector('textarea#receive');
const printButton = document.querySelector('button#print');

startButton.onclick = () => {
    start(myIdField.value)};
connectButton.onclick = () => {
    p2p.addChannel(chatIdField.value,destIdField.value)};
sendButton.onclick = () => {
    channels.get(destIdField.value).send(JSON.stringify({
        id: Math.round(Math.random() * 100),
        sender_id: myIdField.value,
        test_id: testIdField.value,
        chat_id: chatIdField.value,
        timestamp: Date.now(),
        message_index: 0,
        is_final: true,
        content: sendTextField.value,
    }))};


printButton.onclick = () => {console.log(p2p.connection.iceConnectionState, p2p.connection.iceGatheringState, p2p.connection.currentLocalDescription)};

p2p.hooks.collectMessage = (message) => {receiveField.value = message};
