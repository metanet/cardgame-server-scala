package com.basrikahveci
package cardgame.card

import com.weiglewilczek.slf4s.Logger
import java.io.{File, FileInputStream}


trait ImageLoader {

  def imageNames = FileSystemImageLoader.Instance.imageNames

  def image(name: String) = FileSystemImageLoader.Instance.image(name)

  def background = FileSystemImageLoader.Instance.background

}

object FileSystemImageLoader {
  val BackgroundImagePath = "images/background.jpg"

  val CardImagesPath = "images/cards"

  val Instance = new FileSystemImageLoader
}

class FileSystemImageLoader {

  private val logger = Logger(classOf[FileSystemImageLoader])

  private var _background: Array[Byte] = null

  private val images = scala.collection.mutable.Map[String, Array[Byte]]()

  init

  private def init {
    try {
      val in = new FileInputStream(FileSystemImageLoader.BackgroundImagePath)
      _background = new Array[Byte](in.available())
      in.read(_background)
      in.close

      for (eachFile <- new File(FileSystemImageLoader.CardImagesPath).listFiles) {
        val fin = new FileInputStream(eachFile)
        val imageContent = new Array[Byte](fin.available())
        fin.read(imageContent)
        fin.close
        images(eachFile.getName.take(10)) = imageContent
      }
    } catch {
      case e: Exception =>
        logger.error("[loading-card-images-failed]", e)
    }

    logger info "[card-images-loaded] Image Names: " + images.keys
  }

  def image(name: String) = images(name)

  def imageNames = images.keys

  def background = _background
}
