// http, express, app,port 정의
var http = require('http');
var express = require('express');
var app = express();
var port = process.env.PORT || 3000;
// createServer
var server = http.createServer(app);//추가
// server.on listen
server.on('error', (err) => {
	console.error('Server error:', err);
});
server.listen(port, function(){
  console.log('listening on *:' + port);
});

//A가 필요한 정적파일 로드
var clientPath = require('path');
/*A*/app.use(express.static(clientPath.join(__dirname, 'js')));
//A만 사용하는 코드
/*A*/app.get('/', function(req, res){
/*A*/  res.sendFile(__dirname + '/index.html');
/*A*/});
// socket.io 불러오기
var socketio = require('socket.io');
var io = socketio(server);
// io.on 부분

io.on('connection', function(socket){











  socket.on('message', function(msg){
    io.emit('message', msg);
  });
});
