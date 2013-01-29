package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import play.api.libs.json.Json
import play.api.mvc.Request

case class Problem(
                    id: Pk[Long],
                    answers: List[String],
                    problemType: Symbol,
                    points: Double = 1,
                    acceptanceRate: Double = 1,
                    caseModifier: Boolean = false,
                    punctuationModifier: Boolean = false,
                    wordOrderModifier: Boolean = false,
                    responseOrderModifier: Boolean = false,
                    gradientGradeMethod: Boolean = false,
                    subtractiveModifier: Boolean = false
                    ) {

  def insert: Problem = {
    DB.withConnection {
      implicit connection =>
        val id: Option[Long] = SQL(
          """
          insert into problem
          (answers, problemType, acceptanceRate, caseModifier, punctuationModifier, wordOrderModifier,
          responseOrderModifier, gradientGradeMethod, points, subtractiveModifier)
          values
          ({answers}, {problemType}, {acceptanceRate}, {caseModifier}, {punctuationModifier}, {wordOrderModifier},
          {responseOrderModifier}, {gradientGradeMethod}, {points}, {subtractiveModifier})
          """
        ).on(
          'answers -> Json.toJson(this.answers).toString(),
          'problemType -> this.problemType.name,
          'acceptanceRate -> this.acceptanceRate,
          'caseModifier -> this.caseModifier,
          'punctuationModifier -> this.punctuationModifier,
          'wordOrderModifier -> this.wordOrderModifier,
          'responseOrderModifier -> this.responseOrderModifier,
          'gradientGradeMethod -> this.gradientGradeMethod,
          'points -> this.points,
          'subtractiveModifier -> this.subtractiveModifier
        ).executeInsert()

        Problem(
          Id(id.get), this.answers, this.problemType, this.points, this.acceptanceRate, this.caseModifier, this.punctuationModifier,
          this.wordOrderModifier, this.responseOrderModifier, this.gradientGradeMethod, this.subtractiveModifier
        )
    }
  }

  def update: Problem = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update problem set
          answers = {answers},
          problemType = {problemType},
          acceptanceRate = {acceptanceRate},
          caseModifier = {caseModifier},
          punctuationModifier = {punctuationModifier},
          wordOrderModifier = {wordOrderModifier},
          responseOrderModifier = {responseOrderModifier},
          gradientGradeMethod = {gradientGradeMethod},
          points = {points},
          subtractiveModifier = {subtractiveModifier}
          where id = {id}
          """
        ).on(
          'id -> this.id,
          'answers -> Json.toJson(this.answers).toString(),
          'problemType -> this.problemType.name,
          'acceptanceRate -> this.acceptanceRate,
          'caseModifier -> this.caseModifier,
          'punctuationModifier -> this.punctuationModifier,
          'wordOrderModifier -> this.wordOrderModifier,
          'responseOrderModifier -> this.responseOrderModifier,
          'gradientGradeMethod -> this.gradientGradeMethod,
          'points -> this.points,
          'subtractiveModifier -> this.subtractiveModifier
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

    Problem(this.id, answers, problemType, points, acceptanceRate, caseModifier, punctuationModifier,
      wordOrderModifier, responseOrderModifier, gradientGradeMethod, subtractiveModifier)
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
      get[String]("problem.answers") ~
      get[String]("problem.problemType") ~
      get[Double]("problem.acceptanceRate") ~
      get[Boolean]("problem.caseModifier") ~
      get[Boolean]("problem.punctuationModifier") ~
      get[Boolean]("problem.wordOrderModifier") ~
      get[Boolean]("problem.responseOrderModifier") ~
      get[Boolean]("problem.gradientGradeMethod") ~
      get[Double]("problem.points") ~
      get[Boolean]("problem.subtractiveModifier") map {
      case id ~ answers ~ problemType ~ acceptanceRate ~ caseModifier ~ punctuationModifier ~ wordOrderModifier ~ responseOrderModifier ~ gradientGradeMethod ~ points ~ subtractiveModifier => Problem(
        id, Json.parse(answers).as[List[String]], Symbol(problemType), points, acceptanceRate, caseModifier,
        punctuationModifier, wordOrderModifier, responseOrderModifier, gradientGradeMethod, subtractiveModifier
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
    val problemType = Symbol(request.body("problemType")(0))
    val points = request.body("points")(0).toDouble
    val acceptanceRate = request.body("acceptanceRate")(0).toDouble
    val caseModifier = request.body("caseModifier")(0).toBoolean
    val punctuationModifier = request.body("punctuationModifier")(0).toBoolean
    val wordOrderModifier = request.body("wordOrderModifier")(0).toBoolean
    val responseOrderModifier = request.body("responseOrderModifier")(0).toBoolean
    val gradientGradeMethod = request.body("gradientGradeMethod")(0).toBoolean
    val subtractiveModifier = request.body("subtractiveModifier")(0).toBoolean

    Problem(NotAssigned, answers, problemType, points, acceptanceRate, caseModifier, punctuationModifier,
      wordOrderModifier, responseOrderModifier, gradientGradeMethod, subtractiveModifier)
  }
}
