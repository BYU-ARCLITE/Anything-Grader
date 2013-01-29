package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import play.api.libs.json.Json

case class ResponseData(id: Pk[Long], problem: Problem, data: List[String], grade: Double) {

  def insert: ResponseData = {
    DB.withConnection {
      implicit connection =>
        val id: Option[Long] = SQL(
          """
          insert into response_data
          (problem, data, grade)
          values ({problem}, {data}, {grade})
          """
        ).on(
          'problem -> this.problem.id,
          'data -> Json.toJson(this.data).toString(),
          'grade -> this.grade
        ).executeInsert()

        ResponseData(Id(id.get), this.problem, this.data, this.grade)
    }
  }

  def update: ResponseData = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update response_data
          set problem = {problem}, data = {data}, grade = {grade}
          where id = {id}
          """
        ).on(
          'id -> this.id,
          'problem -> this.problem.id,
          'data -> Json.toJson(this.data).toString(),
          'grade -> this.grade
        ).executeUpdate()
        this
    }
  }

  def save: ResponseData = {
    if (id.isDefined)
      update
    else
      insert
  }

  def delete() {
    DB.withConnection {
      implicit connection =>
        SQL("delete from response_data where id = {id}").on('id -> this.id).executeUpdate()
    }
  }
}

object ResponseData {
  val simple = {
    get[Pk[Long]]("response_data.id") ~
      get[Long]("response_data.problem") ~
      get[String]("response_data.data") ~
      get[Double]("response_data.grade") map {
      case id ~ problem ~ data ~ grade => ResponseData(id, Problem.findById(problem).get, Json.parse(data).as[List[String]], grade)
    }
  }

  def findById(id: Long): Option[ResponseData] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from response_data where id = {id}").on('id -> id).as(ResponseData.simple.singleOpt)
    }
  }
}
