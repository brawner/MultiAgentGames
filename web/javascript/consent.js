var StartMenuText = "<p>Can you determine whether you are playing with a human or a computer?</p>" + 
				  "<p>Here you can play a simple two-player cooperative game. If you can, find a " + 
				  " partner to play on the other tablet. If you don't have a partner, " + 
				  " we will connect you with a partner from Amazon Mechanical Turk.<p>" + 
				  "<p>Please select your choice below";

var ConsentText = "<p>In common speech, consent occurs when one person voluntarily agrees to the proposal or desires of another.[1] The concept of consent has been operationalized in several major contexts, including in law, medicine and sexual relationships. Types of consent include implied consent, expressed consent, informed consent and unanimous consent. Consent as understood in legal contexts may differ from the everyday meaning. For example, a person under the Age of sexual consent may willingly engage in a sexual act; but that consent is not valid in a legal context.</p>";

var createConsent = function(callback) {
	var consentDiv = document.createElement("div");
	consentDiv.style.width = "640px";
	var consent = document.createElement("text");
    consent.innerHTML = ConsentText;
    consent.style.position = "relative";
    consent.style.left = "10%";
    consent.style.top = "0%";
    consent.style.width = "640px";

    consentDiv.appendChild(consent);

    var button = document.createElement("button");
    button.style.textAlign = 'center';
    button.style.position = "absolute";
    button.style.left = "480px";
    button.innerHTML = "I agree";

    button.onclick = function() {
        consentDiv.remove();
        callback();
        var game = new Game();
        game.go();  
    };
    consentDiv.appendChild(button);
    return consentDiv;
};

var createStartMenu = function(callback) {
	var startDiv = document.createElement("div");
	startDiv.style.width = "640px";
	
	var text = document.createElement("text");
    text.innerHTML = StartMenuText;
    text.style.position = "relative";
    text.style.left = "10%";
    text.style.top = "0%";
    text.style.width = "640px";

    startDiv.appendChild(text);

    var button1 = document.createElement("button");
    button1.style.textAlign = 'center';
    button1.style.position = "absolute";
    button1.style.left = "360px";
    button1.innerHTML = "1 Player";

    var button2 = document.createElement("button");
    button2.style.textAlign = 'center';
    button2.style.position = "absolute";
    button2.style.left = "480px";
    button2.innerHTML = "2 Player";

    button1.onclick = function() {
    	startDiv.remove();
        callback(1)  
    };

    button2.onclick = function() {
    	startDiv.remove();
        callback(2)  
    };

	startDiv.appendChild(button1);
    startDiv.appendChild(button2);
    return startDiv;
};