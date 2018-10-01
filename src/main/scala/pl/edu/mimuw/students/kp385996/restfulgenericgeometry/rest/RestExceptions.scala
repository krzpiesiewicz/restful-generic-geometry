package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.rest

import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.http.HttpStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenException(msg: String) extends RuntimeException(msg)