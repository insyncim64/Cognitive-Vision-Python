package com.movilizer

import com.movilizer.maf.scripting.MAFContext
import com.movilizer.maf.scripting.access.MAFNotificationGateway


class Logger {
    private static String LOGGER_BAT_PREFIX = "[CN]"

    private MAFNotificationGateway notificationGateway
    private String classPrefix

    /**
     * Mandatory constructor for MAF
     */
    Logger(){}

    /**
     * BAT standard logger to prefix messages with the correct project tags
     *
     * @param context to take the Notification gateway
     * @param classz that will be used to prefix the logs
     */
    Logger(MAFContext context, Class classz){
        notificationGateway = context.getNotificationManager()
        classPrefix = classz.getSimpleName()
    }

    void debug(String message, Object additional){
        notificationGateway.writeDebug(LOGGER_BAT_PREFIX + "[" +  classPrefix + "] " + message, additional)
    }

    void info(String message, Object additional){
        notificationGateway.writeInfo(LOGGER_BAT_PREFIX + "[" +  classPrefix + "] " + message, additional)
    }

    void warning(String message, Object additional){
        notificationGateway.writeWarning(LOGGER_BAT_PREFIX + "[" +  classPrefix + "] " + message, additional)
    }

    void error(String message, Object additional){
        notificationGateway.writeError(LOGGER_BAT_PREFIX + "[" +  classPrefix + "] " + message, additional)
    }
}
