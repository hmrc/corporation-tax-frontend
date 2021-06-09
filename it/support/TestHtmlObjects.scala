package support

object TestHtmlObjects {

  val testServiceInfoPartial: String =
    """| <nav class="bta-navigation centered-content" aria-label="Business Tax navigation">
       |   <ul id="service-info-list">
       |     <li>
       |       <a id="service-info-home-link"
       |          href="http://localhost:9020/business-account"
       |          data-journey-click="link - click:Service info:Business tax home">
       |         Home
       |       </a>
       |     </li>
       |     <li>
       |       <a id="service-info-manage-account-link"
       |          href="http://localhost:9020/business-account/manage-account"
       |          data-journey-click="link - click:Service info:Manage account">
       |         Manage account
       |       </a>
       |     </li>
       |     <li>
       |       <a id="service-info-messages-link"
       |          href="http://localhost:9020/business-account/messages"
       |          data-journey-click="link - click:Service info:Messages" aria-label="Messages5 unread">
       |         Messages<span class="badge">5</span>
       |       </a>
       |     </li>
       |     <li>
       |       <a id="forms-tracking" href="/track/bta" data-journey-click="link - click:Service info:Track your forms">Track your forms</a>
       |     </li>
       |     <li>
       |       <a id="service-info-help-and-contact-link"
       |          href="http://localhost:9733/business-account/help"
       |          data-journey-click="link - click:Service info:Help and contact">
       |         Help and contact
       |       </a>
       |     </li>
       |    </ul>
       | </nav>""".stripMargin

}
