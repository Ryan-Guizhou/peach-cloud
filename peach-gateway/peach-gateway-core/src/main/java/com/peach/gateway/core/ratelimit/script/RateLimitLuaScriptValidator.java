package com.peach.gateway.core.ratelimit.script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 限流 Lua 脚本的受限执行契约校验器。
 *
 * <p>仅允许脚本直接访问 KEYS[1]，并限制 Redis 命令为当前限流算法所需的只读/单 Key 命令集合。
 * 该校验器不是通用 Lua 解析器；不满足受限语法的脚本会被拒绝，而不是尝试猜测其执行行为。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
final class RateLimitLuaScriptValidator {

    private static final Set<String> KEYED_REDIS_COMMANDS = Set.of(
            "GET",
            "SET",
            "HMGET",
            "HSET",
            "PEXPIRE",
            "ZREMRANGEBYSCORE",
            "ZCARD",
            "ZADD",
            "ZRANGE");

    private static final Set<String> FORBIDDEN_GLOBALS = Set.of(
            "_G",
            "collectgarbage",
            "dofile",
            "debug",
            "ffi",
            "getfenv",
            "io",
            "load",
            "loadfile",
            "loadstring",
            "module",
            "os",
            "package",
            "require",
            "setfenv");

    private RateLimitLuaScriptValidator() {
    }

