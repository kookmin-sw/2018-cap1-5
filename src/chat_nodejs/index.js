// http, express, app,port 정의
var http = require('http');
var express = require('express');
var app = express();
var port = process.env.PORT || 3000;
// createServer
var server = http.createServer(app);//추가
// socket.io 불러오기
var socketio = require('socket.io');
var io = socketio(server);
/*A-B*/var clientPath = require('path');
/*A-B*/console.log(`Serving static from ${clientPath}`);


// io.on 부분
io.on('connection', function(socket){












  socket.on('message', function(msg){
    io.emit('message', msg);
  });
});

// server.on listen
server.on('error', (err) => {
	console.error('Server error:', err);
});
server.listen(port, function(){
  console.log('listening on *:' + port);
});


/*A-B*/
/*A-B*/app.use(express.static(clientPath.join(__dirname, 'js')));
/*A-B*/app.get('/', function(req, res){
/*A-B*/  res.sendFile(__dirname + '/index.html');
/*A-B*/});
