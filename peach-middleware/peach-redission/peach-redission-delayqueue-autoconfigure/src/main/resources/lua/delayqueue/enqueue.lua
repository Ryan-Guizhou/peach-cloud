-- KEYS: scheduled, payload, attempts, dlq, completed
-- ARGV: stableId, payload, delayMillis, dedupeRetentionMillis
local ts = redis.call('TIME')
local now = tonumber(ts[1]) * 1000 + math.floor(tonumber(ts[2]) / 1000)
redis.call('ZREMRANGEBYSCORE', KEYS[5], '-inf', now - tonumber(ARGV[4]))
if redis.call('HEXISTS', KEYS[2], ARGV[1]) == 1
  or redis.call('HEXISTS', KEYS[4], ARGV[1]) == 1
  or redis.call('ZSCORE', KEYS[5], ARGV[1]) then
  return 0
end
redis.call('HSET', KEYS[2], ARGV[1], ARGV[2])
redis.call('HSET', KEYS[3], ARGV[1], 0)
redis.call('ZADD', KEYS[1], now + tonumber(ARGV[3]), ARGV[1])
return 1
