package controllers

import play.api.mvc._
import tools.{Grader, OAuthTools}
import com.google.gdata.client.authn.oauth._
import models.{User, Problem}
import anorm.NotAssigned
import play.api.libs.ws.WS
import play.api.libs.json.{JsValue, JsArray}

object Application extends Controller {

  def index = Action {
    implicit request =>
      Ok(views.html.index())
  }

  def test = TODO

  def extract(username: String) = Action {
    implicit request =>

      var user = User.findByUsername(username).get
      var count = 0

      // Get all exercise groups
      val exerciseGroups = WS.url("http://sartre3.byu.edu:5984/phtut_exercisegroup/_all_docs").get().await.get.json \ "rows"

      // For each group
      for (groupEntry <- exerciseGroups.as[List[JsValue]]) {
//      val groups = exerciseGroups.as[List[JsValue]]
//      for (i <- 0 until 10) {
//        val groupEntry = groups(i)

        // Not a design doc
        val groupId = (groupEntry \ "id").as[String]
        if (groupId.substring(0,1) != "_") {

          // Get the group
          val group = WS.url("http://sartre3.byu.edu:5984/phtut_exercisegroup/" + groupId).get().await.get.json
          val groupName = (group \ "name").as[String]

          // For each exercise
          for (exerciseId <- (group \ "exercises").as[List[String]]) {

            // Get the exercise
            val exercise = WS.url("http://sartre3.byu.edu:5984/phtut_exercise/" + exerciseId).get().await.get.json
            val exerciseName = (exercise \ "name").as[String]
            val name = groupName + " -- " + exerciseName

            // For each element
            for (element <- (exercise \ "elements").as[List[JsValue]]) {

              // If it's a question
              if ((element \ "elementType").as[String] == "question") {

                // Get the question
                val questionId = (element \ "value").as[String]
                val question = WS.url("http://sartre3.byu.edu:5984/phtut_question/" + questionId).get().await.get.json

                // Create the problem from the question
                user = createProblemFromQuestion(question, name, user)
                count += 1
              }
            }
          }
        }
      }

      user.save
      Ok(count + " problems created")
  }

  def createProblemFromQuestion(question: JsValue, name: String, user: User): User = {
    // Get the answers
    val answers = (question \ "answer").as[List[String]]

    // Get the problem type
    val variety = (question \ "variety").as[String]
    val problemType =
      if (variety == "dragAndDrop" || variety == "multipleAnswer" || variety == "multipleSelect" || variety == "plusMinus")
        'multiple
      else
        'single

    // Get the acceptance rate and case modifier
    val acceptanceRate = if (variety == "text" || variety == "textarea") 0.5 else 1.0
    val caseModifier = if (variety == "text" || variety == "textarea") true else false

    val problem = Problem(NotAssigned, name, answers, problemType, 1, acceptanceRate, caseModifier,
      punctuationModifier = false, wordOrderModifier = false, responseOrderModifier = true, gradientGradeMethod = false,
      subtractiveModifier = false, multipleGradeModifier = true)

    user.addFloatingProblem(problem)
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