    static void validate(String script) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("Rate limit Lua script must not be empty");
        }

        List<Token> tokens = tokenize(script);
        Set<Integer> allowedKeyReferences = new HashSet<>();
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.type() != TokenType.IDENTIFIER) {
                continue;
            }
            if (FORBIDDEN_GLOBALS.contains(token.text())) {
                throw invalid("forbidden Lua global: " + token.text());
            }
            if ("KEYS".equals(token.text())) {
                validateKeyReference(tokens, index);
            }
            if ("redis".equals(token.text())) {
                validateRedisAccess(tokens, index, allowedKeyReferences);
            }
        }

        for (Token token : tokens) {
            if (token.type() == TokenType.IDENTIFIER
                    && "KEYS".equals(token.text())
                    && !allowedKeyReferences.contains(token.index())) {
                throw invalid("KEYS must be used directly as the first key argument of an approved redis.call");
            }
        }
    }

    private static void validateRedisAccess(List<Token> tokens,
                                            int redisIndex,
                                            Set<Integer> allowedKeyReferences) {
        if (!matches(tokens, redisIndex + 1, TokenType.SYMBOL, ".")
                || redisIndex + 2 >= tokens.size()
                || tokens.get(redisIndex + 2).type() != TokenType.IDENTIFIER) {
            throw invalid("redis must not be aliased or accessed dynamically");
        }

        String method = tokens.get(redisIndex + 2).text();
        if (!"call".equals(method) && !"error_reply".equals(method)) {
            throw invalid("unsupported redis API: " + method);
        }
        int openParenthesis = redisIndex + 3;
        if (!matches(tokens, openParenthesis, TokenType.SYMBOL, "(")) {
            throw invalid("redis API must be invoked directly");
        }

        ParsedArguments parsed = parseArguments(tokens, openParenthesis);
        List<List<Token>> arguments = parsed.arguments();
        if ("error_reply".equals(method)) {
            if (arguments.size() != 1 || !isStringLiteral(arguments.get(0))) {
                throw invalid("redis.error_reply requires one literal message");
            }
            return;
        }

        if (arguments.isEmpty() || !isStringLiteral(arguments.get(0))) {
            throw invalid("redis.call command must be a literal string");
        }
        String command = arguments.get(0).get(0).text().toUpperCase(Locale.ROOT);
        if ("TIME".equals(command)) {
            if (arguments.size() != 1) {
                throw invalid("TIME must not receive key arguments");
            }
            return;
        }
        if (!KEYED_REDIS_COMMANDS.contains(command)) {
            throw invalid("redis command is not allowed: " + command);
        }
        if (arguments.size() < 2 || !isDirectSingleKey(arguments.get(1))) {
            throw invalid("redis command must use KEYS[1] as its only key");
        }
        allowedKeyReferences.add(arguments.get(1).get(0).index());
    }

    private static void validateKeyReference(List<Token> tokens, int index) {
        if (!matches(tokens, index + 1, TokenType.SYMBOL, "[")
                || !matches(tokens, index + 2, TokenType.NUMBER, "1")
                || !matches(tokens, index + 3, TokenType.SYMBOL, "]")) {
            throw invalid("only the literal KEYS[1] reference is allowed");
        }
    }

    private static boolean isDirectSingleKey(List<Token> expression) {
        return expression.size() == 4
                && expression.get(0).type() == TokenType.IDENTIFIER
                && "KEYS".equals(expression.get(0).text())
                && expression.get(1).type() == TokenType.SYMBOL
                && "[".equals(expression.get(1).text())
                && expression.get(2).type() == TokenType.NUMBER
                && "1".equals(expression.get(2).text())
                && expression.get(3).type() == TokenType.SYMBOL
                && "]".equals(expression.get(3).text());
    }

    private static boolean isStringLiteral(List<Token> expression) {
        return expression.size() == 1 && expression.get(0).type() == TokenType.STRING;
    }

    private static ParsedArguments parseArguments(List<Token> tokens, int openParenthesis) {
        List<List<Token>> arguments = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int parentheses = 0;
        int brackets = 0;
        int braces = 0;

        for (int index = openParenthesis + 1; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            String value = token.text();
            if (token.type() == TokenType.SYMBOL) {
                switch (value) {
                    case "(" -> parentheses++;
                    case ")" -> {
                        if (parentheses == 0 && brackets == 0 && braces == 0) {
                            if (!current.isEmpty() || !arguments.isEmpty()) {
                                arguments.add(List.copyOf(current));
                            }
                            return new ParsedArguments(List.copyOf(arguments), index);
                        }
                        parentheses--;
                    }
                    case "[" -> brackets++;
                    case "]" -> brackets--;
                    case "{" -> braces++;
                    case "}" -> braces--;
                    case "," -> {
                        if (parentheses == 0 && brackets == 0 && braces == 0) {
                            arguments.add(List.copyOf(current));
                            current.clear();
                            continue;
                        }
                    }
                    default -> {
                    }
                }
                if (parentheses < 0 || brackets < 0 || braces < 0) {
                    throw invalid("unbalanced Lua expression");
                }
            }
            current.add(token);
        }
        throw invalid("unterminated redis API call");
    }

    private static List<Token> tokenize(String script) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < script.length()) {
            char current = script.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (current == '-' && index + 1 < script.length() && script.charAt(index + 1) == '-') {
                index = skipComment(script, index + 2);
                continue;
            }
            if (current == '\'' || current == '"') {
                int end = skipQuotedString(script, index, current);
                tokens.add(new Token(TokenType.STRING, script.substring(index + 1, end - 1), index));
                index = end;
                continue;
            }
            if (current == '[') {
                int longStringEnd = skipLongBracket(script, index);
                if (longStringEnd > index) {
                    tokens.add(new Token(TokenType.STRING, "", index));
                    index = longStringEnd;
                    continue;
                }
            }
            if (isIdentifierStart(current)) {
                int end = index + 1;
                while (end < script.length() && isIdentifierPart(script.charAt(end))) {
                    end++;
                }
                tokens.add(new Token(TokenType.IDENTIFIER, script.substring(index, end), index));
                index = end;
                continue;
            }
            if (Character.isDigit(current)) {
                int end = index + 1;
                while (end < script.length() && Character.isDigit(script.charAt(end))) {
                    end++;
                }
                tokens.add(new Token(TokenType.NUMBER, script.substring(index, end), index));
                index = end;
                continue;
            }
            tokens.add(new Token(TokenType.SYMBOL, String.valueOf(current), index));
            index++;
        }
        return List.copyOf(tokens);
    }

    private static int skipComment(String script, int start) {
        int longCommentEnd = skipLongBracket(script, start);
        if (longCommentEnd > start) {
            return longCommentEnd;
        }
        int end = script.indexOf('\n', start);
        return end < 0 ? script.length() : end + 1;
    }

    private static int skipQuotedString(String script, int start, char quote) {
        int index = start + 1;
        while (index < script.length()) {
            char current = script.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            if (current == quote) {
                return index + 1;
            }
            index++;
        }
        throw invalid("unterminated Lua string");
    }

    private static int skipLongBracket(String script, int start) {
        if (start >= script.length() || script.charAt(start) != '[') {
            return start;
        }
        int index = start + 1;
        while (index < script.length() && script.charAt(index) == '=') {
            index++;
        }
        if (index >= script.length() || script.charAt(index) != '[') {
            return start;
        }
        String closing = "]" + "=".repeat(index - start - 1) + "]";
        int end = script.indexOf(closing, index + 1);
        if (end < 0) {
            throw invalid("unterminated Lua long string or comment");
        }
        return end + closing.length();
    }

    private static boolean matches(List<Token> tokens, int index, TokenType type, String text) {
        return index >= 0 && index < tokens.size()
                && tokens.get(index).type() == type
                && text.equals(tokens.get(index).text());
    }

    private static boolean isIdentifierStart(char value) {
        return value == '_' || Character.isLetter(value);
    }

    private static boolean isIdentifierPart(char value) {
        return value == '_' || Character.isLetterOrDigit(value);
    }

    private static IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("Invalid rate limit Lua script contract: " + reason);
    }

    private enum TokenType {
        IDENTIFIER,
        NUMBER,
        STRING,
        SYMBOL
    }

    private record Token(TokenType type, String text, int index) {
    }

    private record ParsedArguments(List<List<Token>> arguments, int endIndex) {
    }
}
