
const writeEvent = (text) => {
	// <ul> element
	const parent = document.querySelector('#events');
	
	// <li> element
	const el = document.createElement('li');
	el.innerHTML = text;

	parent.appendChild(el);
};

const onFormSubmitted = (e) => {
	e.preventDefault();

	const input = document.querySelector('#chat'); // find 'chat' in html 
	const text = input.value; // save text in text variable
	input.value = '';
	
	sock.emit('message', text);
};

const addButtonListeners = () => {
	['rock', 'paper', 'scissors'].forEach((id) => {
		const button = document.getElementById(id); // get the button
		button.addEventListener('click', () => {
			sock.emit('turn', id); // show message what I chose (send turn to rps-game.js)
		});
	});	
};

writeEvent('Welcome to RSP');

const sock = io();
sock.on('message', writeEvent);

document
	.querySelector('#chat-form')
	.addEventListener('submit', onFormSubmitted);

addButtonListeners();
