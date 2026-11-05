-- KEYS[1] sorted-set key
-- ARGV[1] limit, ARGV[2] window ms, ARGV[3] cost, ARGV[4] unique request id
local limit, window, cost = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
if not limit or limit <= 0 or not window or window <= 0 or not cost or cost <= 0 then
  return redis.error_reply('INVALID_RATE_LIMIT_ARGUMENT')
end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local cutoff = now - window
redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', cutoff)
local used = redis.call('ZCARD', KEYS[1])
local allowed = used + cost <= limit
if allowed then
  for i = 1, cost do redis.call('ZADD', KEYS[1], now, ARGV[4] .. ':' .. i) end
end
redis.call('PEXPIRE', KEYS[1], window)
local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
local retry = 0
if not allowed and #oldest >= 2 then retry = math.max(1, tonumber(oldest[2]) + window - now) end
return {allowed and 1 or 0, math.max(0, limit - used - (allowed and cost or 0)), window, retry, limit}
