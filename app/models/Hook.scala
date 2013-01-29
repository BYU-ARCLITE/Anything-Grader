package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import play.api.mvc.Request

/**
 * Information on where and how to send a grade
 * @param id The DB ID
 * @param uri The location that the grade will be sent
 * @param method The HTTP method that will be used
 * @param authScheme The authentication scheme to be used
 * @param scaled Do we send a scaled (0-1) score?
 * @param additionalData Sometimes we need special data to send. This is mustache template
 * @param contentType The content type to send. Valid types: application/json, application/xml, text/plain, application/x-www-form-urlencoded
 */
case class Hook(id: Pk[Long], uri: String, method: String, authScheme: AuthScheme, scaled: Boolean,
                additionalData: String, contentType: String) {

  def insert: Hook = {
    DB.withConnection {
      implicit connection =>
        val newAuthScheme = this.authScheme.save

        val id: Option[Long] = SQL(
          """
          insert into hook
          (uri, method, authScheme, scaled, additionalData, contentType)
          values ({uri}, {method}, {authScheme}, {scaled}, {additionalData}, {contentType})
          """
        ).on(
          'uri -> this.uri,
          'method -> this.method,
          'authScheme -> newAuthScheme.id,
          'scaled -> this.scaled,
          'additionalData -> this.additionalData,
          'contentType -> this.contentType
        ).executeInsert()

        Hook(Id(id.get), this.uri, this.method, newAuthScheme, this.scaled, this.additionalData, this.contentType)
    }
  }

  def update: Hook = {
    DB.withConnection {
      implicit connection =>
        val newAuthScheme = this.authScheme.save

        SQL(
          """
          update hook
          set uri = {uri}, method = {method}, authScheme = {authScheme}, scaled = {scaled},
          additionalData = {additionalData}, contentType = {contentType}
          where id = {id}
          """
        ).on(
          'id -> this.id,
          'uri -> this.uri,
          'method -> this.method,
          'authScheme -> newAuthScheme.id,
          'scaled -> this.scaled,
          'additionalData -> this.additionalData,
          'contentType -> this.contentType
        ).executeUpdate()

        Hook(this.id, this.uri, this.method, newAuthScheme, this.scaled, this.additionalData, this.contentType)
    }
  }

  def save: Hook = {
    if (id.isDefined)
      update
    else
      insert
  }

  def updateFromRequest()(implicit request: Request[Map[String, Seq[String]]]): Hook = {
    val params = request.body.map(p => (p._1, p._2(0)))
    val uri = params.get("uri").getOrElse(this.uri)
    val method = params.get("method").getOrElse(this.method)
    val authScheme = this.authScheme.updateFromRequest
    val scaled = params.get("scaled").getOrElse(this.scaled.toString).toBoolean
    val additionalData = params.get("additionalData").getOrElse(this.additionalData)
    val contentType = params.get("contentType").getOrElse(this.contentType)

    Hook(this.id, uri, method, authScheme, scaled, additionalData, contentType)
  }

  def delete() {
    DB.withConnection {
      implicit connection =>
        // Delete the auth scheme
        this.authScheme.delete()

        // Delete the hook
        SQL("delete from hook where id = {id}").on('id -> this.id).executeUpdate()
    }
  }
}

object Hook {
  val simple = {
    get[Pk[Long]]("hook.id") ~
      get[String]("hook.uri") ~
      get[String]("hook.method") ~
      get[Long]("hook.authScheme") ~
      get[Boolean]("hook.scaled") ~
      get[String]("hook.additionalData") ~
      get[String]("hook.contentType") map {
      case id~uri~method~authScheme~scaled~additionalData~contentType => Hook(id, uri, method,
        AuthScheme.findById(authScheme).get, scaled, additionalData, contentType)
    }
  }

  def findById(id: Long): Option[Hook] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from hook where id = {id}").on('id -> id).as(Hook.simple.singleOpt)
    }
  }

  def createFromRequest()(implicit request: Request[Map[String, Seq[String]]]): Hook = {
    val uri = request.body("uri")(0)
    val method = request.body("method")(0)
    val scaled = if (request.body.get("scaled").getOrElse(Seq("off"))(0) == "on") true else false
    val additionalData = request.body("additionalData")(0)
    val authScheme = AuthScheme.createFromRequest
    val contentType = request.body("contentType")(0)

    Hook(NotAssigned, uri, method, authScheme, scaled, additionalData, contentType)
  }
}
