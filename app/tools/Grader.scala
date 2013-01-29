package tools

import models.{ResponseData, Problem}
import anorm.NotAssigned
import org.apache.commons.lang3.StringUtils

object Grader {

  def computeAcceptanceRate(response: List[String], answer: List[String], wordOrderModifer: Boolean): Double = {
    // Get the levenshtein distance
    val s1 = (if (wordOrderModifer) response.sortWith((s,t)=>s<t) else response).mkString(" ")
    val s2 = (if (wordOrderModifer) answer.sortWith((s,t)=>s<t) else answer).mkString(" ")
    val dist: Double = StringUtils.getLevenshteinDistance(s1, s2)

    // Turn it into a percentage
    val len: Double = math.max(s1.length, s2.length)
    1.0 - (dist / len)
  }

  def computeGrade(problem: Problem, acceptanceRate: Double): Double = {
    if (acceptanceRate >= problem.acceptanceRate)
      problem.points
    else {
      if (problem.gradientGradeMethod)
        acceptanceRate * problem.points
      else {
        if (problem.subtractiveModifier)
          -problem.points
        else
          0
      }
    }
  }

  def grade(responses: List[String], problem: Problem): ResponseData = {
    // Pre process each response
    var processedResponses = preprocess(responses, problem)
    var processedAnswers = preprocess(problem.answers, problem)

    if (problem.problemType == 'single) {
      // Single answer
      val acceptanceRate = computeAcceptanceRate(processedResponses(0), processedAnswers(0), problem.wordOrderModifier)
      val grade = computeGrade(problem, acceptanceRate)
      ResponseData(NotAssigned, problem, responses, grade)
    } else {
      // Multiple answer
      if (problem.responseOrderModifier) {
        processedResponses = processedResponses.sortWith((r1, r2) => r1.mkString < r2.mkString)
        processedAnswers = processedAnswers.sortWith((r1, r2) => r1.mkString < r2.mkString)
      }

      val minSize = math.min(processedResponses.size, processedAnswers.size)
      var grade = 0d
      for (i <- 0 until minSize) {
        val acceptanceRate = computeAcceptanceRate(processedResponses(i), processedAnswers(i), problem.wordOrderModifier)
        grade += computeGrade(problem, acceptanceRate)
      }

      // Check for extra bad answers
      if (problem.subtractiveModifier && processedResponses.size > minSize)
        grade -= problem.points * (processedResponses.size - minSize)

      ResponseData(NotAssigned, problem, responses, grade)
    }
  }

  def preprocess(responses: List[String], problem: Problem): List[List[String]] = {
    var processedResponses = responses
    if (problem.caseModifier)
      processedResponses = processedResponses.map(r => r.toLowerCase)
    if (problem.punctuationModifier)
      processedResponses = processedResponses.map(r => r.replaceAll("[^\\w\\s]", ""))
    processedResponses.map(r => Tokenizer(r))
  }
}
