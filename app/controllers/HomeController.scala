package controllers

import javax.inject._

import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import play.api.mvc._
import streams.{ChatFlow, Message}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(chatFlow: ChatFlow, cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }


  def chat(roomId: String) = WebSocket.accept(request => {
    Flow.fromFunction(Message)
      .via(chatFlow.flow(roomId))
      .map(_.content)
  })
}
