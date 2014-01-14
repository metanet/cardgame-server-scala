package com.basrikahveci
package cardgame.server

import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.Request
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import cardgame.card.ImageLoader


class HttpHandler extends AbstractHandler with ImageLoader {

  def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {

    val imageName = request.getParameter("img")

    if (imageName != null) {
      response setContentType "image/png"
      val out = response.getOutputStream
      out write image(imageName)
      out.close
    } else if (request.getParameter("bg") != null) {
      response setContentType "image/jpeg"
      val out = response.getOutputStream
      out write background
      out close
    }
  }

}
