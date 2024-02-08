package com.ogzkesk.tfliteimageclassification.util

object Logger {

    private var enabled: Boolean = true

    fun log(message: Any?) {
        if (enabled) {
            println("${getTag()}: ${if(message is Exception) message.message else message}")
        }
    }

    fun setEnabled(enabled: Boolean){
        Logger.enabled = enabled
    }

    private fun getTag(): String {
        val t = Throwable()
        val element = t.stackTrace[2]
        val modifierClassName = element.className
            .substringAfterLast(".")
            .substringBefore("$")
            .plus(".")
        val methodName = if (element.methodName.contains("suspend", true)) {
            element.className
                .substringAfter("$")
                .substringBeforeLast("$")
                .plus("()")
        } else {
            element.methodName.plus("()")
        }
        return modifierClassName + methodName
    }
}