package controllers

import play.api.mvc.{Action, Controller}
import models.{Problem, ProblemSet, User}
import play.api.libs.json.{JsString, JsBoolean, JsObject}

/**
 * The problem controller used for creating, deleting, and updating problems.
 */
object Problems extends Controller {
  def create = Action(parse.urlFormEncoded) {
    implicit request =>
      val user = User.current

      // Check that the user is logged in
      if (user.isDefined) {

        // Get the problem set
        val problemSet = ProblemSet.findById(request.body("problemSet")(0).toLong).get

        // Make sure the user owns the problem set
        if (user.get.problemSets.contains(problemSet)) {
          val problem = Problem.createFromRequest.save
          problemSet.addProblem(problem).save
          Ok(JsObject(Seq("success" -> JsBoolean(value = true))))

        } else // User doesn't own problem set
          NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("User doesn't own the problem set."))))
      } else // Not logged in
        Unauthorized(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Not logged in."))))
  }

  def delete(id: Long) = Action {
    implicit request =>
      val user = User.current

      // Check that the user is logged in
      if (user.isDefined) {

        // Make sure the problem exists
        val problem = Problem.findById(id)
        if (problem.isDefined) {

          // Get the problem set it belongs to
          val problemSet = user.get.problemSets.find(p => p.problems.contains(problem.get))
          if (problemSet.isDefined) {

            // Delete the problem and remove it from the problem set
            problem.get.delete()
            problemSet.get.removeProblem(problem.get).save
            Ok(JsObject(Seq("success" -> JsBoolean(value = true))))

          } else // Not in problem set
            NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Problem not in user owned problem sets."))))
        } else // Problem doesn't exist
          NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Problem doesn't exist."))))
      } else // Not logged in
        Unauthorized(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Not logged in."))))
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
          val problemSet = user.get.problemSets.find(p => p.problems.contains(problem.get))
          if (problemSet.isDefined) {
            problem.get.updateFromRequest.save
            Ok(JsObject(Seq("success" -> JsBoolean(value = true))))

          } else // Not in problem set
            NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Problem not in user owned problem sets."))))
        } else // Problem doesn't exist
          NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Problem doesn't exist."))))
      } else // Not logged in
        Unauthorized(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Not logged in."))))
  }
}
