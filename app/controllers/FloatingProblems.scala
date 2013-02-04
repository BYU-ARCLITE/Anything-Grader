package controllers

import play.api.mvc.{Action, Controller}
import models.{Problem, User}
import anorm.NotAssigned
import play.api.libs.json.{JsString, JsBoolean, JsObject}

/**
 * Created with IntelliJ IDEA.
 * User: camman3d
 * Date: 2/4/13
 * Time: 11:32 AM
 * To change this template use File | Settings | File Templates.
 */
object FloatingProblems extends Controller {

  def create = Action(parse.urlFormEncoded) {
    implicit request =>
      val user = User.current
      val name = request.body("name")(0)

      // Check that the user is logged in
      if (user.isDefined) {
        val problem = Problem(NotAssigned, name, List(), 'single)
        user.get.addFloatingProblem(problem).save
        Redirect(routes.ProblemSets.dashboard()).flashing("success" -> ("Floating problem " + name + " created."))

      } else // Not logged in
        Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def edit(id: Long) = Action {
    implicit request =>
      val user = User.current

      // Check that the user is logged in
      if (user.isDefined) {

        // Check that the problem exists
        val problem = user.get.floatingProblems.find(_.id.get == id)
        if (problem.isDefined) {
          Ok(views.html.floatingProblems.edit(problem.get))

        } else // Doesn't exist
          Redirect(routes.ProblemSets.dashboard()).flashing("error" -> "The problem doesn't exist")
      } else // Not logged in
        Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def save(id: Long) = Action(parse.urlFormEncoded) {
    implicit request =>
      val user = User.current

      // Check that the user is logged in
      if (user.isDefined) {

        // Make sure the problem exists
        val problem = Problem.findById(id)
        if (problem.isDefined) {

          // Make sure the problem belongs to the user
          if (user.get.floatingProblems.contains(problem.get)) {
            problem.get.updateFromRequest.save
            Ok(JsObject(Seq("success" -> JsBoolean(value = true))))

          } else // Not in problem set
            NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Problem not in user owned problem sets."))))
        } else // Problem doesn't exist
          NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Problem doesn't exist."))))
      } else // Not logged in
        Unauthorized(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Not logged in."))))
  }

  def delete(id: Long) = Action {
    implicit request =>
      val user = User.current

      // Check that the user is logged in
      if (user.isDefined) {

        // Check that the problem exists
        val problem = user.get.floatingProblems.find(_.id.get == id)
        if (problem.isDefined) {

          // Delete the problem
          problem.get.delete()
          user.get.removeFloatingProblem(problem.get).save
          Redirect(routes.ProblemSets.dashboard()).flashing("success" -> "The problem was deleted")

        } else // Doesn't exist
          Redirect(routes.ProblemSets.dashboard()).flashing("error" -> "The problem doesn't exist")
      } else // Not logged in
        Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

}
