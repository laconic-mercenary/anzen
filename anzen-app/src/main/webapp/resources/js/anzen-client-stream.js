
var anzenClientStream = anzenClientStream || function() { };

anzenClientStream._sck = undefined;
anzenClientStream.onOpen = undefined;
anzenClientStream.onClose = undefined;
anzenClientStream.onMessage = undefined;
anzenClientStream.onError = undefined;

anzenClientStream.openSocket = anzenClientStream.openSocket || function(socketPath) {
	var socket = new WebSocket('ws://' + socketPath);
	socket.onopen = anzenClientStream.onOpen;
	socket.onclose = anzenClientStream.onClose;
	socket.onerror = anzenClientStream.onError;
	socket.onmessage = anzenClientStream.onMessage;
	anzenClientStream._sck = socket;
};

anzenClientStream.send = anzenClientStream.send || function(message) {
	anzenClientStream._sck.send(message);
};
