-- Weighted sliding-window counter stored in one hash key for Redis Cluster compatibility.
-- KEYS[1] hash bucket key
-- ARGV[1] limit, ARGV[2] window ms, ARGV[3] cost
local limit, window, cost = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
if not limit or limit <= 0 or not window or window <= 0 or not cost or cost <= 0 or cost > limit then
  return redis.error_reply('INVALID_RATE_LIMIT_ARGUMENT')
end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local currentWindow = math.floor(now / window)
local state = redis.call('HMGET', KEYS[1], 'windowId', 'current', 'previous')
local storedWindow = tonumber(state[1])
local current = tonumber(state[2]) or 0
local previous = tonumber(state[3]) or 0
if not storedWindow then
  current = 0
  previous = 0
elseif storedWindow == currentWindow then
  -- The stored current and previous counters still match this window.
elseif storedWindow == currentWindow - 1 then
  previous = current
  current = 0
else
  current = 0
  previous = 0
end
local elapsed = now - currentWindow * window
local remainingInWindow = window - elapsed
local weighted = previous * remainingInWindow / window + current
local allowed = weighted + cost <= limit
if allowed then current = current + cost end
redis.call('HSET', KEYS[1],
  'windowId', currentWindow,
  'current', current,
  'previous', previous)
redis.call('PEXPIRE', KEYS[1], window * 3)
local remaining = math.max(0, math.floor(limit - weighted - (allowed and cost or 0)))
local retry = 0
if not allowed then
  local threshold = limit - cost
  if current <= threshold and previous > 0 then
    retry = math.max(1, math.ceil(remainingInWindow - ((threshold - current) * window / previous)))
  elseif current > threshold and current > 0 then
    retry = math.max(1, math.ceil(remainingInWindow + ((current - threshold) * window / current)))
  else
    retry = math.max(1, remainingInWindow)
  end
end
return {allowed and 1 or 0, remaining, remainingInWindow, retry, limit}
