package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import play.api.libs.json.Json
import play.api.mvc.Request

case class Problem(
                    id: Pk[Long],
                    name: String,
                    answers: List[String],
                    problemType: Symbol,
                    points: Double = 1,
                    acceptanceRate: Double = 1,
                    caseModifier: Boolean = false,
                    punctuationModifier: Boolean = false,
                    wordOrderModifier: Boolean = false,
                    responseOrderModifier: Boolean = false,
                    gradientGradeMethod: Boolean = false,
                    subtractiveModifier: Boolean = false,
                    multipleGradeModifier: Boolean = false
                    ) {

  def insert: Problem = {
    DB.withConnection {
      implicit connection =>
        val id: Option[Long] = SQL(
          """
          insert into problem
          (name, answers, problemType, acceptanceRate, caseModifier, punctuationModifier, wordOrderModifier,
          responseOrderModifier, gradientGradeMethod, points, subtractiveModifier, multipleGradeModifier)
          values
          ({name}, {answers}, {problemType}, {acceptanceRate}, {caseModifier}, {punctuationModifier}, {wordOrderModifier},
          {responseOrderModifier}, {gradientGradeMethod}, {points}, {subtractiveModifier}, {multipleGradeModifier})
          """
        ).on(
          'name -> this.name,
          'answers -> Json.toJson(this.answers).toString(),
          'problemType -> this.problemType.name,
          'acceptanceRate -> this.acceptanceRate,
          'caseModifier -> this.caseModifier,
          'punctuationModifier -> this.punctuationModifier,
          'wordOrderModifier -> this.wordOrderModifier,
          'responseOrderModifier -> this.responseOrderModifier,
          'gradientGradeMethod -> this.gradientGradeMethod,
          'points -> this.points,
          'subtractiveModifier -> this.subtractiveModifier,
          'multipleGradeModifier -> this.multipleGradeModifier
        ).executeInsert()

        Problem(
          Id(id.get), this.name, this.answers, this.problemType, this.points, this.acceptanceRate, this.caseModifier,
          this.punctuationModifier, this.wordOrderModifier, this.responseOrderModifier, this.gradientGradeMethod,
          this.subtractiveModifier, this.multipleGradeModifier
        )
    }
  }

  def update: Problem = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update problem set
          name = {name},
          answers = {answers},
          problemType = {problemType},
          acceptanceRate = {acceptanceRate},
          caseModifier = {caseModifier},
          punctuationModifier = {punctuationModifier},
          wordOrderModifier = {wordOrderModifier},
          responseOrderModifier = {responseOrderModifier},
          gradientGradeMethod = {gradientGradeMethod},
          points = {points},
          subtractiveModifier = {subtractiveModifier},
          multipleGradeModifier = {multipleGradeModifier}
          where id = {id}
          """
        ).on(
          'id -> this.id,
          'name -> this.name,
          'answers -> Json.toJson(this.answers).toString(),
          'problemType -> this.problemType.name,
          'acceptanceRate -> this.acceptanceRate,
          'caseModifier -> this.caseModifier,
          'punctuationModifier -> this.punctuationModifier,
          'wordOrderModifier -> this.wordOrderModifier,
          'responseOrderModifier -> this.responseOrderModifier,
          'gradientGradeMethod -> this.gradientGradeMethod,
          'points -> this.points,
          'subtractiveModifier -> this.subtractiveModifier,
          'multipleGradeModifier -> this.multipleGradeModifier
        ).executeUpdate()
        this
    }
  }

  def save: Problem = {
    if (id.isDefined)
      update
    else
      insert
  }

  def getPointsPossible: Double = this.points * this.answers.size

  def updateFromRequest()(implicit request: Request[Map[String, Seq[String]]]): Problem = {
    val params = request.body.map(p => (p._1, p._2(0)))
    val name = params.get("name").getOrElse(this.name)
    val answers = Json.parse(params.get("answers").getOrElse(Json.toJson(this.answers).toString())).as[List[String]]
    val problemType = Symbol(params.get("problemType").getOrElse(this.problemType.name))
    val points = params.get("points").getOrElse(this.points.toString).toDouble
    val acceptanceRate = params.get("acceptanceRate").getOrElse(this.acceptanceRate.toString).toDouble
    val caseModifier = params.get("caseModifier").getOrElse(this.caseModifier.toString).toBoolean
    val punctuationModifier = params.get("punctuationModifier").getOrElse(this.punctuationModifier.toString).toBoolean
    val wordOrderModifier = params.get("wordOrderModifier").getOrElse(this.wordOrderModifier.toString).toBoolean
    val responseOrderModifier = params.get("responseOrderModifier").getOrElse(this.responseOrderModifier.toString).toBoolean
    val gradientGradeMethod = params.get("gradientGradeMethod").getOrElse(this.gradientGradeMethod.toString).toBoolean
    val subtractiveModifier = params.get("subtractiveModifier").getOrElse(this.subtractiveModifier.toString).toBoolean
    val multipleGradeModifier = params.get("multipleGradeModifier").getOrElse(this.multipleGradeModifier.toString).toBoolean

    Problem(this.id, name, answers, problemType, points, acceptanceRate, caseModifier, punctuationModifier,
      wordOrderModifier, responseOrderModifier, gradientGradeMethod, subtractiveModifier, multipleGradeModifier)
  }

  def delete() {
    DB.withConnection {
      implicit connection =>
        SQL("delete from problem where id = {id}").on('id -> this.id).executeUpdate()
    }
  }
}

object Problem {
  val simple = {
    get[Pk[Long]]("problem.id") ~
      get[String]("problem.name") ~
      get[String]("problem.answers") ~
      get[String]("problem.problemType") ~
      get[Double]("problem.acceptanceRate") ~
      get[Boolean]("problem.caseModifier") ~
      get[Boolean]("problem.punctuationModifier") ~
      get[Boolean]("problem.wordOrderModifier") ~
      get[Boolean]("problem.responseOrderModifier") ~
      get[Boolean]("problem.gradientGradeMethod") ~
      get[Double]("problem.points") ~
      get[Boolean]("problem.subtractiveModifier") ~
      get[Boolean]("problem.multipleGradeModifier") map {
      case id ~ name ~answers ~ problemType ~ acceptanceRate ~ caseModifier ~ punctuationModifier ~ wordOrderModifier ~
        responseOrderModifier ~ gradientGradeMethod ~ points ~ subtractiveModifier ~ multipleGradeModifier =>
        Problem(
          id, name, Json.parse(answers).as[List[String]], Symbol(problemType), points, acceptanceRate, caseModifier,
          punctuationModifier, wordOrderModifier, responseOrderModifier, gradientGradeMethod, subtractiveModifier,
          multipleGradeModifier
        )
    }
  }

  def findById(id: Long): Option[Problem] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from problem where id = {id}").on('id -> id).as(Problem.simple.singleOpt)
    }
  }

  def createFromRequest()(implicit request: Request[Map[String, Seq[String]]]): Problem = {
    val answers = Json.parse(request.body("answers")(0)).as[List[String]]
    val name = request.body("name")(0)
    val problemType = Symbol(request.body("problemType")(0))
    val points = request.body("points")(0).toDouble
    val acceptanceRate = request.body("acceptanceRate")(0).toDouble
    val caseModifier = request.body("caseModifier")(0).toBoolean
    val punctuationModifier = request.body("punctuationModifier")(0).toBoolean
    val wordOrderModifier = request.body("wordOrderModifier")(0).toBoolean
    val responseOrderModifier = request.body("responseOrderModifier")(0).toBoolean
    val gradientGradeMethod = request.body("gradientGradeMethod")(0).toBoolean
    val subtractiveModifier = request.body("subtractiveModifier")(0).toBoolean
    val multipleGradeModifier = request.body("multipleGradeModifier")(0).toBoolean

    Problem(NotAssigned, name, answers, problemType, points, acceptanceRate, caseModifier, punctuationModifier,
      wordOrderModifier, responseOrderModifier, gradientGradeMethod, subtractiveModifier, multipleGradeModifier)
  }
}
