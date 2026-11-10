-- KEYS: processingLease, tokens, payload, attempts, completed
-- ARGV: stableId, fencingToken, nowMillis
if redis.call('HGET', KEYS[2], ARGV[1]) ~= ARGV[2] then
  return 0
end
redis.call('HDEL', KEYS[2], ARGV[1])
redis.call('ZREM', KEYS[1], ARGV[1])
redis.call('HDEL', KEYS[3], ARGV[1])
redis.call('HDEL', KEYS[4], ARGV[1])
redis.call('ZADD', KEYS[5], ARGV[3], ARGV[1])
return 1
