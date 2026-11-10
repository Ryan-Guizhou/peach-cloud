-- KEYS: processingLease, tokens, scheduled, completed
-- ARGV: nowMillis, maxBatch, retentionMillis
redis.call('ZREMRANGEBYSCORE', KEYS[4], '-inf', tonumber(ARGV[1]) - tonumber(ARGV[3]))
local expired = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, tonumber(ARGV[2]))
local moved = 0
for _, id in ipairs(expired) do
  if redis.call('ZREM', KEYS[1], id) == 1 then
    redis.call('HDEL', KEYS[2], id)
    redis.call('ZADD', KEYS[3], ARGV[1], id)
    moved = moved + 1
  end
end
return moved
