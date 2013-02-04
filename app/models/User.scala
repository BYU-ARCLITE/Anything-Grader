package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import play.api.mvc.RequestHeader

case class User(id: Pk[Long], username: String, password: String, problemSets: List[ProblemSet], floatingProblems: List[Problem]) {

  def insert: User = {
    DB.withConnection {
      implicit connection =>
        val newProblemSets = this.problemSets.map(p => p.save)
        val newFloatingProblems = this.floatingProblems.map(p => p.save)

        val id: Option[Long] = SQL(
          """
          insert into user
          (username, password, problemSets, floatingProblems)
          values ({username}, {password}, {problemSets}, {floatingProblems})
          """
        ).on(
          'username -> this.username,
          'password -> this.password,
          'problemSets -> newProblemSets.map(p => p.id.get).mkString(","),
          'floatingProblems -> newFloatingProblems.map(p => p.id.get).mkString(",")
      ).executeInsert()

        User(Id(id.get), this.username, this.password, newProblemSets, newFloatingProblems)
    }
  }

  def update: User = {
    DB.withConnection {
      implicit connection =>
        val newProblemSets = this.problemSets.map(p => p.save)
        val newFloatingProblems = this.floatingProblems.map(p => p.save)

        SQL(
          """
          update user
          set username = {username}, password = {password}, problemSets = {problemSets},
          floatingProblems = {floatingProblems}
          where id = {id}
          """
        ).on(
          'id -> this.id,
          'username -> this.username,
          'password -> this.password,
          'problemSets -> newProblemSets.map(p => p.id.get).mkString(","),
          'floatingProblems -> newFloatingProblems.map(p => p.id.get).mkString(",")
        ).executeUpdate()
        User(this.id, this.username, this.password, newProblemSets, newFloatingProblems)
    }
  }

  def save: User = {
    if (id.isDefined)
      update
    else
      insert
  }

  def delete() {
    DB.withConnection {
      implicit connection =>
        // Delete the problem sets
        this.problemSets.map(p => p.delete())

        // Delete the user
        SQL("delete from user where id = {id}").on('id -> this.id).executeUpdate()
    }
  }

  def addProblemSet(problemSet: ProblemSet): User = {
    User(this.id, this.username, this.password, problemSet :: this.problemSets, this.floatingProblems)
  }

  def removeProblemSet(problemSet: ProblemSet): User = {
    User(this.id, this.username, this.password, this.problemSets.filterNot(p => p.id == problemSet.id), this.floatingProblems)
  }

  def addFloatingProblem(problem: Problem): User = {
    User(this.id, this.username, this.password, this.problemSets, problem :: this.floatingProblems)
  }

  def removeFloatingProblem(problem: Problem): User = {
    User(this.id, this.username, this.password, this.problemSets, this.floatingProblems.filterNot(p => p.id == problem.id))
  }
}

object User {
  val simple = {
    get[Pk[Long]]("user.id") ~
      get[String]("user.username") ~
      get[String]("user.password") ~
      get[String]("user.problemSets") ~
      get[String]("user.floatingProblems") map {
      case id~username~password~problemSets~floatingProblems => User(id, username, password,
        problemSets.split(",").filterNot(s => s.isEmpty).map(s => ProblemSet.findById(s.toLong).get).toList,
        floatingProblems.split(",").filterNot(s => s.isEmpty).map(s => Problem.findById(s.toLong).get).toList
      )
    }
  }

  def findById(id: Long): Option[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from user where id = {id}").on('id -> id).as(User.simple.singleOpt)
    }
  }

  def findByUsername(username: String): Option[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from user where username = {username}").on('username -> username).as(User.simple.singleOpt)
    }
  }

  def findByProblemSet(problemSet: ProblemSet): Option[User] = list.find(_.problemSets.contains(problemSet))

  def list: List[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from user").as(User.simple *)
    }
  }

  def authenticate(username: String, password: String): Option[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from user where username = {username} and password = {password}").on(
          'username -> username,
          'password -> password
        ).as(User.simple.singleOpt)
    }
  }

  def current()(implicit request: RequestHeader): Option[User] = {
    if (request.session.get("username").isDefined)
      User.findByUsername(request.session("username"))
    else
      None
  }
}
