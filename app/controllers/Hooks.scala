package controllers

import play.api.mvc.{Action, Controller}
import models.{Hook, ProblemSet, User}

/**
 * The controller for creating, deleting, and updating hooks
 */
object Hooks extends Controller {
  def create = Action(parse.urlFormEncoded) {
    implicit request =>
      val user = User.current

      // Check that the user is logged in
      if (user.isDefined) {

        // Get the problem set
        val problemSet = ProblemSet.findById(request.body("problemSet")(0).toLong).get

        // Make sure the user owns the problem set
        if (user.get.problemSets.contains(problemSet)) {
          val hook = Hook.createFromRequest.save
          problemSet.addHook(hook).save
          Redirect(routes.ProblemSets.editHooks(problemSet.id.get)).flashing("success" -> "Hook created")

        } else // User doesn't own problem set
          Redirect(routes.ProblemSets.dashboard()).flashing("error" -> "Problem set doesn't exist")
      } else // Not logged in
        Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def delete(id: Long) = Action {
    implicit request =>
      val user = User.current

      // Check that the user is logged in
      if (user.isDefined) {

        // Make sure the hook exists
        val hook = Hook.findById(id)
        if (hook.isDefined) {

          // Get the problem set it belongs to
          val problemSet = user.get.problemSets.find(p => p.hooks.contains(hook.get))
          if (problemSet.isDefined) {

            // Delete the problem and remove it from the problem set
            hook.get.delete()
            problemSet.get.removeHook(hook.get).save
            Redirect(routes.ProblemSets.editHooks(problemSet.get.id.get)).flashing("success" -> "Hook deleted")

          } else // Not in problem set
            Redirect(routes.ProblemSets.editHooks(problemSet.get.id.get)).flashing("error" -> "Hook doesn't exist")
        } else // Problem doesn't exist
          Redirect(routes.ProblemSets.dashboard()).flashing("error" -> "Problem set doesn't exist")
      } else // Not logged in
        Redirect(routes.Application.index()).flashing("info" -> "You're not logged in")
  }

  def save(id: Long) = Action(parse.urlFormEncoded) {
    implicit request =>
      val user = User.current

      // Check that the user is logged in
      if (user.isDefined) {

        // Make sure the problem exists
        val hook = Hook.findById(id)
        if (hook.isDefined) {

          // Make sure the problem belongs to the user
          val problemSet = user.get.problemSets.find(p => p.problems.contains(hook))
          if (problemSet.isDefined) {
            hook.get.updateFromRequest.save
            Ok // TODO: Respond with success message

          } else // Not in problem set
            Ok // TODO: Respond with error message
        } else // Problem doesn't exist
          Ok // TODO: Respond with error message
      } else // Not logged in
        Ok // TODO: Respond with error message
  }
}
