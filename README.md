# WebRTClojure

This project indents to implement real time JSON data communication between browsers.


## Technical specification

In order to communicate directly between browsers one has to establish WebRTC-sockets through signalling managed by some central server. Put simply: The browsers has to be able to adress each other.

```
        WebRTC
Browser ------ Browser
|              |
\ HTTP         /
 \ WebSockets /
  \          /
     Server
```	 

Specifically this will be achieved through a server written in Clojure using the following libraries:

	The web server framework Ring.
	The web server HTTPKit.
	The routing library Secretary.
	
	
On the client side we will use ClojureScript and the following technologies:

	The client framework ReactJS through the ClojureScript library Reagent.

The management of WebSockets on both the client and server side will be with the help of a library called Sente.	
	
Automatic deployment will be managed through Heroku and its GitHub integration.

Testing will be done with the help of Travis.
	
