-- Fixed window counter stored in one hash key for Redis Cluster compatibility.
-- KEYS[1] stable hash bucket key
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
local state = redis.call('HMGET', KEYS[1], 'windowId', 'count')
local storedWindow = tonumber(state[1])
local count = tonumber(state[2]) or 0
if storedWindow ~= windowId then
  count = 0
end
local allowed = count + cost <= limit
if allowed then
  count = count + cost
end
redis.call('HSET', KEYS[1], 'windowId', windowId, 'count', count)
redis.call('PEXPIRE', KEYS[1], window * 2)
local elapsed = now - windowId * window
local resetAfter = window - elapsed
return {allowed and 1 or 0, math.max(0, limit - count), resetAfter, allowed and 0 or resetAfter, limit}
