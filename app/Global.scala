import play.api._
import mvc.RequestHeader

object Global extends GlobalSettings {

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error("Play Error: " + ex.getMessage)
    play.api.mvc.Results.BadRequest
  }

  override def onStart(app: Application) {
    createFixtures()
  }

  def createFixtures() {

  }
}
