function A(){}var G="agent_type",H="why_error",N="heartbeat",G="agent_type",H="why_error",N="heartbeat";function O(){function a(b){var d;return"name"in b&&"value"in b&&"x"in b&&"y"in b?d=new P(b.name,b.x,b.y):d}function e(b){var d;if(!("name"in b&&"wallType"in b&&"pos"in b&&"end1"in b&&"end2"in b))return d;d=b.pos;return d=new Q(b.name,d,b.end1,d,b.end2+1)}function c(b){var d;if(!("name"in b&&"wallType"in b&&"pos"in b&&"end1"in b&&"end2"in b))return d;d=b.pos;return d=new Q(b.name,b.end1,d,b.end2+1,d)}function f(b){var d;return"name"in b&&"gt"in b&&"x"in b&&"y"in b?d=new aa(b.name,b.gt,b.x,b.y):d}function l(b){var d;
return"name"in b&&"playerNum"in b&&"x"in b&&"y"in b?d=new ba(b.name,b.playerNum,b.x,b.y):d}function h(b,d,a){for(var e=[],c=0;c<b.length;c++)if(b[c]["class"]===d){var h=a(b[c]);"undefined"!==typeof h&&null!==h?e.push(h):console.log("Could not parse %O",b[c])}return e}var g=this;this.V=function(b){var d;"msg_type"in b&&(d=b.msg_type);return d};this.A=function(b){var d;"client_id"in b&&(d=b.client_id);return d};this.R=function(b){var d;if(!("agent"in b&&"state"in b&&"world_type"in b))return d;d={};
d.ga=b.agent;d.state=g.u(b.state);d.ma=b.world_type;d.ready=b.is_ready;return d};this.aa=function(b){var d;if(!("update"in b))return d;b=b.update;if(!("state"in b))return d;d={};"score"in b&&(d.ea=b.score);"action"in b&&(console.log("Joint actions:"),console.log(b.action.joint_action),d.action=b.action.joint_action);d.state=g.u(b.state);return d};this.B=function(b){var d;if(!("score"in b))return d;d={};d.ea=b.score;return d};this.u=function(b){var d=h(b,"agent",l),g=h(b,"goal",f),q=[],u=h(b,"dimensionlessHorizontalWall",
c),w=h(b,"dimensionlessVerticalWall",e),q=q.concat(u,w);h(b,"reward",a);b=new ca(d,g,q);console.log("State %O",b);return b};this.ha=function(b){var d=[];if("worlds"in b){b=b.worlds;for(var a=0;a<b.length;a++){var e;e=b[a];e="agents"in e&&"label"in e&&"description"in e?new da:void 0;"undefined"!==typeof e&&d.push(e)}}};this.i=function(b){var d=[];if(!("active"in b))return d;b=b.active;for(var a=0;a<b.length;a++)d.push(new ea(b[a].agents));return d}};function fa(){this.u=function(a,e){var c={msg_type:"join_game"};c.world_id=a;c.client_id=e;return c};this.i=function(a,e,c,f,l,h){var g={msg_type:"reaction_time"};g.client_id=a;g.agent_name=e;g.url_id=c;g.reaction_time=f;g.game_number=l;g.action_number=h;return g};this.A=function(a,e){var c={msg_type:"take_action"};c.action=a;c.action_params=[e];return c};this.B=function(a,e,c,f,l){var h={msg_type:"run_url_game"};h.world_id=a;h.agents=e;h[G]="human";h.url_id=c;h.exp_name=f;a={};for(var g in l)g in
h||(a[g]=l[g]);h.other_vars=a;return h}};function ga(a){this.i=a}function ha(a,e,c){for(var f=0;f<a.i.O.length;f++){var l=a.i.O[f];if(String(l.location)==String(e)&&l.fa==c)return!0}return!1}function ia(a,e){var c=a;switch(e){case "left":c=[a[0]-1,a[1]];break;case "right":c=[a[0]+1,a[1]];break;case "up":c=[a[0],a[1]+1];break;case "down":c=[a[0],a[1]-1];break;case "wait":c=a}return c};function W(){if(W.prototype.ka)return GameSceneMatch.prototype.ka;var a="ws://localhost:8787/events/",e=[],c=[],f=0,l=null;this.R=function(e){a=e};this.A=function(a){if("string"!==typeof a)return console.log("URL value is not a string"),!1;a=a.split(":");if(3!==a.length)return console.log("URL needs to be of the form ws://server_url:port/web_socket_handler_name/"),!1;if("ws"!==a[0])return console.log("URL needs to be a websocket address (use ws://)"),!1;a=a[2].split("/");return 2>a.length?(console.log("URL needs a port and a events handler name"),
!1):isNaN(a[0])?(console.log("URL does not contain a valid port"),!1):0==a[1].length?(console.log("URL does not contain an appropriate events handler name"),!1):!0};this.B=function(){l=new WebSocket(a);l.onopen=function(){console.log("Connection to websocket opened!");for(var a=0;a<e.length;a++)e[a].la()};l.onmessage=function(a){if('"{}"'!==JSON.stringify(a.data)){a=JSON.parse(a.data);console.log("Websocket Message from server %O",a);"client_id"in a&&(f=a.client_id);for(var c=0;c<e.length;c++)e[c].onMessage(a)}};
l.onclose=function(){console.log("Connection to websocket closed!");for(var a=0;a<e.length;a++);};l.onerror=function(a){console.log("Websocket Error: "+a);for(a=0;a<e.length;a++);}};this.u=function(a){var f=0;0<c.length&&(f=c[c.length-1]+1);e.push(a);c.push(f)};this.i=function(a){a.client_id=f;var e=JSON.stringify(a);a.msg_type!==N&&console.log("Sending: "+e);l.send(e)}};window.fnMain=function(){console.log($("#task_display"));(new ja).i()};
function ja(){function a(a){console.log("In experiment_complete");$(window).bind("beforeunload",function(){return"Please do not close or reload this window before completing the task. Doing so will invalidate your responses!"});var b=$.proxy(function(){var b=$.proxy(function(){console.log("Drawing final screen");var a=q;a.u=a.paper.add([{type:"rect",x:0,y:0,width:a.width,height:a.height,stroke:"black",fill:"black"}])[0];a.A=a.paper.add([{type:"text",x:a.width/2,y:a.height/2,text:"End of Task","font-size":a.width/
8,fill:"white"}])[0]},this),d=$.proxy(function(){var b=R+"&found_partner="+(!a).toString();console.log("Redirecting to: "+b);$(location).attr("href",b)},this);setTimeout(b,5E3);console.log("Drawing done");setTimeout(d,1E4);console.log("Redirecting done")},this);$(window).unbind("beforeunload");setTimeout(b,5E3/3)}function e(a){for(var b={},d,e=0;e<a.F.length;e++){var c={};d=a.F[e];c.name=d.D;c.location=[d.X,d.Y];c.type="agent";b[d.D]=c}return b}function c(a){for(var b={},d=0;d<a.length;d++){var e=
a[d].agent,c=a[d].action;switch(c){case "west":c="left";break;case "north":c="up";break;case "east":c="right";break;case "south":c="down";break;case "noop":c="wait";break;default:return}b[e]=c}return b}function f(){B&&a(!0)}function l(a){console.log("Running hello: vars length "+p.length);w=d.A(a);a=d.i(a);if(0<p.length)h(),console.log("Ran on URL");else{for(var e in a)return;console.log("No games initialized. Initialize some game first")}setInterval(function(){var a={};a.msg_type=N;b.i(a)},1E4)}
function h(){console.log(" running on URL");C=p.t_id;R=p.redirect+"/?SID="+p.SID+"&t_id="+C;if(null!=p.human&&null!=p.game){var a=p.game;"yes"==p.human&&(I=["human","human"])}else{var a=null,d=p.exp_name;I=null}console.log("Running based on URL");a=t.B(a,I,C,d,p);console.log(a);b.i(a)}function g(){var a=document.getElementById("text_box");"undefined"!==typeof a&&null!==a&&(a=a.value,isNaN(a)||(a=t.u(a,w),console.log("Sending from submit: "+a),b.i(a)))}var b=new W,d=new O,t=new fa,q,u,w,J,S,y=0,T,
D,E=!1,F=0,R,B=!1,p={},z=document.URL.split("?")[1];if(void 0!=z)for(var z=z.split("&"),x=0;x<z.length;x++){var U=z[x].split("=");p[U[0]]=U[1]}p.length=z.length;console.log("Vars: %O",p);var K;"server"in p&&(K=p.server);var C,V=!1,I=["human","random"],ka=this,L=0,M=0;this.da=function(a){console.log("On action press");L++;switch(a.which){case 37:a="west";break;case 38:a="north";break;case 39:a="east";break;case 40:a="south";break;case 32:a="noop";break;default:return}var d=+new Date-F;console.log("reaction time");
console.log(d);b.i(t.i(w,J,C,d,M,L));$(document).unbind("keydown.gridworld");var e=t.A(a,J);"noop"==a?u.html("You are the "+q.J+" player! <br> Your Score: "+y+"<br> You waited.<br> Waiting on other player to move..."):u.html("You are the "+q.J+" player! <br> Your Score: "+y+"<br> You went "+a+".<br> Waiting on other player to move...");$.proxy(function(){console.log(e);b.i(e)},this)()};this.i=function(){console.log(" running go");$(document).bind("keydown.gridworld",this.da);b.A(K)&&b.R(K);b.u(ka);
b.B();if(0==p.length){q=new la;document.createElement("div").setAttribute("id","divvyDiv");var a=document.getElementById("text_box"),d=document.getElementById("submit_button");d.onclick=g;q.i(a,d)}var c=$.proxy(function(){var a=e(S);console.log("Printing again %O",a);X(q,a)});$(window).focus(function(){setTimeout(c,10)})};this.onMessage=function(b){A.Error in b&&!0===b.Error&&console.log(b[H]);var c=d.V(b);if(null!=c&&"undefined"!==typeof c)switch(d.ha(b),d.i(b),console.log(c),c){case "hello":l(b);
break;case "initialize":b=d.R(b);console.log(b);if("undefined"!==typeof b&&!V){console.log("Initializing game "+b.ma);J=b.ga;var h=b.state;console.log(JSON.stringify(h));for(var c={},n=h.ba,m=0,r=0;r<n.length;r++)n[r].I>m&&(m=n[r].I);c.height=m;n=h.ba;for(r=m=0;r<n.length;r++)n[r].G>m&&(m=n[r].G);c.width=m;for(var n=h.ba,m=c.width,r=c.height,g=[],k,w=0;w<n.length;w++)if(k=n[w],!(0==k.K&&0==k.G||k.K==m&&k.G==m||0==k.N&&0==k.I||k.N==r&&k.I==r))if(k.K==k.G)for(x=k.N;x<k.I;x++){var p=[k.K,x,"left"];g.push([k.K-
1,x,"right"]);g.push(p)}else if(k.N==k.I)for(x=k.K;x<k.G;x++)p=[x,k.N,"down"],g.push([x,k.N-1,"up"]),g.push(p);c.U=g;console.log(c.U);n=h.ja;m=h.F;r=[];for(k=0;k<n.length;k++){g=n[k];w={};for(p=0;p<m.length;p++)m[p].Number==g.ia-1&&(w.fa=m[p].D);w.location=[g.X,g.Y];r.push(w)}c.O=r;c.S=[];for(n=0;n<h.F.length;n++)m={},m.name=h.F[n].D,c.S.push(m);console.log(c);h=e(b.state);T=new ga(c);c=q=new Y(c);m=$("#task_display")[0];n=b.ga;n="undefined"!==typeof n?n:"agent1";c.width=150*c.j.width;c.height=150*
c.j.height;c.paper=Raphael(m,c.width,c.height);c.P={};for(r=m=0;r<c.j.S.length;r++)c.j.S[r].name==n?c.P[c.j.S[r].name]=c.J:(c.P[c.j.S[r].name]=c.R[m],m++);c.U={};c.O={};for(n=0;n<c.j.width;n++)for(m=0;m<c.j.height;m++){r={type:"rect",x:150*n,y:c.height-150*(m+1),width:150,height:150,stroke:"black"};for(g=0;g<c.j.O.length;g++)k=c.j.O[g],c.O[k.location]=k,String(k.location)===String([n,m])&&(r.fill=c.P[k.fa]);for(g=0;g<c.j.U.length;g++)if(k=c.j.U[g],String([k[0],k[1]])==String([n,m]))if(c.U[k]=1,2==
k.length)r.fill="black";else if(3==k.length){var v,t;switch(k[2]){case "up":v=[0,1];t=[1,1];break;case "down":v=[0,0];t=[1,0];break;case "left":v=[0,0];t=[0,1];break;case "right":v=[1,0],t=[1,1]}v=[150*(n+v[0]),150*(c.j.height-(v[1]+m))];t=[150*(n+t[0]),150*(c.j.height-(t[1]+m))];c.paper.path("M"+v.join(" ")+"L"+t.join(" ")).attr({"stroke-width":10,stroke:"black"})}c.paper.add([r])}$(q.paper.canvas).css({display:"block",margin:"auto"});X(q,h);D=h;F=+new Date;u=$("#messages");u.html("You are the "+
q.J+" player! <br> Your Score: "+y+"<br>      <br>        <br>        ");"false"==b.ready&&(B=!0,v=q,v.u=v.paper.add([{type:"rect",x:0,y:0,width:v.width,height:v.height,stroke:"black",fill:"black"}])[0],v.A=v.paper.add([{type:"text",x:v.width/2,y:v.height/2,text:"Waiting for partner to join...","font-size":v.width/30,fill:"white"}])[0],setTimeout(f,3E5));V=!0}break;case "action_sorely_needed":break;case "update":this.u(b);break;case "game_complete":"undefined"!==typeof d.B(b)&&(E=!0,console.log("Round "+
M+" ended"),M++,L=0);break;case "experiment_complete":a(!1);break;default:console.log("Unknown message type "+c)}};this.la=function(){console.log("Connected to server")};this.u=function(a){B&&(ma(q),B=!1);console.log("Running update_game with msg below");console.log(a);a=d.aa(a);"undefined"!==typeof a&&(S=a.state,"undefined"!==typeof a.ea&&(y=a.ea));console.log(" RUNNING MARKS UPDATE CODE");if(null!=a.action){console.log(a.na);var b=c(a.action),f=e(a.state);console.log("current actions:");console.log(b);
b=na(q,D,b,f,T);u.html("You are the "+q.J+" player! <br> Your Score: "+y+"<br>        <br>        <br>        ");D=f;f=$.proxy(function(){F=+new Date;if(E){var a=$.proxy(function(){var a=q;a.u=a.paper.add([{type:"rect",x:0,y:0,width:a.width,height:a.height,stroke:"black",fill:"black"}])[0];a.A=a.paper.add([{type:"text",x:a.width/2,y:a.height/2,text:"End of round","font-size":a.width/8,fill:"white"}])[0]},this),b=$.proxy(function(){var a=q;a.u.animate({opacity:0},250);a.A.animate({opacity:0},250);
$(document).bind("keydown.gridworld",this.da)},this);setTimeout(a,2500);setTimeout(b,5E3)}else $(document).bind("keydown.gridworld",this.da)},this);setTimeout(f,b)}E&&(a=function(a){return function(){X(q,a);F=+new Date;D=a;u.html("You are the "+q.J+" player! <br> Your Score: "+y+"<br>        <br>        <br>        ");E=!1}}(e(a.state)),a=$.proxy(a,this),setTimeout(a,5E3))}};function Y(a){this.R=["blue","green","red"];this.J="orange";this.B=45;this.V=this.aa=.7;this.j=a;this.i={}}function X(a,e){for(var c in e)if(e.hasOwnProperty(c)&&(c=e[c],"agent"==c.type)){var f={cx:150*(c.location[0]+.5),cy:150*(a.j.height-c.location[1]-.5)};a.i.hasOwnProperty(c.name)?a.i[c.name].attr(f):(f.type="circle",f.fill=a.P[c.name],f.r=a.B,f.stroke="white",f["stroke-width"]=1,f=a.paper.add([f])[0],a.i[c.name]=f)}}
function na(a,e,c,f,l){var h;console.log("------------");X(a,e);var g=0,b={},d;for(d in e)"agent"===e[d].type&&(b[d]="undefined"==typeof l?f[d]:ia(e[d].location,c[d]));for(d in e)if(e.hasOwnProperty(d)&&"agent"===e[d].type)if("wait"==c[d])console.log(d+" waits"),oa(a,d);else if(String(b[d])!==String(f[d].location)){console.log(d+" tried and failed");var t=.2,q=.2,u;for(u in e)!e.hasOwnProperty(u)||"agent"!==e[u].type||d==u||String(b[d])!=String(f[u].location)&&String(b[d])!=String(b[u])||String(b[d])==
String(f[u].location)&&String(b[u])==String(f[d].location)||(console.log(d+" collided with "+u),t=a.V,q=a.aa);pa(a,d,b,e,q,t)}else console.log(d+" makes normal movement"),console.log("object images %O",a.i),t=Raphael.animation({cx:150*(f[d].location[0]+.5),cy:150*(a.j.height-f[d].location[1]-.5)},150,"easeInOut"),a.i[d].animate(t),$.subscribe("killtimers",Z(a.i[d],t));g+=150;"undefined"===typeof h&&(h=function(a,b,c){a=a.paper.add([{type:"text",x:150*(b[0]+.5),y:150*(a.j.height-b[1]-.5),text:"Home",
"font-size":40,stroke:a.P[c],fill:"yellow"}])[0];b=Raphael.animation({y:a.attr("y")-75,opacity:0},800);a.animate(b);$.subscribe("killtimers",Z(a,b))});c=!1;for(d in e){if("undefined"===typeof l)break;e=function(a,b,c){return function(){h(a,b,c)}}(a,f[d].location,d);ha(l,f[d].location,d)&&(e=setTimeout(e,150),$.subscribe("killtimers",function(a){return function(){clearTimeout(a)}}(e)),c=!0)}c&&(g+=800);return g}
function oa(a,e){var c=function(a,c,e,g){return function(){var a=Raphael.animation({r:e},g,"backOut");c.animate(a);$.subscribe("killtimers",Z(c,a))}}(a,a.i[e],a.i[e].attr("r"),75),c=Raphael.animation({r:.9*a.i[e].attr("r")},75,"backIn",c);a.i[e].animate(c);$.subscribe("killtimers",Z(a.i[e],c))}
function pa(a,e,c,f,l,h){var g=function(a,b,c,e,f){return function(){var a=Raphael.animation({cx:c,cy:e},f,"backOut");b.animate(a);$.subscribe("killtimers",Z(b,a))}}(a,a.i[e],a.i[e].attr("cx"),a.i[e].attr("cy"),150*l),b=c[e][0]*h+f[e].location[0]*(1-h);c=(a.j.height-c[e][1])*h+(a.j.height-f[e].location[1])*(1-h);l=Raphael.animation({cx:150*(b+.5),cy:150*(c-.5)},150*(1-l),"backIn",g);a.i[e].animate(l);$.subscribe("killtimers",Z(a.i[e],l))}function Z(a,e){return function(){a.stop(e)}}
Y.prototype.remove=function(){this.paper.remove()};function ma(a){a.u.animate({opacity:0},250);a.A.animate({opacity:0},250)};function ba(a,e,c,f){this.D=a;this.Number=e;this.X=c;this.Y=f}function aa(a,e,c,f){this.D=a;this.ia=e;this.X=c;this.Y=f}function Q(a,e,c,f,l){this.D=a;this.K=e;this.N=c;this.G=f;this.I=l}function P(a,e,c){this.D=a;this.X=e;this.Y=c}function ca(a,e,c){this.F=a;this.ja=e;this.ba=c}function da(){}function ea(a){this.F=a};function la(){var a=document.getElementById("GGCanvas").getContext("2d");this.i=function(e,c){a.fillStyle="black";a.fillRect(0,0,768,512);e.style.left=384;e.style.bottom=256;e.style.align="center";e.style.position="absolute";c.style.left=384;c.style.bottom=226;c.style.align="center";c.style.position="absolute"}};
