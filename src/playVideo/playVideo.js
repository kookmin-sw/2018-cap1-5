// http, express, app,port 정의
const http = require('http');
// const express = require('express');
var app;
var port = process.env.PORT || 5000;
// 추가: app에 대한 정의
app = function (request, response){
    response.writeHead(200, {'Content-Type': 'video/mp4'});
    var rs = fs.createReadStream("sample.mp4");
    rs.pipe(response);
}; //video를 실행하게 하는 간단한 코드

// createServer
const server = http.createServer(app); //app이 비어있으면 안됨
// server.on listen
server.on('error', (err) => {
	console.error('Server error:', err);
});
server.listen(port, function(){
	console.log('listening on *:' + port);
    });
    

//V만 사용하는 코드
var fs = require('fs');





