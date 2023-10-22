package email

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import data.Person
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.Email
import org.apache.commons.mail.HtmlEmail
import utils.readResourceFileAsProperties
import java.io.StringWriter
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import javax.mail.internet.InternetAddress

class EmailService() {

    private val mustacheFactory: DefaultMustacheFactory = DefaultMustacheFactory("templates")

    fun sendEmail(recipient: Person, linkedPeople: List<Person>) {
        // Template done online with MJML
        val htmlMustache: Mustache? = mustacheFactory.compile("secret_santa.html")
        val txtMustache: Mustache? = mustacheFactory.compile("secret_santa.txt")

        // Give interpolation params to mustache template
        mapOf(
            "recipientName" to recipient.firstName,
            "linkedPeopleNames" to linkedPeople.map { EmailPerson(it.firstName, it.lastName) }
        )
            .let { MailContent(txtMustache!!.buildTemplate(it), htmlMustache!!.buildTemplate(it)) }
            .run {
                send(
                    recipients = listOf(recipient.email),
                    subject = "Père Noël Secret 2023", // TODO use properties files for translation
                    textContent = textContent,
                    htmlContent = htmlContent,
                )
            }
    }

    private fun send(
        recipients: List<String>,
        subject: String,
        textContent: String,
        htmlContent: String,
    ) {
        try {
            HtmlEmail()
                .also { configure(it) }
                .apply {
                    setCharset(UTF_8.toString())
                    addBcc(*recipients.toTypedArray())
                    setSubject(subject)
                    setHtmlMsg(htmlContent)
                    setTextMsg(textContent)
                    addTo(*recipients.toTypedArray())
                }
                .send()
                .also { println("Email has been sent to $recipients - Subject was: '$subject'") }
        } catch (exception: Exception) {
            println(
                "Error occurred during email sending (recipients were: $recipients - subject was: '$subject')\n" +
                        "$exception\n${exception.cause}"
            )
        }
    }

    // TODO make dummy email.properties file for example
    // TODO configure only once, move to Main ?
    private fun configure(email: Email) {
        val emailProperties = readResourceFileAsProperties("email.properties")

        email.apply {
            // server connection
            hostName = emailProperties.getProperty("hostName")

            if (emailProperties.getProperty("sslOnConnect").toBoolean()) {
                isSSLOnConnect = true
                sslSmtpPort = emailProperties.getProperty("smtpSslPort")
            } else {
                setSmtpPort(emailProperties.getProperty("smtpTlsPort").toInt())
            }
            isStartTLSEnabled = emailProperties.getProperty("startTlsEnabled").toBoolean()

            if (emailProperties.getProperty("userName").isNotBlank()) {
                setAuthenticator(
                    DefaultAuthenticator(
                        emailProperties.getProperty("userName"),
                        emailProperties.getProperty("password")
                    )
                )
            }

            setFrom(
                emailProperties.getProperty("fromEmail"),
                emailProperties.getProperty("fromFullName"),
                UTF_8.toString()
            )
        }
    }
}

private fun Mustache.buildTemplate(context: Map<String, Any>): String =
    StringWriter().use {
        execute(it, context)
        it.flush()
        it.toString()
    }

private data class MailContent(val textContent: String, val htmlContent: String)