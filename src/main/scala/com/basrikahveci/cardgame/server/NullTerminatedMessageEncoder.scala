package com.basrikahveci
package cardgame.server

import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}

import org.jboss.netty.buffer.ChannelBuffers.{wrappedBuffer, copiedBuffer}
import org.jboss.netty.buffer.ChannelBuffer

class NullTerminatedMessageEncoder extends OneToOneEncoder {
  def encode(ctx: ChannelHandlerContext, channel: Channel, msg: Any) = wrappedBuffer(msg.asInstanceOf[ChannelBuffer], copiedBuffer(Array[Byte](0)))
}
