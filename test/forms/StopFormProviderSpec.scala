package forms

import forms.behaviours.FormBehaviours
import models._

class StopFormProviderSpec extends FormBehaviours {

  val validData: Map[String, String] = Map(
    "value" -> Stop.options.head.value
  )

  val form = new StopFormProvider()()

  "Stop form" must {

    behave like questionForm[Stop](Stop.values.head)

    behave like formWithOptionField(
      Field(
        "value",
        Required -> "stop.error.required",
        Invalid -> "error.invalid"),
      Stop.options.toSeq.map(_.value): _*)
  }
}
