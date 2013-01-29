package controllers

import play.api.mvc.{Action, Controller}
import models.{GradeSession, ProblemSet, User}
import anorm.NotAssigned
import play.api.libs.json.{JsString, JsBoolean, JsObject}

/**
 * Created with IntelliJ IDEA.
 * User: camman3d
 * Date: 1/23/13
 * Time: 12:56 PM
 * To change this template use File | Settings | File Templates.
 */
object ProblemSets extends Controller {

  def dashboard = Action { implicit request =>
    val user = User.current

    // Check that the user is logged in
    if (user.isDefined) {
      Ok(views.html.problemSets.dashboard(user.get))

    } else // Not logged in
      Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def grades(id: Long) = Action { implicit request =>
    val user = User.current

    // Check that the user is logged in
    if (user.isDefined) {

      // Check that the problem set exists
      val problemSet = user.get.problemSets.find(p => p.id.get == id)
      if (problemSet.isDefined) {
        val sessions = GradeSession.listByProblemSet(problemSet.get.id.get)
        Ok(views.html.problemSets.grades(problemSet.get, sessions))

      } else // User doesn't have that problem set
        Redirect(routes.ProblemSets.dashboard()).flashing("error" -> "You don't own a problem set with that ID.")
    } else // Not logged in
      Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def create = Action(parse.urlFormEncoded) { implicit request =>
    val user = User.current
    val name = request.body("name")(0)

    // Check that the user is logged in
    if (user.isDefined) {
      val problemSet = ProblemSet(NotAssigned, name, List(), List())
      user.get.addProblemSet(problemSet).save
      Redirect(routes.ProblemSets.dashboard()).flashing("success" -> ("Problem set " + name + " created."))

    } else // Not logged in
      Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def delete(id: Long) = Action { implicit request =>
    val user = User.current

    // Check that the user is logged in
    if (user.isDefined) {

      // Check that the problem set exists
      val problemSet = user.get.problemSets.find(p => p.id.get == id)
      if (problemSet.isDefined) {
        problemSet.get.delete()
        user.get.removeProblemSet(problemSet.get).save
        Redirect(routes.ProblemSets.dashboard()).flashing("success" -> "Problem set deleted.")

      } else // User doesn't have that problem set
        Redirect(routes.ProblemSets.dashboard()).flashing("error" -> "You don't own a problem set with that ID.")
    } else // Not logged in
      Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def edit(id: Long) = Action { implicit request =>
    val user = User.current

    // Check that the user is logged in
    if (user.isDefined) {

      // Check that the problem set exists
      val problemSet = user.get.problemSets.find(p => p.id.get == id)
      if (problemSet.isDefined) {
        Ok(views.html.problemSets.edit(problemSet.get))

      } else // User doesn't have that problem set
        Redirect(routes.ProblemSets.dashboard()).flashing("error" -> "Problem set doesn't exist")
    } else // Not logged in
      Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def editHooks(id: Long) = Action { implicit request =>
    val user = User.current

    // Check that the user is logged in
    if (user.isDefined) {

      // Check that the problem set exists
      val problemSet = user.get.problemSets.find(p => p.id.get == id)
      if (problemSet.isDefined) {
        Ok(views.html.problemSets.hooks(problemSet.get))

      } else // User doesn't have that problem set
        Redirect(routes.ProblemSets.dashboard()).flashing("error" -> "Problem set doesn't exist")
    } else // Not logged in
      Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def save(id: Long) = Action(parse.urlFormEncoded) { implicit request =>
    val user = User.current

    // Check that the user is logged in
    if (user.isDefined) {

      // Check that the problem set exists
      val problemSet = user.get.problemSets.find(p => p.id.get == id)
      if (problemSet.isDefined) {
        problemSet.get.updateFromRequest.save
        Ok(JsObject(Seq("success" -> JsBoolean(true))))

      } else // User doesn't have that problem set
        NotFound(JsObject(Seq("success" -> JsBoolean(false),"message" -> JsString("User doesn't own the problem set."))))
    } else // Not logged in
      Unauthorized(JsObject(Seq("success" -> JsBoolean(false),"message" -> JsString("Not logged in."))))
  }

}
