var createSurvey = function() {
	var surveyDiv = document.createElement("div");
    surveyDiv.style.left = "250px";
	surveyDiv.style.top = "100px";
	
	var textDiv = document.createElement("text");
	textDiv.innerHTML = "<p>Do you think you played with a human or a computer?</p>";
	surveyDiv.appendChild(textDiv);

	var human = document.createElement("button");
    human.innerHTML = "Human";
    
    var computer = document.createElement("button");
    computer.innerHTML = "Computer";

    human.onclick = function() {
        var msg = message_writer.surveyResponseMsg(MessageFields.HUMAN);
        connection.Send(msg);   
    };
    computer.onclick = function() {
        var msg = message_writer.surveyResponseMsg(MessageFields.COMPUTER);
        connection.Send(msg);   
    };
    surveyDiv.appendChild(human);
    surveyDiv.appendChild(computer);
    return surveyDiv;	
};