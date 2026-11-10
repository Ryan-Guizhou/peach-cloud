-- KEYS: deadLetter, deadLetterMetadata, scheduled, payload, attempts
-- ARGV: stableId, dueMillis
local body = redis.call('HGET', KEYS[1], ARGV[1])
if not body then
  return 0
end
if redis.call('HEXISTS', KEYS[4], ARGV[1]) == 1 then
  return 0
end
redis.call('HSET', KEYS[4], ARGV[1], body)
redis.call('HSET', KEYS[5], ARGV[1], 0)
redis.call('ZADD', KEYS[3], ARGV[2], ARGV[1])
redis.call('HDEL', KEYS[1], ARGV[1])
redis.call('HDEL', KEYS[2], ARGV[1])
return 1
