# WebRTClojure

[![Build Status](https://travis-ci.org/Rovanion/WebRTClojure.svg?branch=master)](https://travis-ci.org/Rovanion/WebRTClojure)

This project indents to implement real time JSON data communication between browsers.


## Technical specification

In order to communicate directly between browsers one has to establish WebRTC-sockets through signalling managed by some central server. Put simply: The browsers has to be able to adress each other.

```
        WebRTC
Browser‾‾‾‾‾‾‾‾Browser
 \                /
  \ HTTP         /
   \ WebSockets /
    \          /
       Server
```

Specifically this will be achieved through a server written in Clojure using the following libraries:

	The web programming framework Ring.
	The web server HTTPKit.


On the client side we will use ClojureScript and the following technologies:

	The client framework ReactJS through the ClojureScript library Reagent.
	The development tool figwheel - live programming!

The following libraries will be used on both the front and back ends:

	The routing library bidi.
	The HTTP and WebSocket connection handler Sente.


Automatic deployment will be managed through Heroku and its GitHub integration.

Testing will be done with the help of Travis.
