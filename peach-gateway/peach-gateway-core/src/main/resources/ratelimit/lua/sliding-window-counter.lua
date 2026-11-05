-- KEYS[1] current window, KEYS[2] previous window
-- ARGV[1] limit, ARGV[2] window ms, ARGV[3] cost
local limit, window, cost = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
if not limit or limit <= 0 or not window or window <= 0 or not cost or cost <= 0 then
  return redis.error_reply('INVALID_RATE_LIMIT_ARGUMENT')
end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local currentWindow = math.floor(now / window)
local currentKey = KEYS[1] .. ':' .. currentWindow
local previousKey = KEYS[2] .. ':' .. (currentWindow - 1)
local elapsed = now - currentWindow * window
local current = tonumber(redis.call('GET', currentKey) or '0')
local previous = tonumber(redis.call('GET', previousKey) or '0')
local weighted = previous * (window - elapsed) / window + current
local allowed = weighted + cost <= limit
if allowed then
  current = redis.call('INCRBY', currentKey, cost)
  redis.call('PEXPIRE', currentKey, window * 2)
end
local remaining = math.max(0, math.floor(limit - weighted - (allowed and cost or 0)))
return {allowed and 1 or 0, remaining, window - elapsed, allowed and 0 or math.max(1, math.ceil(window - elapsed)), limit}
