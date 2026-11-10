-- KEYS: deadLetter, deadLetterMetadata, scheduled, payload, attempts
-- ARGV: stableId, delayMillis
local body = redis.call('HGET', KEYS[1], ARGV[1])
if not body then
  return 0
end
if redis.call('HEXISTS', KEYS[4], ARGV[1]) == 1 then
  return 0
end
local ts = redis.call('TIME')
local now = tonumber(ts[1]) * 1000 + math.floor(tonumber(ts[2]) / 1000)
redis.call('HSET', KEYS[4], ARGV[1], body)
redis.call('HSET', KEYS[5], ARGV[1], 0)
redis.call('ZADD', KEYS[3], now + tonumber(ARGV[2]), ARGV[1])
redis.call('HDEL', KEYS[1], ARGV[1])
redis.call('HDEL', KEYS[2], ARGV[1])
return 1
