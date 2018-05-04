const http = require('http');
const express = require('express');
const socketio = require('socket.io');

const RpsGame = require('./rps-game'); // call rps-game.js
const app = express();

const clientPath = `${__dirname}/../client`;
console.log(`Serving static from ${clientPath}`);

app.use(express.static(clientPath));

const server = http.createServer(app);

const io = socketio(server);

let waitingPlayer = null;

// call when user connected?
io.on('connection', (sock) => {
	
	if (waitingPlayer) { // if there is waiting player -> start a game

		//[sock, waitingPlayer].forEach(s => s.emit('message', 'Game Starts!')); // send message to two guys
		new RpsGame(waitingPlayer , sock);
		waitingPlayer = null;
	} else { // no player waiting -> waitingPlayer will be sock
		waitingPlayer = sock;
		waitingPlayer.emit('message', 'Waiting for an opponent');
	}

	sock.on('message', (text) => { // message inputed?
		io.emit('message', text); // broadcast message
	});
});

server.on('error', (err) => {
	console.error('Server error:', err);
});


server.listen(8080, () => {
	console.log('RSP started on 8080');
});
