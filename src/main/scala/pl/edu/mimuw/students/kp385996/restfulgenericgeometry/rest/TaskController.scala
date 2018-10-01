package pl.edu.mimuw.students.kp385996.restfulgenericgeometry.rest

import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestMethod.GET

import org.springframework.scheduling.annotation.Async

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import task.ArithmeticTask


@RestController
class TaskController {
  @RequestMapping(value = Array("/task"), method = Array(POST), consumes = Array("application/json"))
  @ResponseBody
  def test(@RequestBody task: ArithmeticTask): JsonNode = {
    try {
      task.getRes
    }
    catch {
      case e: Exception => throw new ForbiddenException(e.getMessage)
    }
  }
}