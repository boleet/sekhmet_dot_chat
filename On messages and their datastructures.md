# Local data structures in front-end

## Websocket communications:

```
{
	messageType: string
	receiverId: int 	# -> links to person_id in DB, null if aimed at server
	message: {}		# format depends on messageType
}
```

### The different messageTypes, and what the server should do with them:

#### "init"

receiverId: doesn't matter, assumed 0

message: String, should equal this person's personId.

Action: Verify personId using httpSession context & save webSocketSession to personId.

#### "message"

receiverId: some receiver

message: message object as defined below

Action: forward message to receiver

#### "message_final"

receiverId: some receiver

message: message object as defined below

Action: forward message to receiver and save contents to database.

#### "message_edit"

receiverId: some receiver

message: message object as defined below

Action: forward message to receiver and update contents in database.

#### "message_delete"

receiverId: some receiver

testId: id of the test the message belongs to

conversationId: id of the conversation the mesasge belongs to

messageId: id of the message to be deleted

Action: forward message to receiver and updated visbility field in the database

#### "conversation_unread"

receiverId: some receiver

testId: id of the test the conversation belongs to

conversationId: id of the conversation that is being updated

unread: integer representing the amount of unread messages

Action: forward message to receiver and updated unread field in the database

#### "conversation_assigned"

receiverId: some receiver

testId: id of the test the conversation belongs to

conversationId: id of the conversation that is being updated

userId: id of the user that the conversation now is assigned to

Action: forward message to receiver and updated unread field in the database

## A message, as it is received raw from tcp connection:

```
message: {
	message_id: int
	sender_id: int 		# -> links to person_id in DB, not technically nescecary in p2p because of ip, but can be used to verify regardless.
	test_id: int		# -> links to test_id   in DB
	chat_id: int 	    	# -> links to chat_id   in DB
	timestamp: unix timestamp
	message_index: revision: int # how manyeth version of this message?
	is_final: bool          # curretly typing or  message send
	content: string
	}
```

If message_index <= current -> discard

message received: discard non-final messages with lower index for same chat & sender (should always be only one)

## The local people DB, kept up to date by server-connection:

```
local_people_db: { # map
	int sender_id_1: {
		user_id: int
		display_name: string
		connection: RTCPeerConnection
		is_supervisor: bool # important for teachers whether to send keystrokes. A TA is not going to be TA-ing while taking a test. So while the app is running everybody is either a supervisor or a student.
		},
	int sender_id_2...
	}
```

## the local message DB

Messages as described [above](#a-message-as-it-is-received-raw-from-tcp-connection)

```
local_message_db: { # a map
	int test_id: {
		int chat_id: {
			id: int,
			unread: int # amount of unread messages in chat
			last_timestamp: unix timestamp # last message received
			people: [ // a map
				int sender_id_1: DataChannel,
				int sender_id_2...
				]
			messages: [		// order maintained first-last
				message1: // message as described above
				message2...
				]
			},
		int chat_id...
		}
	int test_id...
	}
```

## full example for front-end structure (05-03-2021 11:03)

```
tests: {
  0:{
    chats: {
      0: {
	id: 0,
	name: "Chat 1",
	unread: 1,
	last_message_id: 1,
	type: CHAT_ONETOONE,
	people: {
	  0: {
	    //datachannel
	  },
	  1: {
	    //datachannel
	  },
	},
	messages: {
	  0: {
	    id: 0,
	    sender_id: 1,
	    test_id: 0,
	    chat_id: 0,
	    timestamp: 1614932305439,
	    message_index: null,
	    is_final: true,
	    content: "Good luck with the exam!",
	  },
	  1: {
	    id: 1,
	    sender_id: 0,
	    test_id: 0,
	    chat_id: 0,
	    timestamp: 1614932305439,
	    message_index: null,
	    is_final: true,
	    content: "Thanks, I studied hard...",
	  },
	},
      },
      1: {
	id: 1,
	name: "Questions",
	unread: 0,
	last_message_id: 3,
	type: CHAT_ONETOONE,
	people: {
	  0: {
	    //datachannel
	  },
	  1: {
	    //datachannel
	  },
	},
	messages: {
	  2: {
	    id: 2,
	    sender_id: 0,
	    test_id: 0,
	    chat_id: 0,
	    timestamp: 1614932305439,
	    message_index: null,
	    is_final: true,
	    content: "I have a question about excercise 1....",
	  },
	  3: {
	    id: 3,
	    sender_id: 1,
	    test_id: 0,
	    chat_id: 0,
	    timestamp: 1614932305439,
	    message_index: null,
	    is_final: true,
	    content: "Ah yeah that difficult indeed.",
	  },
	},
      },
      2: {
	id: 2,
	name: "Announcements",
	unread: 0,
	last_message_id: 4,
	type: CHAT_ANNOUNCEMENTS,
	people: {
	  0: {
	    //datachannel
	  },
	  1: {
	    //datachannel
	  },
	},
	messages: {
	  4: {
	    id: 4,
	    sender_id: 0,
	    test_id: 0,
	    chat_id: 0,
	    timestamp: 1614932305439,
	    message_index: null,
	    is_final: true,
	    content: "Good luck with the exam...",
	  },
	},
      },
    }
  }
}
```
