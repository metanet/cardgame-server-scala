package com.basrikahveci

import cardgame.card.FileSystemImageLoader
import cardgame.core.db.QueryEvaluatorContainer
import cardgame.server.{HttpHandler, ServerPipelineFactory}
import com.weiglewilczek.slf4s.Logger
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import org.streum.configrity.Configuration


object Server {

  val logger = Logger("Server")

  def main(args: Array[String]) {

    logger info "Starting CardGame Server..."

    val config = Configuration.load("server.properties")

    logger info "Config is read."

    QueryEvaluatorContainer.init(config[String]("db.url"), config[String]("db.name"), config[String]("db.user"), config[String]("db.pw"))

    logger info "DB is initialized."

    FileSystemImageLoader.Instance

    logger info "Cards are loaded."

    val server = new org.eclipse.jetty.server.Server(8080)
    server.setHandler(new HttpHandler)
    server.start

    logger info "HTTP Server is set up."

    val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()))
    bootstrap.setPipelineFactory(new ServerPipelineFactory)

    bootstrap.bind(new InetSocketAddress(config[Int]("port")))

    logger info "Network is set up."

  }

}
