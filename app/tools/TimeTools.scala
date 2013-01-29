package tools

import java.util.Date

/**
 * Created with IntelliJ IDEA.
 * User: camman3d
 * Date: 5/2/12
 * Time: 9:37 AM
 * To change this template use File | Settings | File Templates.
 */

object TimeTools {
  def dateFromTimestamp(timestamp: Long): String = {
    val d = new Date(timestamp)
    d.toString
  }
}
