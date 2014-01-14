package com.basrikahveci
package cardgame.server

import org.jboss.netty.channel.{ChannelPipeline, ChannelPipelineFactory}
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder
import org.jboss.netty.handler.codec.string.{StringEncoder, StringDecoder}
import org.jboss.netty.handler.execution.{OrderedMemoryAwareThreadPoolExecutor, ExecutionHandler, OrderedDownstreamThreadPoolExecutor}
import java.util.concurrent.TimeUnit
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

object ServerPipelineFactory {
  private val FRAME_DELIMITER = ChannelBuffers.wrappedBuffer(Array[Byte](0))

  private val CORE_POOL_SIZE = 10

  private val threadPoolExecutor = new OrderedMemoryAwareThreadPoolExecutor(CORE_POOL_SIZE, 0, 0, 30, TimeUnit.SECONDS)

  private val downStreamThreadPoolExecutor = new OrderedDownstreamThreadPoolExecutor(CORE_POOL_SIZE, 60, TimeUnit.SECONDS)
}


class ServerPipelineFactory extends ChannelPipelineFactory {
  def getPipeline: ChannelPipeline = {
    val pipeline = org.jboss.netty.channel.Channels.pipeline

    pipeline.addLast("orderedDownStreamHandler", new ExecutionHandler(ServerPipelineFactory.downStreamThreadPoolExecutor, true, false))
    pipeline.addLast("framer", new DelimiterBasedFrameDecoder(1024 * 1024, ServerPipelineFactory.FRAME_DELIMITER))
    pipeline.addLast("nullEncoder", new NullTerminatedMessageEncoder)
    pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8))
    pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8))
    pipeline.addLast("threadPoolExecutor", new ExecutionHandler(ServerPipelineFactory.threadPoolExecutor))
    pipeline.addLast("server", new CardGameServerHandler)

    pipeline
  }
}
