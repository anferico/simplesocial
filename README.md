# Simple Social
A simple Social Network with a command line interface (CLI) that exploits Java's TCP and UDP sockets, as well as RMI (Remote Method Invocation).

In order to use the social network, users must be registered. Registered users can then:
 - Log in
 - Search for a user by providing a search key
 - Send/accept friendship request to/from other users
 - Post textual contents
 - Follow another user, in order to be notified of the content they post
 - Log out

The application consists of two main modules: `socialclient` and `socialserver`. The purpose of each of them is described in the sections below.

## Social Client
The `socialclient` module handles the communication with the user by providing a CLI. In fact, since this is a thin client, the user's requests are forwarded to the social server, which is responsible for fulfilling them. Apart from handling the communication with the user, the social client joins a multicast group for replying to keep-alive messages sent by the server.

## Social Server
The `socialserver` module has complete control over the whole social network. It handles registration requests, stores all the users and their friendship relationships, handles contents posted by users and checks the state (online or offline) of every user.

## Implementation details
Keep-alive messages are sent by the server over a **UDP** multicast socket. In order for a user to be notified of new contents published by another user they follow, its social client module registers a **RMI** callback, which is called each time new content is available for that user. All the remaining functionalities require a **TCP** connection to be estabilished between a social client and the social server. 

## Usage
Run [socialserver/SocialServer.java](socialserver/SocialServer.java) to start the server. You can optionally pass an argument that specifies the number of hours a friendship request can stay in the "pending" state before it is deleted automatically.

Once the server is running, you can start as many instances of the client as you want (one for each user) by running [Program.java](Program.java). At this point, you can test the functionalities of the social network.
