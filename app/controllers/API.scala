package controllers

import play.api.mvc.{Request, Action, Controller}
import models.{Problem, ProblemSet, GradeSession}
import anorm.NotAssigned
import java.util.Date
import play.api.libs.json._
import tools._
import play.api.libs.ws.WS
import com.ning.http.client.Realm.AuthScheme
import com.google.gdata.client.authn.oauth.OAuthUtil
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.Logger

/**
 * This is the controller for the three different API endpoints. They are:
 * 1. Start a session
 * 2. Grade a problem in an ongoing session
 * 3. Finish a session and send scores out to registered hooks
 */
object API extends Controller {

  /**
   * This creates a random hex string to be used as the access token
   * @return A random hex string
   */
  def createToken: String = Hasher.md5Hex(util.Random.nextString(32))

  /**
   * This is the endpoint which starts a grading session.
   * Expected parameters:
   * - userInfo: String, A description about the user. Not used for grading purposes, just for the administrator
   * - problemSet: Long, The ID of the problem set being taken
   * @return An HTTP response.
   */
  def startSession = Action(parse.urlFormEncoded) {
    implicit request =>
      try {
        val userInfo = request.body("userInfo")(0)
        val problemSet = ProblemSet.findById(request.body("problemSet")(0).toLong)

        // Check that the problem set exists
        if (problemSet.isDefined) {
          val session = GradeSession(
            NotAssigned, problemSet.get, List(), new Date().getTime, 0, userInfo, createToken
          ).save
          Ok(JsObject(Seq(
            "success" -> JsBoolean(value = true),
            "sessionId" -> JsNumber(session.id.get),
            "accessToken" -> JsString(session.token)
          ))).as("application/json")

        } else {
          Logger.error("No problem set with ID = " + request.body("problemSet")(0))
          NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("The problem set was not found")))).as("application/json")
        }
      } catch {
        case error: Throwable => {
          Logger.error(error.getMessage)
          BadRequest(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("An unexpected error occured")))).as("application/json")
        }
      }
  }

  /**
   * This is the endpoint which grades a problem in an ongoing session.
   * Expected parameters:
   * - sessionId: Long, The ID of the active session
   * - accessToken: String, The latest given access token
   * - problemId: Long, The ID of the problem being graded
   * - responses: String, A JSON formatted string of the responses to the problem
   * @return An HTTP response.
   */
  def grade = Action(parse.urlFormEncoded) {
    implicit request =>
      try {

        // Check that the session is real and active
        val session = GradeSession.findById(request.body("sessionId")(0).toLong)
        if (session.isDefined && session.get.finished == 0) {

          // Check that the auth token matches
          val token = request.body("accessToken")(0)
          if (token == session.get.token) {

            // Check that the problem is real, is part of the active session, and not already graded
            val problem = Problem.findById(request.body("problemId")(0).toLong)
            if (problem.isDefined && session.get.problemSet.problems.contains(problem.get) &&
              session.get.responseData.filter(d => d.problem == problem.get).isEmpty) {

              // Grade the problem
              val responses = Json.parse(request.body("responses")(0)).as[List[String]]
              Logger.debug("(API - grade) Got responses")
              val data = Grader.grade(responses, problem.get)
              Logger.debug("(API - grade) Problem graded")
              val updatedSession = session.get.addResponseData(data).setToken(createToken).save
              Logger.debug("(API - grade) Session updated")

              // Return the result
              Ok(JsObject(Seq(
                "success" -> JsBoolean(value = true),
                "score" -> JsNumber(data.grade),
                "possible" -> JsNumber(problem.get.getPointsPossible),
                "scaled" -> JsNumber(data.grade / problem.get.getPointsPossible),
                "accessToken" -> JsString(updatedSession.token)
              ))).as("application/json")

            } else {
              Logger.error("Invalid problem id")
              BadRequest(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Invalid problem id")))).as("application/json")
            }
          } else {
            Logger.error("Bad access token")
            Unauthorized(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Bad access token")))).as("application/json")
          }
        } else {
          Logger.error("The session id was invalid")
          NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("The session id was invalid")))).as("application/json")
        }
      } catch {
        case error: Throwable => {
          Logger.error(error.getMessage)
          BadRequest(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("An unexpected error occured")))).as("application/json")
        }
      }
  }

  /**
   * This is the endpoint which closes a session to grading and reports grades by sending them through registered hooks.
   * Expected Parameters:
   * - sessionId: Long, The ID of the active session to close
   * - accessToken: String, The latest given access token
   * - Additional parameters can be passed in which will be used in creating the response data
   * @return An HTTP response.
   */
  def finishSession = Action(parse.urlFormEncoded) {
    implicit request =>
      try {

        // Check that the session is real and not finished
        val session = GradeSession.findById(request.body("sessionId")(0).toLong)
        if (session.isDefined && session.get.finished == 0) {

          // Check that the auth token matches
          val token = request.body("accessToken")(0)
          if (token == session.get.token) {
            session.get.setFinished(new Date().getTime).save
            Logger.debug("(API - finishSession) Session Finished")

            // Send grades out
            sendGrades(session.get)
            Logger.debug("(API - finishSession) Grades sent out")

            Ok(JsObject(Seq(
              "success" -> JsBoolean(value = true),
              "score" -> JsNumber(session.get.getScore),
              "possible" -> JsNumber(session.get.problemSet.getPointsPossible),
              "scaled" -> JsNumber(session.get.getScaledScore)
            ))).as("application/json")

          } else {
            Logger.error("Bad access token")
            Unauthorized(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("Bad access token")))).as("application/json")
          }
        } else {
          Logger.error("The session id was invalid")
          NotFound(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("The session was not found")))).as("application/json")
        }
      } catch {
        case error: Throwable => {
          Logger.error(error.getMessage)
          BadRequest(JsObject(Seq("success" -> JsBoolean(value = false), "message" -> JsString("An unexpected error occured")))).as("application/json")
        }
      }
  }

  /**
   * This function creates the WS requests and sends them according to the hook type.
   * @param session The session object which contains the score and hooks.
   * @param request The Play Framework request object. This is used to get the POST parameters used for formatting the
   *                additional data
   * @return Unit
   */
  def sendGrades(session: GradeSession)(implicit request: Request[Map[String, Seq[String]]]) {
    import collection.JavaConversions._

    // For each hook
    for (hook <- session.problemSet.hooks) {

      // Create the request
      var wsRequest = WS.url(hook.uri).withHeaders("Content-Type" -> hook.contentType)
      Logger.debug("(API - sendGrades) WS request created")

      // Create the data to be sent
      val score = if (hook.scaled) session.getScaledScore else session.getScore
      var data = ""
      if (hook.method == "POST") {
        val context = request.body.map(stuff => (stuff._1, stuff._2(0))) + ("grade" -> score.toString) +
          ("random" -> util.Random.nextInt(32).toString)
        data = new Mustache(hook.additionalData).render(context)
      }
      Logger.debug("(API - sendGrades) Data created")

      // Check the authentication and sign
      // HTTP Authentication
      if (hook.authScheme.authType == "http") {
        wsRequest = wsRequest.withAuth(hook.authScheme.publicKey, hook.authScheme.privateKey, AuthScheme.BASIC)
        Logger.debug("(API - sendGrades) HTTP Auth set up")
      }

      // OAuth 1.0a signing
      if (hook.authScheme.authType == "oauth") {

        // Include the data. Check if we are posting a url encoded data string
        var params = Map[String, String]()
        if (hook.method == "POST" && hook.contentType == "application/x-www-form-urlencoded")
          params = OAuthUtil.parseQuerystring(data).toMap
        else
          params = Map("oauth_body_hash" -> Hasher.sha1Base64(data))

        // Sign it
        val oauthParameters = OAuthTools.generateOauthParameters(params, (hook.authScheme.publicKey, hook.authScheme.privateKey))
        val authorization = OAuthTools.getAuthorizationHeader(hook.uri, hook.method, oauthParameters)
        wsRequest = wsRequest.withHeaders("Authorization" -> authorization)
        Logger.debug("(API - sendGrades) OAuth 1.0a signed")
      }

      // Send the request
      if (hook.method == "POST") {
        val response = wsRequest.post(data).await.get
        Logger.debug("LMS response: " + response.body)
      } else
        wsRequest.withQueryString("grade" -> grade.toString()).get()
      Logger.debug("(API - sendGrades) WS request sent")
    }
  }
}
