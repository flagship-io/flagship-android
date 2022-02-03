package com.abtasty.flagship.utils

/**
 * Class to extends in order to provide a custom Log manager.
 */
abstract class LogManager(var level: Level = Level.ALL) {


    /**
     * This enum class defines Flagship log levels that can be used to control SDK outputs.
     * <br></br><br></br>
     * The levels in ascending order are : NONE(0), EXCEPTIONS(1), ERROR(2), WARNING(3), DEBUG(4), INFO(5), ALL(6).
     * <br></br><br></br>
     *
     *  * NONE = 0: Logging will be disabled.
     *  * EXCEPTIONS = 1: Only caught exception will be logged.
     *  * ERROR = 2: Only errors and above will be logged.
     *  * WARNING = 3: Only warnings and above will be logged.
     *  * DEBUG = 4: Only debug logs and above will be logged.
     *  * INFO = 5: Only info logs and above will be logged.
     *  * ALL = 6: All logs will be logged.
     *
     *
     */
    enum class Level(var level: Int) {
        /**
         * NONE = 0: Logging will be disabled.
         */
        NONE(0),

        /**
         * EXCEPTIONS = 1: Only caught exception will be logged.
         */
        EXCEPTIONS(1),

        /**
         * ERROR = 2: Only errors and above will be logged.
         */
        ERROR(2),

        /**
         * WARNING = 3: Only warnings and above will be logged.
         */
        WARNING(3),

        /**
         * DEBUG = 4: Only debug logs and above will be logged.
         */
        DEBUG(4),

        /**
         * INFO = 5: Only info logs and above will be logged.
         */
        INFO(5),

        /**
         * ALL = 6: All logs will be logged.
         */
        ALL(6);

        fun isAllowed(newLevel: Level): Boolean {
            return newLevel.level < level || newLevel.level == level
        }
    }

    fun newLog(level: Level, tag: String, message: String) {
        if (this.level.isAllowed(level)) onLog(level, tag, message)
    }

    /**
     * Called when the SDK produce a log.
     * @param level log level.
     * @param tag location where the log come from.
     * @param message log message.
     */
    abstract fun onLog(level: Level, tag: String, message: String)

    /**
     * Called when the SDK has caught an Exception.
     * @param e exception.
     */
    open fun onException(e: Exception) {}
}