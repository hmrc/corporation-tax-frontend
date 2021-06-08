package support

import org.joda.time.{DateTime, LocalDateTime}

object DateTimeUtils {

  def createLocalDateTime(dateTime: String): LocalDateTime = new DateTime(dateTime).toLocalDateTime

}
