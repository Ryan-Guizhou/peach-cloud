-- KEYS: processingLease, tokens, scheduled, completed
-- ARGV: maxBatch, retentionMillis
local ts = redis.call('TIME')
local now = tonumber(ts[1]) * 1000 + math.floor(tonumber(ts[2]) / 1000)
redis.call('ZREMRANGEBYSCORE', KEYS[4], '-inf', now - tonumber(ARGV[2]))
local expired = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', now, 'LIMIT', 0, tonumber(ARGV[1]))
local moved = 0
for _, id in ipairs(expired) do
  if redis.call('ZREM', KEYS[1], id) == 1 then
    redis.call('HDEL', KEYS[2], id)
    redis.call('ZADD', KEYS[3], now, id)
    moved = moved + 1
  end
end
return moved
