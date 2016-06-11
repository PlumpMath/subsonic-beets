# WebRTClojure

[![Build Status](https://travis-ci.org/Rovanion/WebRTClojure.svg?branch=master)](https://travis-ci.org/Rovanion/WebRTClojure)

This project indents to implement real time JSON data communication between browsers.


## Dependencies

First install Clojure through your favourite package manager:

```
sudo apt-get install clojure1.6 postgresql
```

Then install [Leiningen](http://leiningen.org/#install).

You now have to populate the database before starting up the server, unless postgress blindly trust all connections from localhost you'll have to provide a database URL heroku style:

```
export DATABASE_URL="postgresql://username:password@localhost:5432/webrtclojure"
```


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
