var createSurvey = function(connection, message_writer) {
	var surveyDiv = document.createElement("div");
    surveyDiv.style.left = "250px";
	surveyDiv.style.top = "100px";
	
	var textDiv = document.createElement("text");
	textDiv.innerHTML = "<p>On a scale of 1 to 7, how human was your partner?</p>";
	surveyDiv.appendChild(textDiv);

	var human = document.createElement("button");
    human.innerHTML = "Human";
    
    var computer = document.createElement("button");
    computer.innerHTML = "Computer";

    human.onclick = function() {
        var msg = message_writer.surveyResponseMsg(MessageFields.HUMAN);
        surveyDiv.remove();
        connection.Send(msg);
        window.location.reload();
    };
    computer.onclick = function() {
        var msg = message_writer.surveyResponseMsg(MessageFields.COMPUTER);
        surveyDiv.remove();
        connection.Send(msg); 
        window.location.reload();  
    };
    surveyDiv.appendChild(human);
    surveyDiv.appendChild(computer);
    return surveyDiv;	
};