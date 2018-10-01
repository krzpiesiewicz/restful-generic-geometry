package pl.edu.mimuw.students.kp385996.restfulgenericgeometry

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.scheduling.annotation.EnableAsync

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala._


import pl.edu.mimuw.students.kp385996.restfulgenericgeometry._

import runtime.Security
import runtime.SandboxSecurityPolicy

@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  override def configure(http: HttpSecurity) {
    super.configure(http)
    http.csrf.disable
  }
}

@Configuration
@EnableWebMvc
class WebConfig extends WebMvcConfigurer {

  override def configureMessageConverters(converters: java.util.List[HttpMessageConverter[_]]): Unit =
    converters.add(jackson2HttpMessageConverter())

  @Bean
  def jackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter =
    new MappingJackson2HttpMessageConverter(objectMapper())

  @Bean
  def objectMapper(): ObjectMapper =
    new ObjectMapper() {
      setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      registerModule(DefaultScalaModule)
    }
}

@SpringBootApplication
@EnableAsync
class Application

object Application extends App {
  Security.initSandboxSecurityPolicy
  SpringApplication.run(classOf[Application]);
}
