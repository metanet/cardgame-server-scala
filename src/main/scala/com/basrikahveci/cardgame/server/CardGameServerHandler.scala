package com.basrikahveci
package cardgame.server

import org.jboss.netty.channel._
import com.weiglewilczek.slf4s.Logger
import cardgame.core.Session
import cardgame.messaging.{Request, MessageFactory}
import cardgame.messaging.request.SignInRequest

class CardGameServerHandler extends SimpleChannelUpstreamHandler {

  val logger = Logger(classOf[CardGameServerHandler])

  val messageFactory = MessageFactory.Instance

  override def channelConnected(ctx: ChannelHandlerContext, event: ChannelStateEvent) {
    logger info "Channel connected. Channel Id: " + ctx.getChannel.getId
  }

  override def channelClosed(ctx: ChannelHandlerContext, event: ChannelStateEvent) {
    ctx.getAttachment.asInstanceOf[Session] close false
    ctx setAttachment null

    logger info "Channel closed. Channel Id: " + ctx.getChannel.getId
  }

  override def channelOpen(ctx: ChannelHandlerContext, event: ChannelStateEvent) {
    val channel = ctx.getChannel
    ctx setAttachment new Session(channel)

    logger info "Channel Opened. Channel Id: " + channel.getId
  }

  override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent) {
    val message = event.getMessage.asInstanceOf[String]

    if (message startsWith "<policy-file-request/>") {
      val allowAccessFrom = "*"
      val coreProtocolPort = "*"

      val ALLOW_ACCESS_FROM_TAG = new StringBuilder("<?xml version=\"1.0\"?><!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\"><cross-domain-policy><allow-access-from domain=\"").append(allowAccessFrom).append("\" to-ports=\"").append(coreProtocolPort).append("\" /></cross-domain-policy>").toString()

      ctx.getChannel.write(ALLOW_ACCESS_FROM_TAG)
    } else {
      logger info "Request Received. Channel Id: " + ctx.getChannel.getId + " , Message: " + message

      messageFactory.decode(message) match {
        case Some(request) =>
          handleRequest(ctx.getAttachment.asInstanceOf[Session], request)
        case None =>
          ctx.getAttachment.asInstanceOf[Session] close true
      }
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, evt: ExceptionEvent) {
    handleException(ctx.getAttachment.asInstanceOf[Session], evt.getCause)
  }

  def handleRequest(session: Session, request: Request) {
    try {
      request match {
        case _: SignInRequest =>
          request.handle(session, null)
        case _ if session.isUserSignedIn =>
          request.handle(session, session.user)
        case _ =>
          logger.error("[unauthenticated-request] Request Class: " + request.getClass)
          session close true
      }
    } catch {
      case e: Exception =>
        handleException(session, e)
    }
  }

  def handleException(session: Session, throwable: Throwable) {
    logger.error("[exception-handled] ", throwable)
    if (session != null) {
      session close true
    }
  }

}
