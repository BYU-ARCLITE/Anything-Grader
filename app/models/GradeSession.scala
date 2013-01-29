package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

/**
 * When grading, this object is used to saving grading information.
 * @param id The DB ID
 * @param problemSet The problem set that is being graded
 * @param responseData Grading data
 * @param started When the grading was started
 * @param finished When the grading was finished
 * @param userInfo Any information about the user
 * @param token The current access token
 */
case class GradeSession(id: Pk[Long], problemSet: ProblemSet, responseData: List[ResponseData], started: Long,
                        finished: Long, userInfo: String, token: String) {

  def insert: GradeSession = {
    DB.withConnection {
      implicit connection =>
        val newResponseData = responseData.map(r => r.save)

        val id: Option[Long] = SQL(
          """
          insert into grade_session
          (problemSet, responseData, started, finished, userInfo, token)
          values ({problemSet}, {responseData}, {started}, {finished}, {userInfo}, {token})
          """
        ).on(
          'problemSet -> this.problemSet.id,
          'responseData -> newResponseData.map(r => r.id.get).mkString(","),
          'started -> this.started,
          'finished -> this.finished,
          'userInfo -> this.userInfo,
          'token -> this.token
        ).executeInsert()

        GradeSession(
          Id(id.get), this.problemSet, newResponseData, this.started, this.finished, this.userInfo, this.token
        )
    }
  }

  def update: GradeSession = {
    DB.withConnection {
      implicit connection =>
        val newResponseData = responseData.map(r => r.save)

        SQL(
          """
          update grade_session
          set problemSet = {problemSet}, responseData = {responseData}, started = {started}, finished = {finished},
          userInfo = {userInfo}, token = {token} where id = {id}
          """
        ).on(
          'id -> this.id,
          'problemSet -> this.problemSet.id,
          'responseData -> newResponseData.map(r => r.id.get).mkString(","),
          'started -> this.started,
          'finished -> this.finished,
          'userInfo -> this.userInfo,
          'token -> this.token
        ).executeUpdate()

        GradeSession(
          this.id, this.problemSet, newResponseData, this.started, this.finished, this.userInfo, this.token
        )
    }
  }

  def save: GradeSession = {
    if (id.isDefined)
      update
    else
      insert
  }

  def delete() {
    DB.withConnection {
      implicit connection =>
      // Delete the response data
        this.responseData.map(r => r.delete())

        // Delete the grade session
        SQL("delete from grade_session where id = {id}").on('id -> this.id).executeUpdate()
    }
  }

  def addResponseData(data: ResponseData): GradeSession = GradeSession(
    this.id, this.problemSet, data :: this.responseData, this.started, this.finished, this.userInfo, this.token)

  def setFinished(time: Long): GradeSession =
    GradeSession(this.id, this.problemSet, this.responseData, this.started, time, this.userInfo, this.token)

  def setToken(token: String): GradeSession =
    GradeSession(this.id, this.problemSet, this.responseData, this.started, this.finished, this.userInfo, token)

  def getScore: Double = this.responseData.map(d => d.grade).sum

  def getScaledScore: Double = getScore / this.problemSet.getPointsPossible
}

object GradeSession {
  val simple = {
    get[Pk[Long]]("grade_session.id") ~
      get[Long]("grade_session.problemSet") ~
      get[String]("grade_session.responseData") ~
      get[Long]("grade_session.started") ~
      get[Long]("grade_session.finished") ~
      get[String]("grade_session.userInfo") ~
      get[String]("grade_session.token") map {
      case id ~ problemSet ~ responseData ~ started ~ finished ~ userInfo ~ token =>
        GradeSession(id, ProblemSet.findById(problemSet).get,
          responseData.split(",").filterNot(s => s.isEmpty).map(r => ResponseData.findById(r.toLong).get).toList,
          started, finished, userInfo, token)
    }
  }

  def findById(id: Long): Option[GradeSession] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from grade_session where id = {id}").on('id -> id).as(GradeSession.simple.singleOpt)
    }
  }

  def listByProblemSet(id: Long): List[GradeSession] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from grade_session where problemSet = {id}").on('id -> id).as(GradeSession.simple *)
    }
  }
}
