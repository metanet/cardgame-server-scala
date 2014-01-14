cardgame-server-scala
=====================

Scala Server code of https://apps.facebook.com/hafiza_oyunu/

I wrote this code on Feb'13 to port my Cardgame server's Java codebase to Scala (2.9.2). 

I have been learning Scala from some books such as Programming in Scala, Scala for the Impatient, Scala in Depth etc. My main motivation for this project was applying the things I had learned about Scala to a real project and practicing new paradigms like functional programming, actor-based programming etc. Being far far away from idiomatic Scala code, most of the code actually looks like Java. But still, it was a good start and great fun! 

Please note that this is not a production-ready code base, just a hobby project. 

You can see how I applied actor-based programming to a turn-based game. Basic game-related objects like User, Game, Bot are actors. Other things like OnlineUsers, GameManager are also actors. 

I think you can develop any turn-based game with actor-based programming very easily.

Networking layer is built on top of Netty, nothing interesting. 

Have fun.




