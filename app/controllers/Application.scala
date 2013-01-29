package controllers

import play.api.mvc._
import tools.OAuthTools
import com.google.gdata.client.authn.oauth._

object Application extends Controller {

  def index = Action {
    implicit request =>
      Ok(views.html.index())
  }

  def test = Action {
    implicit request =>
    //    val template = new Mustache("This is a {{test}}")
    //    val s = template.render(Map("test" -> "cool thing"))


//      val parameters = Map(
//        "oauth_consumer_key" -> "dpf43f3p2l4k3l03",
//        "oauth_token" -> "nnch734d00sl2jdk",
//        "oauth_signature_method" -> "HMAC-SHA1",
//        "oauth_timestamp" -> "1191242096",
//        "oauth_nonce" -> "kllo9940pd9333jh",
//        "oauth_version" -> "1.0",
//        "file" -> "vacation.jpg",
//        "size" -> "original"
//      )

      val oauthParameters = new OAuthParameters()
      oauthParameters.setOAuthConsumerKey("dpf43f3p2l4k3l03")
      oauthParameters.setOAuthConsumerSecret("kd94hf93k423kf44")
      oauthParameters.setOAuthTokenSecret("pfkkdhi9sl3r4s00")
      oauthParameters.setOAuthSignatureMethod("HMAC-SHA1")
      oauthParameters.setOAuthToken("nnch734d00sl2jdk")
      oauthParameters.setOAuthNonce("kllo9940pd9333jh")
      oauthParameters.setOAuthTimestamp("1191242096")
      oauthParameters.addCustomBaseParameter("oauth_version", "1.0")
      oauthParameters.addCustomBaseParameter("file", "vacation.jpg")
      oauthParameters.addCustomBaseParameter("size", "original")


      //      val baseString = OAuthUtil.getSignatureBaseString("http://photos.example.net/photos", "GET", oauthParameters.getBaseParameters)

      val auth = OAuthTools.getAuthorizationHeader("http://photos.example.net/photos", "GET", oauthParameters)


      //      val signature = new OAuthHmacSha1Signer().getSignature(baseString, oauthParameters)

      //val oa = new OAuthParameters()
      //signer.set
      //      val oauthParameters = new GoogleOAuthHelper(signer)
      //      oauthParameters.s
      //      oauthParameters.setOAuthConsumerKey("dpf43f3p2l4k3l03")
      //      oauthParameters.setOAuthConsumerSecret("kd94hf93k423kf44&pfkkdhi9sl3r4s00")
      ////      val asdf = new com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer()
      //      oauthParameters.get



      Ok(auth)
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