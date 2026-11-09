package com.peach.mongo.log;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import com.peach.mongo.PeachMongoProperties;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 记录 Mongo 命令耗时，并输出普通命令与慢命令。
 * <p>默认 Preparing 把 filter、更新文档、插入文档中的值换成 {@code ?}，Parameters 按顺序给出这些值。
 * {@code peach.mongo.log.executable=true} 时 Preparing 改为 {@code db.runCommand(...)}，不再输出 Parameters。
 * 同一次命令只输出其中一种。慢命令使用 WARN，失败命令使用 ERROR，其余命令在 DEBUG 输出。
 * ping、hello 等驱动心跳命令默认不记录。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
public class PeachMongoCommandListener implements CommandListener {

    private static final Logger log = LoggerFactory.getLogger("MONGO_LOG");

    /**
     * 尚未结束的命令上限。超过后丢弃最早放入的一条，避免回调丢失时命令文档一直占用内存。
     */
    private static final int MAX_INFLIGHT_COMMANDS = 4096;

    private final PeachMongoProperties.Log properties;

    private final MongoCommandMetrics metrics;

    private final Set<String> excludedCommands;

    private final ConcurrentMap<Long, BsonDocument> commands = new ConcurrentHashMap<>();

    private final ConcurrentLinkedDeque<Long> commandOrder = new ConcurrentLinkedDeque<>();

    private final AtomicBoolean inflightOverflowLogged = new AtomicBoolean();

    private final String lineSeparator = System.lineSeparator();

    public PeachMongoCommandListener(PeachMongoProperties.Log properties, MongoCommandMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
        this.excludedCommands = properties.getExcludeCommands().stream()
                .filter(command -> command != null && !command.isBlank())
                .map(command -> command.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        log.info("Mongo command logging enabled: longQueryTime={}ms, executable={}",
                properties.getLongQueryTime(), properties.isExecutable());
    }

    @Override
    public void commandStarted(CommandStartedEvent event) {
        if (excluded(event.getCommandName())) {
            return;
        }
        while (commands.size() >= MAX_INFLIGHT_COMMANDS) {
            Long dropped = commandOrder.pollFirst();
            if (dropped == null) {
                break;
            }
            if (commands.remove(dropped) != null && inflightOverflowLogged.compareAndSet(false, true)) {
                log.warn("Dropped an in-flight Mongo command log because the buffer reached {}", MAX_INFLIGHT_COMMANDS);
            }
        }
        commands.put(event.getOperationId(), event.getCommand());
        commandOrder.addLast(event.getOperationId());
    }

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        if (excluded(event.getCommandName())) {
            return;
        }
        BsonDocument command = forget(event.getOperationId());
        long elapsed = event.getElapsedTime(TimeUnit.MILLISECONDS);
        safeLog(() -> write(event.getDatabaseName(), event.getCommandName(), command, event.getResponse(), elapsed, null));
    }

    @Override
    public void commandFailed(CommandFailedEvent event) {
        if (excluded(event.getCommandName())) {
            return;
        }
        BsonDocument command = forget(event.getOperationId());
        long elapsed = event.getElapsedTime(TimeUnit.MILLISECONDS);
        safeLog(() -> write(event.getDatabaseName(), event.getCommandName(), command, null, elapsed, event.getThrowable()));
    }

    private void write(String database, String commandName, BsonDocument command, BsonDocument response,
                       long elapsed, Throwable failure) {
        String statementId = MongoCommands.statementId(database, commandName, command);
        boolean slow = elapsed > properties.getLongQueryTime();
        boolean missingFilter = properties.isWarnEmptyFilter() && MongoCommands.emptyWriteFilter(commandName, command);
        boolean logStatement = failure != null || slow || missingFilter || log.isDebugEnabled();
        String statement = null;
        String parameters = null;
        if (logStatement) {
            if (properties.isExecutable()) {
                statement = MongoCommands.executable(command);
            } else {
                MongoCommandText.Placeholder placeholder = MongoCommandText.placeholder(command);
                statement = placeholder.text();
                parameters = placeholder.parameters();
            }
        }
        String elapsedText = elapsedText(statementId, elapsed);
        if (failure != null) {
            log.error("!!!mongo failed->{}!!!", statementId);
            log.error("==>{}", statementId);
            log.error("==>  Preparing: {}{}", lineSeparator, statement);
            if (parameters != null) {
                log.error("==> Parameters: {}", parameters);
            }
            log.error("{}{}", lineSeparator, elapsedText, failure);
        } else if (slow) {
            log.warn("!!!slow query->{}!!!", statementId);
            writeStatement(true, statementId, statement, parameters, elapsedText);
        } else if (log.isDebugEnabled()) {
            writeStatement(false, statementId, limit(statement), parameters, elapsedText);
        }
        if (missingFilter) {
            log.warn("!!!missing filter->{}!!!{}{}", statementId, lineSeparator, statement);
        }
        int size = MongoCommands.resultSize(response);
        if (properties.getLargeResultThreshold() > 0 && size > properties.getLargeResultThreshold()) {
            log.warn("!!!large result->{} size={}!!!", statementId, size);
        }
        if (metrics != null) {
            metrics.record(commandName, MongoCommands.collection(commandName, command), elapsed, slow);
        }
    }

    private void writeStatement(boolean warn, String statementId, String statement, String parameters, String elapsedText) {
        if (warn) {
            log.warn("==>{}", statementId);
            log.warn("==>  Preparing: {}{}", lineSeparator, statement);
            if (parameters != null) {
                log.warn("==> Parameters: {}", parameters);
            }
            log.warn("{}{}", lineSeparator, elapsedText);
            return;
        }
        log.debug("==>{}", statementId);
        log.debug("==>  Preparing: {}{}", lineSeparator, statement);
        if (parameters != null) {
            log.debug("==> Parameters: {}", parameters);
        }
        log.debug("{}{}", lineSeparator, elapsedText);
    }

    /**
     * 只截断 DEBUG 的 Preparing 文本。慢命令和失败命令保持完整，便于直接查看或执行。
     */
    private String limit(String statement) {
        int maxLength = properties.getMaxCommandLength();
        if (maxLength <= 0 || statement.length() <= maxLength) {
            return statement;
        }
        return statement.substring(0, maxLength) + "...";
    }

    private static String elapsedText(String statementId, long elapsed) {
        return "StopWatch '" + statementId + "': " + elapsed + " ms" + System.lineSeparator()
                + "---------------------------------------------" + System.lineSeparator()
                + "ms         %     Task name" + System.lineSeparator()
                + "---------------------------------------------" + System.lineSeparator()
                + elapsed + "       100%";
    }

    private BsonDocument forget(long operationId) {
        commandOrder.remove(operationId);
        return commands.remove(operationId);
    }

    private boolean excluded(String commandName) {
        return commandName != null && excludedCommands.contains(commandName.toLowerCase(Locale.ROOT));
    }

    private static void safeLog(Runnable action) {
        try {
            action.run();
        } catch (Exception exception) {
            log.warn("Failed to write Mongo command log", exception);
        }
    }
}
