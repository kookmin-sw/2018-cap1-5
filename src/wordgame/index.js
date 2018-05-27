// http, express, app, port 정의
const http = require('http');
var express = require('express');
var app = express();// Create a new instance of Express
var port = process.env.PORT || 8080;
// createServer
var server = http.createServer(app);
// server.on listen
server.on('error', (err) => {
	console.error('Server error:', err);
});
server.listen(port, function(){
	console.log('listening on *:' + port);
	}); // Create a Node.js based http server on port 8080

//W가 필요한 정적파일 로드
var path = require('path'); // Import the 'path' module (packaged with Node.js)

//W만 사용하는 코드
var agx = require('./agxgame');// Import the Anagrammatix game file.
// Create a simple Express application
app.configure(function() {
    // Turn down the logging activity
    app.use(express.logger('dev'));

    // Serve static html, js, css, and image files from the 'public' directory
    app.use(express.static(path.join(__dirname,'public')));
});
// socket.io 불러오기
var io = require('socket.io').listen(server); // Create a Socket.IO server and attach it to the http server

// io.on 부분
io.set('log level',1); // Reduce the logging output of Socket.IO

// Listen for Socket.IO Connections. Once connected, start the game logic.
io.sockets.on('connection', function (socket) {
    //console.log('client connected');
    agx.initGame(io, socket);
});


