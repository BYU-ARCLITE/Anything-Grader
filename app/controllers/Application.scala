package controllers

import play.api.mvc._
import tools.{Grader, OAuthTools}
import com.google.gdata.client.authn.oauth._
import models.Problem
import anorm.NotAssigned

object Application extends Controller {

  def index = Action {
    implicit request =>
      Ok(views.html.index())
  }

  def test = Action {
    implicit request =>

      val problem = Problem(NotAssigned, List("Red", "Yellow", "Blue"), 'multiple, 1, 1, caseModifier = false,
        punctuationModifier = false, wordOrderModifier = false, responseOrderModifier = true)
      val responses = List("Yellow", "Blue")
      val grade = Grader.grade(responses, problem)

      Ok(grade.grade.toString)
  }

  def testPost = Action {
    implicit request =>

      val parameters = OAuthTools.createOauthParametersFromRequest(request)
      parameters.setOAuthConsumerSecret("dmb8kph3fqke9eccjtnffgnntr")

      import collection.JavaConversions._
      val baseString = OAuthTools.getSignatureBaseString(routes.Application.testPost().absoluteURL(), "POST", parameters.getBaseParameters.toMap)
      //val signature = OAuthTools.generateSignature(routes.Application.testPost().absoluteURL(), "POST", parameters)


      Ok(request.body.toString + " ====== " + baseString)
    //Ok(baseString)
  }

}