package views

import play.api.data.Form
import forms.StopFormProvider
import models.Stop
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.stop

class StopViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "stop"

  val form = new StopFormProvider()()

  val serviceInfoContent = HtmlFormat.empty

  def createView = () => stop(frontendAppConfig, form)(serviceInfoContent)(fakeRequest, messages)

  def createViewUsingForm = (form: Form[_]) => stop(frontendAppConfig, form)(serviceInfoContent)(fakeRequest, messages)

  "Stop view" must {
    behave like normalPage(createView, messageKeyPrefix)
  }

  "Stop view" when {
    "rendered" must {
      "contain radio buttons for the value" in {
        val doc = asDocument(createViewUsingForm(form))
        for (option <- Stop.options) {
          assertContainsRadioButton(doc, option.id, "value", option.value, false)
        }
      }
    }

    for(option <- Stop.options) {
      s"rendered with a value of '${option.value}'" must {
        s"have the '${option.value}' radio button selected" in {
          val doc = asDocument(createViewUsingForm(form.bind(Map("value" -> s"${option.value}"))))
          assertContainsRadioButton(doc, option.id, "value", option.value, true)

          for(unselectedOption <- Stop.options.filterNot(o => o == option)) {
            assertContainsRadioButton(doc, unselectedOption.id, "value", unselectedOption.value, false)
          }
        }
      }
    }
  }
}
