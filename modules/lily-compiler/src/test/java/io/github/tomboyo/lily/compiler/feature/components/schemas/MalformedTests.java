package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

@ExtendWith(LilyExtension.class)
public class MalformedTests {
  @Test
  void unknownType(LilyTestSupport support) throws ClassNotFoundException {
    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);

    assertDoesNotThrow(
        () ->
            support.compileOas(
                """
                components:
                    schemas:
                        Foo:
                """));

    assertThrows(ClassNotFoundException.class, () -> support.getClassForName("{{package}}.Foo"));

    appender.stop();
    assertTrue(
        appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("#/components/schemas/Foo has no schema")));
  }
}
