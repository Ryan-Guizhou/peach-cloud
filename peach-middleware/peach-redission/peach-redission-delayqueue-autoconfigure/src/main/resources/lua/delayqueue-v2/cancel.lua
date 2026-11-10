-- KEYS: scheduled, payload, attempts
-- ARGV: stableId
if redis.call('ZREM', KEYS[1], ARGV[1]) ~= 1 then
  return 0
end
redis.call('HDEL', KEYS[2], ARGV[1])
redis.call('HDEL', KEYS[3], ARGV[1])
return 1
