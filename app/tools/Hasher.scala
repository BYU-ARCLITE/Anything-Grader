package tools

import org.apache.commons.codec.binary.Hex

/**
 * Created with IntelliJ IDEA.
 * User: camman3d
 * Date: 4/18/12
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */

object Hasher {
  def sha256Base64(input: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-256")
    new sun.misc.BASE64Encoder().encode(md.digest(input.getBytes()))
  }

  def sha1Base64(input: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    new sun.misc.BASE64Encoder().encode(md.digest(input.getBytes()))
  }

  def md5Hex(input: String): String = {
    val md = java.security.MessageDigest.getInstance("MD5")
    new String(Hex.encodeHex(md.digest(input.getBytes())))
  }
}
