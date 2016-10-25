var StartMenuText = "<p>Can you determine whether you are playing with a human or a computer?</p>" + 
				  "<p>Here you can play a simple two-player cooperative game. If you can, find a " + 
				  " partner to play on the other tablet. If you don't have a partner, " + 
				  " we will connect you with a partner from Amazon Mechanical Turk.<p>" + 
				  
                  "<p>When the game starts, you will be randomly assigned to play with the designated partner " +
				  " or an intelligent computer agent trained to play with humans </p>" + 

                  "<p>Your goal is to arrive at your goal square at the same time. If you do, you and your partner " + 
                  " score a point. If you don't, neither agent will score. You must cooperate with your partner to " + 
                  " successfully solve the game.</p>" + 

                  "<p>Important! Do not talk to your partner while you are playing. Please be silent</p>"

				  "<p>Please select your choice below";

var PilotConsentText = "<p>This experiment investigates how people make decisions in environments containing " + 
                  "multiple agents. In the experiment, you will be an agent in a virtual environment that " +
                  "may contain one or more other agents (whose behavior may be determined by another participant " + 
                  "or a computer algorithm). Depending on the study, you will have a score that depends on your " + 
                  "and other agents' actions in the environment. The experiment is a Brown University research " + 
                  "study.</p>" +

                  "<p>Currently, this study is only in a pilot study phase. Your participation will only be used " + 
                  " to better inform our study at a later date. If you have any information, you may contact " + 
                  " Stephen Brawner (stephen_brawner@brown.edu).</p>" +

                  "<p>Do you agree to participate in our pilot study?</p>"

var ConsentText = "<p>This experiment investigates how people make decisions in environments containing " + 
                  "multiple agents. In the experiment, you will be an agent in a virtual environment that " +
                  "may contain one or more other agents (whose behavior may be determined by another participant " + 
                  "or a computer algorithm). Depending on the study, you will have a score that depends on your " + 
                  "and other agents’ actions in the environment. The experiment is a Brown University research " + 
                  "study that will take place over a single session of at most one hour and take place at your " + 
                  "current location. You will be paid a base payment of $2.00 for your participation with a bonus " + 
                  "up to $1.00 contingent on your performance on the task.</p>" + 

                  "<p>All data collected by us will be kept confidential to the extent of the law. We may ask " + 
                  "you demographic information, such as your age, gender, level of education, and ethnicity. " + 
                  "All experiments conducted online are hosted on a secure server. Your data will be maintained " + 
                  "on a password-protected database. To preserve confidentiality, you will be assigned a random " + 
                  "number (that is in no known way connected to this form) and you will automatically be identified " + 
                  "by that number for the remainder of the experiment. Furthermore, any data stored on a computer will " + 
                  "reference you only by this number. Your responses may be shown to future participants; however, it will " + 
                  "be confidential and devoid of any identifying information. Only investigators delegated to this study " + 
                  "will have access to any identifiable information and it will not be shared with third parties. " + 
                  "In general, neither we nor anyone can absolutely guarantee the security of data transmitted over the web.</p>" + 

                  "<p>Any questions concerning this research can be directed to Joseph Austerweil, Cognitive, Linguistic, " + 
                  "and Psychological Sciences, Box 1821, Brown University; phone (401) 863-9758. If you have any questions " + 
                  "about your rights as a human participant, contact the Research Protections Office at (401) 863-3050.</p> "+ 

                  "<p>You will be put at no known risk in this experiment. There is no expected direct personal benefit " + 
                  "from participation. The experimenter would be happy to answer any questions you might have. Your " + 
                  "participation in this experiment is entirely voluntary. You may refuse to participate or discontinue " + 
                  "your participation at any time without suffering penalty or any loss of benefit to which you are" +  
                  "otherwise entitled. However, you will only be paid if you complete the task.</p>" + 

                  "<p>I understand the above and hereby agree to serve as a research participant for this experiment. " + 
                  "I am 18 years of age or older.</p><p>By clicking Next below, you agree to the above statement.</p>";

var createConsent = function(callback) {
	var consentDiv = document.createElement("div");
	consentDiv.style.width = "640px";
	var consent = document.createElement("text");
    consent.innerHTML = PilotConsentText;
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
    button1.innerHTML = "No Partner";

    var button2 = document.createElement("button");
    button2.style.textAlign = 'center';
    button2.style.position = "absolute";
    button2.style.left = "480px";
    button2.innerHTML = "I have a partner";

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