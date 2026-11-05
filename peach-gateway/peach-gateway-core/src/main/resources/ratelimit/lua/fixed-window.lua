-- KEYS[1] bucket key
-- ARGV[1] limit, ARGV[2] window milliseconds, ARGV[3] cost
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local cost = tonumber(ARGV[3])
if not limit or limit <= 0 or not window or window <= 0 or not cost or cost <= 0 then
  return redis.error_reply('INVALID_RATE_LIMIT_ARGUMENT')
end
local current = redis.call('INCRBY', KEYS[1], cost)
if current == cost then redis.call('PEXPIRE', KEYS[1], window) end
local ttl = redis.call('PTTL', KEYS[1])
local allowed = current <= limit
return {allowed and 1 or 0, math.max(0, limit - current), math.max(0, ttl), allowed and 0 or math.max(1, ttl), limit}
