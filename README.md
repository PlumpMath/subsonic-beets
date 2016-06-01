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

Automatic deployment will be managed through Heroku and its GitHub integration.

Testing will be done with the help of Travis.
