package tools

import com.google.gdata.client.authn.oauth._
import play.api.mvc.{AnyContent, Request}
import collection.JavaConversions._
import java.util
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * OAuth 1.0a message signing can be complicated. There are a lot of use cases. Also, the Play Framework's WS library
 * cannot sign POST requests with url encoded bodies. This is a bunch of tools to help with OAuth signatures.
 *
 * This requires the gdata client authentication library. To include it with sbt, use this dependency:
 * "com.google.gdata.gdata-java-client" % "gdata-client-1.0" % "1.47.0"
 * @author Joshua Monson
 */
object OAuthTools {

  /**
   * Create an OAuthParameters object from given data
   * @param parameters Additional base parameters
   * @param nonce The OAuth nonce
   * @param timestamp The OAuth timestamp
   * @param consumer The OAuth consumer key and secret
   * @param token The OAuth token key and secret (if no token then leave as empty strings)
   * @param signature The OAuth signature
   * @param signatureMethod The OAuth signature method
   * @return The OAuthParameters object with the data
   */
  def createOauthParameters(parameters: Map[String, String], nonce: String, timestamp: String,
                            consumer: (String, String), token: (String, String) = ("", ""), signature: String = "",
                            signatureMethod: String = "HMAC-SHA1"): OAuthParameters = {
    val oauthParameters = new OAuthParameters()

    // Set the consumer and token
    oauthParameters.setOAuthConsumerKey(consumer._1)
    oauthParameters.setOAuthConsumerSecret(consumer._2)
    if (!token._1.isEmpty) {
      oauthParameters.setOAuthToken(token._1)
      oauthParameters.setOAuthTokenSecret(token._2)
    }

    // Set the nonce, timestamp, signature, and signature method
    oauthParameters.setOAuthNonce(nonce)
    oauthParameters.setOAuthTimestamp(timestamp)
    oauthParameters.setOAuthSignature(signature)
    oauthParameters.setOAuthSignatureMethod(signatureMethod)

    // Set other stuff
    oauthParameters.addCustomBaseParameter("oauth_version", "1.0")

    // Set the parameters
    for ((key, value) <- parameters)
      oauthParameters.addCustomBaseParameter(key, value)

    oauthParameters
  }

  /**
   * This extracts oauth information from a Play Framework request and creates an OAuthParameters with the data
   * @param request The Play Framework request
   * @return The OAuthParameters object created from the request
   */
  def createOauthParametersFromRequest(request: Request[AnyContent]): OAuthParameters = {
    val parameters = getAllParameters(request)

    val nonce = parameters("oauth_nonce")
    val timestamp = parameters("oauth_timestamp")
    val token = parameters.get("oauth_token").getOrElse("")
    val consumer = parameters("oauth_consumer_key")
    val signature = parameters("oauth_signature")
    val signatureMethod = parameters("oauth_signature_method")

    val omitted = Set("oauth_nonce", "oauth_timestamp", "oauth_token", "oauth_consumer_key", "oauth_signature", "oauth_signature_method")
    val otherParameters = parameters.filterKeys(key => !omitted.contains(key))

    createOauthParameters(otherParameters, nonce, timestamp, (consumer, ""), (token, ""), signature, signatureMethod)
  }

  /**
   * Because OAuth information can be located in the Authorization header, the query string, or in the POST body as
   * url-encoded data, it can get messy getting all the data. This aggregates all the parameters together.
   * @param request The Play Framework request from which the parameters will be extracted and aggregated
   * @return A map of all the parameters
   */
  def getAllParameters(request: Request[AnyContent]): Map[String, String] = {
    // Aggregate the parameters. They can be in authorization header, post body, or query string
    var parameters = Map[String, String]()

    // Check the authorization header
    if (request.headers.get("Authorization").isDefined) {
      parameters = request.headers("Authorization").substring(0, 6).split(",").map(s => {
        val parts = s.split("=")
        (parts(0), parts(1).replaceAll("\"", ""))
      }).filterNot(p => p._1 == "realm").toMap
    }

    // Check the post body
    if (request.body.asFormUrlEncoded.isDefined)
      parameters = parameters ++ request.body.asFormUrlEncoded.get.map(p => (p._1, p._2(0)))

    // Check the query string
    parameters = parameters ++ request.queryString.map(p => (p._1, p._2(0)))

    parameters
  }

  /**
   * This creates an OAuthParameters object to be used when signing a WS request.
   * @param parameters Additional parameters to include
   * @param consumer The consumer key/secret
   * @param token The token key/secret
   * @return The newly created OAuthParameters object
   */
  def generateOauthParameters(parameters: Map[String, String], consumer: (String, String), token: (String, String) = ("", "")): OAuthParameters = {
    val nonce = OAuthUtil.getNonce
    val timestamp = OAuthUtil.getTimestamp
    createOauthParameters(parameters, nonce, timestamp, consumer, token)
  }

