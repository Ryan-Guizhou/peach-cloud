-- KEYS: processingLease, tokens
-- ARGV: stableId, fencingToken, leaseMillis
if redis.call('HGET', KEYS[2], ARGV[1]) ~= ARGV[2] then
  return 0
end
local ts = redis.call('TIME')
local now = tonumber(ts[1]) * 1000 + math.floor(tonumber(ts[2]) / 1000)
redis.call('ZADD', KEYS[1], now + tonumber(ARGV[3]), ARGV[1])
return 1
