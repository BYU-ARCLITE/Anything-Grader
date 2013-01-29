import anorm.NotAssigned
import models.Problem
import org.specs2.mutable.Specification
import tools.Grader

/**
 * Created with IntelliJ IDEA.
 * User: camman3d
 * Date: 1/23/13
 * Time: 10:29 AM
 * To change this template use File | Settings | File Templates.
 */
class GraderSpecification extends Specification {
  "The grader" should {
    "Support single answer" in {
      val problem = Problem(NotAssigned, List("one"), 'single)
      val grade1 = Grader.grade(List("one"), problem).grade
      val grade2 = Grader.grade(List("two"), problem).grade

      grade1 must beEqualTo(1d)
      grade2 must beEqualTo(0d)
    }
    "Support multiple answer" in {
      val problem = Problem(NotAssigned, List("one", "two", "three"), 'multiple)
      val grade1 = Grader.grade(List("one", "two", "three"), problem).grade
      val grade2 = Grader.grade(List("one", "two"), problem).grade
      val grade3 = Grader.grade(List("seven"), problem).grade

      grade1 must beEqualTo(3d)
      grade2 must beEqualTo(2d)
      grade3 must beEqualTo(0d)
    }
    "Have different point values" in {
      val problem1 = Problem(NotAssigned, List("one"), 'single)
      val problem2 = Problem(NotAssigned, List("one"), 'single, 5)
      val grade1 = Grader.grade(List("one"), problem1).grade
      val grade2 = Grader.grade(List("one"), problem2).grade

      grade1 must beEqualTo(1d)
      grade2 must beEqualTo(5d)
    }
    "Accept responses that match at or above a certain rate" in {
      val problem = Problem(NotAssigned, List("four"), 'single, 1, 0.5)
      val grade1 = Grader.grade(List("four"), problem).grade
      val grade2 = Grader.grade(List("FOur"), problem).grade
      val grade3 = Grader.grade(List("FOUr"), problem).grade

      grade1 must beEqualTo(1d)
      grade2 must beEqualTo(1d)
      grade3 must beEqualTo(0d)
    }
    "Be able to ignore case" in {
      val problem1 = Problem(NotAssigned, List("one"), 'single, 1, 1, true)
      val problem2 = Problem(NotAssigned, List("one"), 'single, 1, 1)
      val grade1 = Grader.grade(List("one"), problem1).grade
      val grade2 = Grader.grade(List("OnE"), problem1).grade
      val grade3 = Grader.grade(List("OnE"), problem2).grade

      grade1 must beEqualTo(1d)
      grade2 must beEqualTo(1d)
      grade3 must beEqualTo(0d)
    }
    "Be able to ignore punctuation" in {
      val problem1 = Problem(NotAssigned, List("one two"), 'single, 1, 1, false, true)
      val problem2 = Problem(NotAssigned, List("one. two"), 'single, 1, 1, false, true)
      val problem3 = Problem(NotAssigned, List("one. two"), 'single)
      val grade1 = Grader.grade(List("one. two"), problem1).grade
      val grade2 = Grader.grade(List("one two"), problem2).grade
      val grade3 = Grader.grade(List("one two"), problem3).grade

      grade1 must beEqualTo(1d)
      grade2 must beEqualTo(1d)
      grade3 must beEqualTo(0d)
    }
    "Be able to ignore word order" in {
      val problem1 = Problem(NotAssigned, List("one two"), 'single, 1, 1, false, false, true)
      val problem2 = Problem(NotAssigned, List("one two"), 'single)
      val grade1 = Grader.grade(List("one two"), problem1).grade
      val grade2 = Grader.grade(List("two one"), problem1).grade
      val grade3 = Grader.grade(List("two one"), problem2).grade

      grade1 must beEqualTo(1d)
      grade2 must beEqualTo(1d)
      grade3 must beEqualTo(0d)
    }
    "Be able to ignore response order" in {
      val problem1 = Problem(NotAssigned, List("one", "two"), 'multiple, 1, 1, false, false, false, true)
      val problem2 = Problem(NotAssigned, List("one", "two"), 'multiple)
      val grade1 = Grader.grade(List("one", "two"), problem1).grade
      val grade2 = Grader.grade(List("two", "one"), problem1).grade
      val grade3 = Grader.grade(List("two", "one"), problem2).grade

      grade1 must beEqualTo(2d)
      grade2 must beEqualTo(2d)
      grade3 must beEqualTo(0d)
    }
    "Give partial credit" in {
      val problem1 = Problem(NotAssigned, List("four"), 'single, 10, 1, false, false, false, false, true)
      val problem2 = Problem(NotAssigned, List("four"), 'single)
      val grade1 = Grader.grade(List("four"), problem1).grade
      val grade2 = Grader.grade(List("foUR"), problem1).grade
      val grade3 = Grader.grade(List("FOUR"), problem1).grade
      val grade4 = Grader.grade(List("foUR"), problem2).grade

      grade1 must beEqualTo(10d)
      grade2 must beEqualTo(5d)
      grade3 must beEqualTo(0d)
      grade4 must beEqualTo(0d)
    }
    "Take away points" in {
      val problem = Problem(NotAssigned, List("one", "two", "three"), 'multiple, 1, 1, false, false, false, false, false, true)
      val grade1 = Grader.grade(List("one", "two", "three"), problem).grade // 3
      val grade2 = Grader.grade(List("one", "two"), problem).grade // 2
      val grade3 = Grader.grade(List("one", "two", "four"), problem).grade // 1
      val grade4 = Grader.grade(List("one", "two", "four", "five"), problem).grade // 0
      val grade5 = Grader.grade(List("one", "two", "four", "five", "six"), problem).grade // -1

      grade1 must beEqualTo(3d)
      grade2 must beEqualTo(2d)
      grade3 must beEqualTo(1d)
      grade4 must beEqualTo(0d)
      grade5 must beEqualTo(-1d)
    }
  }
}
