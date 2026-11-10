-- KEYS: processingLease, tokens, scheduled, attempts, payload, deadLetter, deadLetterMetadata
-- ARGV: stableId, fencingToken, maxAttempts, retryDelayMillis
if redis.call('HGET', KEYS[2], ARGV[1]) ~= ARGV[2] then
  return 0
end
local ts = redis.call('TIME')
local now = tonumber(ts[1]) * 1000 + math.floor(tonumber(ts[2]) / 1000)
redis.call('HDEL', KEYS[2], ARGV[1])
redis.call('ZREM', KEYS[1], ARGV[1])
local tries = tonumber(redis.call('HGET', KEYS[4], ARGV[1]) or '1')
if tries >= tonumber(ARGV[3]) then
  local body = redis.call('HGET', KEYS[5], ARGV[1])
  if body then
    redis.call('HSET', KEYS[6], ARGV[1], body)
    redis.call('HSET', KEYS[7], ARGV[1], tries)
  end
  redis.call('HDEL', KEYS[5], ARGV[1])
  redis.call('HDEL', KEYS[4], ARGV[1])
  return 2
end
redis.call('ZADD', KEYS[3], now + tonumber(ARGV[4]), ARGV[1])
return 1
