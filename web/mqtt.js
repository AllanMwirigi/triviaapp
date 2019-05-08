
var clientId = 'TriviaApp';
// var host = 'm12.cloudmqtt.com';
// var port = 35080;

/*********** public broker ************/
var host = 'broker.mqttdashboard.com';
var port = 8000;
/*************************************** */

// var host = 'localhost';
// var port = 5000;

var subTopic = 'sestrivia/client';
var pubTopic = 'sestrivia/admin';
var client = new Paho.MQTT.Client(host, port, clientId);

var sq1 = document.getElementById('sq1');
var sq2 = document.getElementById('sq2');
var sq3 = document.getElementById('sq3');
var sq4 = document.getElementById('sq4');
var sq5 = document.getElementById('sq5');	
var sqList = [sq1, sq2, sq3, sq4, sq5];

function startSession(){
	client.onConnectionLost = onConnectionLost;
    client.onMessageArrived = onMessageArrived;
    client.connect({ 
        onSuccess: onConnect,
        onFailure: onFailure
	});
	for(var i = 0; i < sqList.length; i++){
		sqList[i].textContent = '';		
	}
}

function onConnect(){
	client.subscribe(subTopic);
	var teams = localStorage.getItem("teams");
	sendMessage('start:'+teams);
	console.log('MQTT', 'connected');
}

function onMessageArrived(message){
	var payload = message.payloadString;
	var parts = payload.split(",");
	for(var i = 0; i < sqList.length; i++){
		if(sqList[i].textContent.length === 0){
			// sqList[i].textContent = message.payloadString;
			sqList[i].innerHTML = parts[0] + "<br/>" + parts[1];
			break;
		}
	}
	// console.log("onMessageArrived: "+ payload);
}

function resetSession(){
	sendMessage('reset');
	for(var i = 0; i < sqList.length; i++){
		sqList[i].textContent = '';		
	}
}

function onConnectionLost(response){
	alert('Server disconnected');
	console.log('MQTT', 'connection lost');
	if (response.errorCode !== 0) {
		console.log("onConnectionLost: "+response.errorMessage);
	  }
}

function stopSession() {
	sendMessage('stop');
	client.disconnect();
	for(var i = 0; i < sqList.length; i++){
		sqList[i].textContent = '';		
	}
	console.log('MQTT', 'disconnected successfully');
}

function onFailure(){
	console.log('MQTT', 'failure');
}

function sendMessage(value){
	var message = new Paho.MQTT.Message(value);
	message.destinationName = pubTopic;
	client.send(message);
}

