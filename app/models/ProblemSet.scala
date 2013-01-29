package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import play.api.mvc.{AnyContent, Request}
import play.api.libs.json.Json

case class ProblemSet(id: Pk[Long], name: String, problems: List[Problem], hooks: List[Hook]) {

  def insert: ProblemSet = {
    DB.withConnection {
      implicit connection =>
        val newProblems = this.problems.map(p => p.save)
        val newHooks = this.hooks.map(h => h.save)

        val id: Option[Long] = SQL(
          """
          insert into problem_set
          (name, problems, hooks)
          values ({name}, {problems}, {hooks})
          """
        ).on(
          'name -> this.name,
          'problems -> newProblems.map(p => p.id.get).mkString(","),
          'hooks -> newHooks.map(h => h.id.get).mkString(",")
        ).executeInsert()

        ProblemSet(Id(id.get), this.name, newProblems, newHooks)
    }
  }

  def update: ProblemSet = {
    DB.withConnection {
      implicit connection =>
        val newProblems = this.problems.map(p => p.save)
        val newHooks = this.hooks.map(h => h.save)

        SQL(
          """
          update problem_set
          set name = {name}, problems = {problems}, hooks = {hooks}
          where id = {id}
          """
        ).on(
          'id -> this.id,
          'name -> this.name,
          'problems -> newProblems.map(p => p.id.get).mkString(","),
          'hooks -> newHooks.map(h => h.id.get).mkString(",")
        ).executeUpdate()

        ProblemSet(this.id, this.name, newProblems, newHooks)
    }
  }

  def save: ProblemSet = {
    if (id.isDefined)
      update
    else
      insert
  }

  def delete() {
    DB.withConnection {
      implicit connection =>
        // Delete the problems
        problems.map(p => p.delete())

        // Delete the hooks
        hooks.map(h => h.delete())

        // Delete the associated grade sessions
        GradeSession.listByProblemSet(this.id.get).map(g => g.delete())

        // Delete the problem set
        SQL("delete from problem_set where id = {id}").on('id -> this.id).executeUpdate()
    }
  }

  def updateFromRequest()(implicit request: Request[Map[String, Seq[String]]]): ProblemSet = {
    val name = request.body.get("name").getOrElse(Seq(this.name))(0)
    // Reorder the problems
    val problems = (
      if(request.body.contains("problems")) {
        Json.parse(request.body("problems")(0)).as[List[String]].map(p =>
          this.problems.find(pr => pr.id.get == p).get
        )
      } else
        this.problems
    )
    ProblemSet(this.id, name, problems, this.hooks)
  }

  def addProblem(problem: Problem): ProblemSet = {
    ProblemSet(this.id, this.name, this.problems ::: List(problem), this.hooks)
  }

  def removeProblem(problem: Problem): ProblemSet = {
    ProblemSet(this.id, this.name, this.problems.filterNot(p => p == problem), this.hooks)
  }

  def addHook(hook: Hook): ProblemSet = {
    ProblemSet(this.id, this.name, this.problems, this.hooks ::: List(hook))
  }

  def removeHook(hook: Hook): ProblemSet = {
    ProblemSet(this.id, this.name, this.problems, this.hooks.filterNot(h => h == hook))
  }

  def getPointsPossible: Double = this.problems.map(p => p.getPointsPossible).sum
}

object ProblemSet {
  val simple = {
    get[Pk[Long]]("problem_set.id") ~
      get[String]("problem_set.name") ~
      get[String]("problem_set.problems") ~
      get[String]("problem_set.hooks") map {
      case id~name~problems~hooks => ProblemSet(id, name,
        problems.split(",").filterNot(s => s.isEmpty).map(p => Problem.findById(p.toLong).get).toList,
        hooks.split(",").filterNot(s => s.isEmpty).map(p => Hook.findById(p.toLong).get).toList)
    }
  }

  def findById(id: Long): Option[ProblemSet] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from problem_set where id = {id}").on('id -> id).as(ProblemSet.simple.singleOpt)
    }
  }
}
