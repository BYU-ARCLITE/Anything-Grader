package tools

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64._
import play.api.libs.ws.WS.WSRequestHolder
import play.api.mvc.{Request, AnyContent}

object MessageSigner {
  def urlenc(input: String) = {
    URLEncoder.encode(input.replaceAll("\\+", "-PLUS-WAS-HERE-"), "UTF-8")
      .replaceAll("%7E", "~")
      .replaceAll("\\+", "%20")
      .replaceAll("-PLUS-WAS-HERE-", "%2B")
  }

  def addOauthParameters(consumerKey: String, params: List[(String, String)]): List[(String, String)] =
    params ::: List(
      "oauth_consumer_key" -> consumerKey,
      "oauth_nonce" -> System.nanoTime.toString,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp" -> (System.currentTimeMillis / 1000).toString,
      "oauth_version" -> "1.0"
    )

  def createAuthorizationHeader(url: String, signature: String, params: List[(String, String)]): String = {
    "OAuth " + (List(
      //"realm" -> urlenc(url.substring(0, url.indexOf("/", 8))),
      "realm" -> url.substring(0, url.indexOf("/", 8)),
      "oauth_signature" -> urlenc(signature)
    ) ::: params.filter(h => h._1.length > 6 && h._1.substring(0,6) == "oauth_") .map(h => h._1 -> urlenc(h._2))).map(h => h._1+"=\""+h._2+"\"").reduceLeft(_+","+_)
  }

  def createBaseString(method: String, host: String, path: String, params: List[(String, String)], secure: Boolean = false): String = {
    // Start by creating the parameters string
    val paramsString = params
      .sortWith((a, b) => a._1 < b._1)
      .filterNot(p => p._1 == "oauth_signature")
      .map(a => URLEncoder.encode(a._1, "UTF-8") + "=" + URLEncoder.encode(a._2, "UTF-8"))
      .reduceLeft(_ + "&" + _).replaceAll("\\+", "%20")

    // Create the base string taking into account if we are running on https or not and include the method
    val http = if (secure) "https://" else "http://"
    method + "&" + URLEncoder.encode(http + host + path, "UTF-8") + "&" + URLEncoder.encode(paramsString, "UTF-8")
  }

  def createSignature(baseString: String, key: String, token: String = ""): String = {
    val encodedKey = URLEncoder.encode(key, "UTF-8") + "&" + URLEncoder.encode(token, "UTF-8")
    val mac = Mac.getInstance("HmacSHA1")
    val secret = new SecretKeySpec(encodedKey.getBytes, "HmacSHA1")
    mac.init(secret)
    val digest = mac.doFinal(baseString.getBytes)
    new String(encodeBase64(digest))
  }

  def getOauthHeaders()(implicit request: Request[AnyContent]): Map[String, String] = {
    // Check for the Authorization header
    if (request.headers.get("Authorization").isDefined)
      request.headers("Authorization").split(" ")(1).split(",").map(v => {val p=v.split("=");(p(0),p(1).substring(1, p(1).length-1))}).toMap
    else
      request.body.asFormUrlEncoded.get.map(v => (v._1, v._2(0))).filter(v => v._1.length > 6 && v._1.substring(0,6) == "oauth_").map(v => {
        if (v._1 == "oauth_signature")
          (v._1, urlenc(v._2))
        else
          v
      })
  }

  def sign(url: String, method: String, secure: Boolean, params: List[(String, String)], consumerKey: String, secretKey: String, token: String = ""): String = {
    val host = url.substring(0, url.indexOf("/", 8)).replace("http://", "").replace("https://", "")
    val path = url.substring(url.indexOf("/", 8))

    val oauthParams = addOauthParameters(consumerKey, params)
    val baseString = createBaseString(method, host, path, oauthParams, secure)
    val signature = createSignature(baseString, secretKey, token)
    createAuthorizationHeader(url, signature, oauthParams)
  }

  def sign(request: WSRequestHolder, method: String, keys: (String, String, String), secure: Boolean, postData: Map[String, String]): WSRequestHolder = {
    val params =
      if (method == "GET")
        request.queryString.toList
      else
        postData.toList
    val auth = sign(request.url, method, secure, params, keys._1, keys._2, keys._3)
    request.withHeaders("Authorization" -> auth)
  }

  def verify(privateKey: String, secure: Boolean = false)(implicit request: Request[AnyContent]): Boolean = {
    // Create the parameters
    val oauthHeaders = getOauthHeaders()
    val signature = oauthHeaders("oauth_signature")
    val params = (
      (
        if (request.method == "POST")
          request.body.asFormUrlEncoded.get
        else
          request.queryString
        ).map(p => (p._1, p._2(0))) ++ Map(
        "oauth_consumer_key" -> oauthHeaders("oauth_consumer_key"),
        "oauth_nonce" -> oauthHeaders("oauth_nonce"),
        "oauth_signature_method" -> oauthHeaders("oauth_signature_method"),
        "oauth_timestamp" -> oauthHeaders("oauth_timestamp"),
        "oauth_version" -> oauthHeaders("oauth_version")
      )
      ).toList

    // Create the signature and compare that with what was given
    val baseString = createBaseString(request.method, request.host, request.path, params, secure)
    val computedSignature = urlenc(createSignature(baseString, privateKey))
    computedSignature == signature
  }
}