  /**
   * This creates the message signature given the url, method, and parameters
   * @param url The URL the request is going to
   * @param method The HTTP method
   * @param oauthParameters The OAuth parameters
   * @return The signature
   */
  def generateSignature(url: String, method: String, oauthParameters: OAuthParameters): String = {
    val baseString = getSignatureBaseString(url, method, oauthParameters.getBaseParameters.toMap)
    val signatureMethod = oauthParameters.getOAuthSignatureMethod
    if (signatureMethod == "HMAC-SHA1")
      //new OAuthHmacSha1Signer().getSignature(baseString, oauthParameters)
      generateHmacSha1Signature(baseString, oauthParameters.getOAuthConsumerSecret, oauthParameters.getOAuthTokenSecret)
    else
      throw new OAuthException("Unsupported signature method")
  }

  /**
   * This creates the HMAC-SHA1 signature
   * @param baseString The signature base string
   * @param consumer The consumer key which will be used to sign
   * @param token The token key which will be used to sign. Can be blank
   * @return The HMAC-SHA1 signature
   */
  def generateHmacSha1Signature(baseString: String, consumer: String, token: String): String = {
    val key = OAuthUtil.encode(consumer) + "&" + OAuthUtil.encode(token)
    val mac = Mac.getInstance("HmacSHA1")
    val secret = new SecretKeySpec(key.getBytes, "HmacSHA1")
    mac.init(secret)
    val digest = mac.doFinal(baseString.getBytes)
    new sun.misc.BASE64Encoder().encode(digest)
  }

  /**
   * This creates the authorization header to be included with as WS request. Use this to sign the requests.
   * <code>
   *   val authorization = OAuthTools.getAuthorizationHeader("http://example.com/bunnies", "POST", oauthParameters)
   *   request.withHeaders("Authorization" -> authorization)
   * </code>
   * @param url The URL the request is going to
   * @param method The HTTP method
   * @param oauthParameters The OAuthParameters object which contains the oauth information
   * @return The Authorization header data
   */
  def getAuthorizationHeader(url: String, method: String, oauthParameters: OAuthParameters): String = {
    // Get the signature
    val signature = generateSignature(url, method, oauthParameters)

    // Compute the realm
    val u = new URL(url)
    val realm = u.getProtocol + "://" + u.getHost + "/"

    // Create the parts
    val parts = List(
      "realm" -> realm
    ) ::: oauthParameters.getBaseParameters.filterKeys(k => k.length > 6 && k.substring(0,6) == "oauth_").toList :::
      List("oauth_signature" -> OAuthUtil.encode(signature))

    // Generate the authorization header
    "OAuth " + parts.map(p => p._1 + "=\"" + p._2 + "\"").mkString(",")
  }

  /**
   * Calculates the signature base url as per section 9.1 of the OAuth Spec.
   * This is a concatenation of http method, request url, and other request
   * parameters.
   *
   * @see <a href="http://oauth.net/core/1.0/#anchor14">9.1 Signature Base
   *      String</a>
   *
   * @param requestUrl the url of the request
   * @param httpMethod the http method, for example "GET" or "PUT"
   * @param baseParameters the request parameters (see section 9.1.3)
   * @return the base string to be used in the OAuth signature
   * @throws OAuthException if the input url is not formatted properly
   */
  def  getSignatureBaseString(requestUrl: String, httpMethod: String, baseParameters: Map[String, String]): String = {
    OAuthUtil.encode(httpMethod.toUpperCase) + '&' +
      OAuthUtil.encode(OAuthUtil.normalizeUrl(requestUrl)) + '&' +
      OAuthUtil.encode(normalizeParameters(requestUrl, baseParameters))
  }

  /**
   * Calculates the normalized request parameters string to use in the base
   * string, as per section 9.1.1 of the OAuth Spec.
   *
   * @see <a href="http://oauth.net/core/1.0/#rfc.section.9.1.1">9.1.1
   *      Normalize Request Parameters</a>
   *
   * This is a fix from the google code which removes keys with empty values
   *
   * @param requestUrl the request url to normalize (not <code>null</code>)
   * @param requestParameters key/value pairs of parameters in the request
   * @return the parameters normalized to a string
   */
  def normalizeParameters(requestUrl: String, requestParameters: Map[String, String]): String = {
    // use a TreeMap to alphabetize the parameters by key
    val alphaParams = new util.TreeMap[String, String](requestParameters)

    // piece together the base string, encoding each key and value
    val paramString = new StringBuilder()
    for (e <- alphaParams.entrySet()) {
      if (paramString.length > 0)
        paramString.append("&")
      paramString
        .append(OAuthUtil.encode(e.getKey))
        .append("=")
        .append(OAuthUtil.encode(e.getValue))
    }
    paramString.toString()
  }


}
