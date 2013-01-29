package controllers

import play.api.mvc.{Action, Controller}
import models.User
import anorm.NotAssigned
import tools.Hasher

/**
 * The authentication controller. Used for signing up and logging in
 */
object Auth extends Controller {
  def signup = Action { implicit request =>
    Ok(views.html.auth.signup())
  }

  def createAccount = Action(parse.urlFormEncoded) { implicit request =>
    val username = request.body("username")(0)
    val password = request.body("password")(0)
    val password2 = request.body("password2")(0)

    // Check that the username isn't already taken
    val userCheck = User.findByUsername(username)
    if (!userCheck.isDefined) {

      // Check that the passwords match
      if(password == password2) {
        val user = User(NotAssigned, username, Hasher.sha256Base64(password), List()).save
        Redirect(routes.ProblemSets.dashboard()).flashing("success" -> ("Welcome " + user.username + "!"))
          .withSession("username" -> user.username)

      } else // Passwords don't match
        Redirect(routes.Auth.signup()).flashing("info" -> "Passwords don't match")
    } else // Username already taken
      Redirect(routes.Auth.signup()).flashing("info" -> "That username is already taken")
  }

  def login = Action { implicit request =>
    Ok(views.html.auth.login())
  }

  def authenticate = Action(parse.urlFormEncoded) { implicit request =>
    val username = request.body("username")(0)
    val password = Hasher.sha256Base64(request.body("password")(0))

    // Check that the username isn't already taken
    val user = User.authenticate(username, password)
    if (user.isDefined) {
      Redirect(routes.ProblemSets.dashboard()).flashing("success" -> ("Welcome " + user.get.username + "!"))
        .withSession("username" -> user.get.username)

    } else // Invalid credentials
      Redirect(routes.Auth.login()).flashing("info" -> "Invalid username/password.")
  }
}
