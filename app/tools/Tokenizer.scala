package tools

import java.io.FileInputStream
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}

/**
 * Created with IntelliJ IDEA.
 * User: camman3d
 * Date: 1/22/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
object Tokenizer {
  var model: Option[TokenizerModel] = None

  def apply(s: String): List[String] = {
    val model = loadModel()
    val tokenizer = new TokenizerME(model)
    tokenizer.tokenize(s).toList
  }

  def loadModel(): TokenizerModel = {
    if (!model.isDefined) {
      val modelIn = new FileInputStream("./nlpModels/en-token.bin");
      model = Some(new TokenizerModel(modelIn))
    }
    model.get
  }
}
