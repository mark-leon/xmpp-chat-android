package com.example.whatsappclone

import org.jivesoftware.smack.packet.IQ

class PushRegistrationIQ(private val token: String) : IQ(ELEMENT, NAMESPACE) {

    companion object {
        const val ELEMENT = "query"
        const val NAMESPACE = "google:push:fcm"
    }

    init {
        type = Type.set
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder): IQChildElementXmlStringBuilder {
        // Add XML content inside <query xmlns='google:push:fcm'>...</query>
        xml.rightAngleBracket()
        xml.element("key", token)
        return xml
    }
}
