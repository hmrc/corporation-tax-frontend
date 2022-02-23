package support

object TestHtmlObjects {

  val testServiceInfoPartial: String =
    """| <nav class="bta-navigation centered-content" aria-label="Business Tax navigation">
       |   <ul id="service-info-list">
       |     <li>
       |       <a id="service-info-home-link"
       |          href="http://localhost:9020/business-account">
       |         Home
       |       </a>
       |     </li>
       |     <li>
       |       <a id="service-info-manage-account-link"
       |          href="http://localhost:9020/business-account/manage-account">
       |         Manage account
       |       </a>
       |     </li>
       |     <li>
       |       <a id="service-info-messages-link"
       |          href="http://localhost:9020/business-account/messages">
       |         Messages<span class="badge">5</span>
       |       </a>
       |     </li>
       |     <li>
       |       <a id="forms-tracking" href="/track/bta">Track your forms</a>
       |     </li>
       |     <li>
       |       <a id="service-info-help-and-contact-link"
       |          href="http://localhost:9733/business-account/help">
       |         Help and contact
       |       </a>
       |     </li>
       |    </ul>
       | </nav>""".stripMargin

}
