-- KEYS: scheduled, processingLease, tokens, attempts, payload, fencingCounter
-- ARGV: nowMillis, leaseMillis, maxBatch
local due = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, tonumber(ARGV[3]))
local out = {}
for _, id in ipairs(due) do
  if redis.call('ZREM', KEYS[1], id) == 1 then
    local body = redis.call('HGET', KEYS[5], id)
    if body then
      local fence = tostring(redis.call('INCR', KEYS[6]))
      redis.call('ZADD', KEYS[2], tonumber(ARGV[1]) + tonumber(ARGV[2]), id)
      redis.call('HSET', KEYS[3], id, fence)
      redis.call('HINCRBY', KEYS[4], id, 1)
      table.insert(out, id)
      table.insert(out, body)
      table.insert(out, fence)
    else
      redis.call('HDEL', KEYS[4], id)
    end
  end
end
return out
