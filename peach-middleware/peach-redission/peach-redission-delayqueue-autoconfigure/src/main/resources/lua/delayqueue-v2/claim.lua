-- KEYS: scheduled, processingLease, tokens, attempts, payload, fencingCounter
-- ARGV: leaseMillis, maxBatch
local ts = redis.call('TIME')
local now = tonumber(ts[1]) * 1000 + math.floor(tonumber(ts[2]) / 1000)
local due = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', now, 'LIMIT', 0, tonumber(ARGV[2]))
local out = {}
for _, id in ipairs(due) do
  if redis.call('ZREM', KEYS[1], id) == 1 then
    local body = redis.call('HGET', KEYS[5], id)
    if body then
      local fence = tostring(redis.call('INCR', KEYS[6]))
      redis.call('ZADD', KEYS[2], now + tonumber(ARGV[1]), id)
      redis.call('HSET', KEYS[3], id, fence)
      local attempts = redis.call('HINCRBY', KEYS[4], id, 1)
      table.insert(out, id)
      table.insert(out, body)
      table.insert(out, fence)
      table.insert(out, attempts)
    else
      redis.call('HDEL', KEYS[4], id)
    end
  end
end
return out
