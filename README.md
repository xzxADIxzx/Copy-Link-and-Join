# Copy Link and Join
Provides the ability to transfer packets between Mindustry clients for network play.

## How to use
Everything you need to connect is in my [Scheme Size](https://github.com/xzxADIxzx/Scheme-Size) mod.
Just host the game and create a room in a special dialog next to the open server button, then copy the link and send it to your friend who can connect using it.

## How to host server
Download the repository and compile on any version of Java using `./gradlew jar` command. Start the server with the command `java -jar Copy-Link-and-Join.jar port` and replace port with the one you need.

## Local definitions
host - the player who hosted the game and created a room on the server.   
server - a remote machine that transmits packets from a client to a host.   
client - a player connecting via a link to a host through a server.   
link - a string containing the room key, ip and port of the server.   
room - an object that exists only on the server, serving as a container for connections to the host and client.