// http, express, app,port 정의
const http = require('http');
const express = require('express');
const app = express();
var port = process.env.PORT || 4000;
// createServer
const server = http.createServer(app);
// socket.io 불러오기
const socketio = require('socket.io');
const io = socketio(server);
/*B-A*/const clientPath = `${__dirname}/../client`;
/*B-A*/console.log(`Serving static from ${clientPath}`);
/*B-A*/let waitingPlayer = null;

// io.on 부분
io.on('connection', function(socket){

/*B-A*/	if (waitingPlayer) { // if there is waiting player -> start a game

/*B-A*/		//[socket, waitingPlayer].forEach(s => s.emit('message', 'Game Starts!')); // send message to two guys
/*B-A*/		new RpsGame(waitingPlayer , socket);
/*B-A*/		waitingPlayer = null;
/*B-A*/	} else { // no player waiting -> waitingPlayer will be sock
/*B-A*/		waitingPlayer = socket;
/*B-A*/		waitingPlayer.emit('message', 'Waiting for an opponent');
/*B-A*/	}
	
	
	socket.on('message', function(msg){ // message inputed?
		io.emit('message', msg); // broadcast message
	});
});
	
// server.on listen
server.on('error', (err) => {
	console.error('Server error:', err);
});
server.listen(port, function(){
	console.log('listening on *:' + port);
	});


/*B-A*/const RpsGame = require('./rps-game'); // call rps-game.js
/*B-A*/app.use(express.static(clientPath));
/*B-A*/
/*B-A*/
/*B-A*/
