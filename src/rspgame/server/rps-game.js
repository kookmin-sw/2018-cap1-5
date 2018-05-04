

class RpsGame {
	
	constructor(p1, p2) {
		this._players = [p1, p2];
		this._turns = [null, null];

		//[p1, p2].forEach(s => s.emit('message', 'Rock Scissor Paper Starts!')); // send message to two guys
		this._sendToPlayers('Rock Scissor Paper Starts!');

		this._players.forEach((player, idx) => {
			player.on('turn', (turn) => {
				this._onTurn(idx, turn);
			});
		});
	}
	
	_sendToPlayer(playerIndex, msg) {
		this._players[playerIndex].emit('message', msg);
	}

	_sendToPlayers(msg) { // send message to two players
		this._players.forEach((player) => {
			player.emit('message', msg);
		});
	}

	_onTurn(playerIndex, turn) {
		this._turns[playerIndex] = turn;
		this._sendToPlayer(playerIndex, `You selected ${turn}`);		
		
		this._checkGameOver();
	}
	

	_checkGameOver() {
		const turns = this._turns;

		if (turns[0] && turns[1]) {
			this._sendToPlayers('Game Over -> ' + turns.join(' : '));
			this._getGameResult();
			this._turns = [null, null];
			this._sendToPlayers('Next Round!!');
			//const el = document.getElementById('events');
			//el.scrollTop = el.scrollHeight;
		}
	}

	_getGameResult() {

		const p0 = this._decodeTurn(this._turns[0]);
		const p1 = this._decodeTurn(this._turns[1]);

		const distance = (p1 - p0 + 3) % 3;

		switch (distance) {
			case 0:
				this._sendToPlayers('Draw!');
				break;
			case 1: // p0 won
				this._sendWinMessage(this._players[0], this._players[1]);
				break;	
			case 2:
				this._sendWinMessage(this._players[1], this._players[0]);
				break;	
		}
	}

	_sendWinMessage(winner, loser) {
		winner.emit('message', 'You won!');
		loser.emit('message', 'You lost.');
	}
	
	_decodeTurn(turn) {

		switch (turn) {
			case 'rock':
				return 0;
			case 'scissors':
				return 1;
			case 'paper':
				return 2;
			default:
				throw new Error(`Could not decode ${turn}`);
		}
	}
}

module.exports = RpsGame; // export RpsGame
