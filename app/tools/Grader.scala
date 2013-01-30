package tools

import models.{ResponseData, Problem}
import anorm.NotAssigned
import org.apache.commons.lang3.StringUtils

object Grader {

  def compareSets(responses: List[List[String]], answers: List[List[String]], wordOrder: Boolean): List[Double] =
    // Check each response with each answer and compute a percentage on how much it matches (1st line)
    // Then pick the answer which it is closest to (2nd line)
    // Then group the responses by the answer they are closest to. (3rd line)
    // Manipulate it so we get: answerIndex -> acceptanceRate (4th line)
    // For each answer pick the highest acceptance rate (5th line)
    // Return the list of acceptance rates (6th line)
    responses.map(response => answers.map(answer => computeAcceptanceRate(response, answer, wordOrder)).zipWithIndex)
      .map(mapping => mapping.maxBy(_._1))
      .groupBy(mapping => mapping._2)
      .map(entry => (entry._1, entry._2.map(mapping => mapping._1)))
      .map(entry => (entry._1, entry._2.max))
      .values.toList

  def computeAcceptanceRate(response: List[String], answer: List[String], wordOrderModifier: Boolean): Double = {
    // Get the levenshtein distance
    val s1 = (if (wordOrderModifier) response.sortWith((s,t)=>s<t) else response).mkString(" ")
    val s2 = (if (wordOrderModifier) answer.sortWith((s,t)=>s<t) else answer).mkString(" ")
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
      var grade = 0d
      val minSize = math.min(processedResponses.size, processedAnswers.size)

      // Check if we care about response order
      if (problem.responseOrderModifier) {
        // We don't. So compare the sets

        val results = compareSets(processedResponses, processedAnswers, problem.wordOrderModifier)

        // Compute the grade of each result, and sum them up
        grade = results.map(r => computeGrade(problem, r)).sum

        // Penalize

//        processedResponses = processedResponses.sortWith((r1, r2) => r1.mkString < r2.mkString)
//        processedAnswers = processedAnswers.sortWith((r1, r2) => r1.mkString < r2.mkString)
      } else {
        // We do. So go sequentially
        for (i <- 0 until minSize) {
          val acceptanceRate = computeAcceptanceRate(processedResponses(i), processedAnswers(i), problem.wordOrderModifier)
          grade += computeGrade(problem, acceptanceRate)
        }
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
