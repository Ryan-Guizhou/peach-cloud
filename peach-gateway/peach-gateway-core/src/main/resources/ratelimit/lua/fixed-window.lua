-- KEYS[1] stable bucket key
-- ARGV[1] limit, ARGV[2] window milliseconds, ARGV[3] cost
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local cost = tonumber(ARGV[3])
if not limit or limit <= 0 or not window or window <= 0 or not cost or cost <= 0 or cost > limit then
  return redis.error_reply('INVALID_RATE_LIMIT_ARGUMENT')
end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local windowId = math.floor(now / window)
local bucketKey = KEYS[1] .. ':' .. windowId
local current = redis.call('INCRBY', bucketKey, cost)
if current == cost then redis.call('PEXPIRE', bucketKey, window * 2) end
local elapsed = now - windowId * window
local resetAfter = window - elapsed
local allowed = current <= limit
return {allowed and 1 or 0, math.max(0, limit - current), resetAfter, allowed and 0 or resetAfter, limit}
