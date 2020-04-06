package app.model

/**
  * Type for providing an additional info for errors in some complicated requests
  */
sealed trait ErrorInfo extends Product with Serializable

final case class NotFound(additional: String)                                     extends ErrorInfo
final case class Forbidden(additional: String)                                    extends ErrorInfo
final case class InternalServerError(additional: String)                          extends ErrorInfo
final case class Unauthorized(additional: String = "Cookie timed out or invalid") extends ErrorInfo
