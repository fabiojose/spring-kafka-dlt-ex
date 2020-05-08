import ch.qos.logback.classic.Level

statusListener(OnConsoleStatusListener)

// Obter nível de registro da variável de ambiente
def _level = System.getenv("APP_LOG_LEVEL")
_level = (_level != null ? _level.toUpperCase() : "INFO")

// Obter padrão de registro da variável de ambiente
def _pattern = System.getenv("APP_LOG_PATTERN")
_pattern = (_pattern != null ? _pattern :
	"%-5level [%t] %c{6}: %msg%n%throwable")

// Obter configurações de outros loggers
def _loggers = System.getenv("APP_LOG_LOGGERS")
if( null != _loggers ){
  _loggers.split(',').each {
    def _logger = it.split(':')
  	logger(_logger[0], Level.toLevel(_logger[1]))
  }
}

def appenders = ["CONSOLE"]

appender("CONSOLE", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = _pattern
  }
}

root(Level.toLevel(_level), appenders)
