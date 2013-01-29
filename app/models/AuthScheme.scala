package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import play.api.mvc.Request

/**
 * The AuthScheme object
 * @param id The ID used in the DB
 * @param publicKey The username/consumerKey
 * @param privateKey The password/consumerSecret
 * @param authType The authentication method (None, HTTP Authentication, OAuth 1.0a)
 */
case class AuthScheme(
  id: Pk[Long],
  publicKey: String,
  privateKey: String,
  authType: String
) {

  def insert: AuthScheme = {
    DB.withConnection { implicit connection =>
      val id: Option[Long] = SQL(
        "insert into auth_scheme (publicKey, privateKey, authType) values ({publicKey}, {privateKey}, {authType})"
      ).on(
        'publicKey -> this.publicKey,
        'privateKey -> this.privateKey,
        'authType -> this.authType
      ).executeInsert()

      AuthScheme(Id(id.get), this.publicKey, this.privateKey, this.authType)
    }
  }

  def update: AuthScheme = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          update auth_scheme
          set publicKey = {publicKey}, privateKey = {privateKey}, authType = {authType}
          where id = {id}
        """
      ).on(
        'id -> this.id,
        'publicKey -> this.publicKey,
        'privateKey -> this.privateKey,
        'authType -> this.authType
      ).executeUpdate()
      this
    }
  }

  def save: AuthScheme = {
    if (id.isDefined)
      update
    else
      insert
  }

  def updateFromRequest()(implicit request: Request[Map[String, Seq[String]]]): AuthScheme = {
    val params = request.body.map(p => (p._1, p._2(0)))
    val publicKey = params.get("publicKey").getOrElse(this.publicKey)
    val privateKey = params.get("privateKey").getOrElse(this.privateKey)
    val authType = params.get("authType").getOrElse(this.authType)

    AuthScheme(this.id, publicKey, privateKey, authType)
  }

  def delete() {
    DB.withConnection {
      implicit connection =>
        SQL("delete from auth_scheme where id = {id}").on('id -> this.id).executeUpdate()
    }
  }
}

object AuthScheme {
  val simple = {
    get[Pk[Long]]("auth_scheme.id") ~
    get[String]("auth_scheme.publicKey") ~
    get[String]("auth_scheme.privateKey") ~
    get[String]("auth_scheme.authType") map {
      case id~publicKey~privateKey~authType => AuthScheme(id, publicKey, privateKey, authType)
    }
  }

  def findById(id: Long): Option[AuthScheme] = {
    DB.withConnection { implicit connection =>
      SQL("select * from auth_scheme where id = {id}").on('id -> id).as(AuthScheme.simple.singleOpt)
    }
  }

  def createFromRequest()(implicit request: Request[Map[String, Seq[String]]]): AuthScheme = {
    val authType = request.body("authType")(0)
    var publicKey = ""
    var privateKey = ""
    if (authType == "http" || authType == "oauth") {
      publicKey = request.body("key")(0)
      privateKey = request.body("secret")(0)
    }

    AuthScheme(NotAssigned, publicKey, privateKey, authType)
  }
}
