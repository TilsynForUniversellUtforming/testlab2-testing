package no.uutilsynet.testlab2testing.common

import java.util.*
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor

@Configuration
class LocaleConfig() : WebMvcConfigurer {

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(localeChangeInterceptor())
  }

  @Bean
  fun messageSource(): MessageSource {
    val messageSource = ResourceBundleMessageSource()
    messageSource.setBasenames("lang/messages")
    messageSource.setDefaultEncoding("UTF-8")
    return messageSource
  }

  @Bean
  fun localeResolver(): CookieLocaleResolver {
    val localeResolver = CookieLocaleResolver("lang")
    localeResolver.setDefaultLocale(Locale.getDefault())
    localeResolver.defaultTimeZone = TimeZone.getTimeZone("UTC")
    return localeResolver
  }

  @Bean
  fun localeChangeInterceptor(): LocaleChangeInterceptor {
    val interceptor = LocaleChangeInterceptor()
    interceptor.paramName = "lang"
    return interceptor
  }
}
