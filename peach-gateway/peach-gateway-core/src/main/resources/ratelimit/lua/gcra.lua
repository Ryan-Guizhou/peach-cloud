-- Generic Cell Rate Algorithm with weighted request cost.
-- KEYS[1] theoretical arrival time (TAT)
-- ARGV[1] rate/sec, ARGV[2] burst capacity, ARGV[3] cost
local rate, burst, cost = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
if not rate or rate <= 0 or not burst or burst <= 0 or not cost or cost <= 0 or cost > burst then
  return redis.error_reply('INVALID_RATE_LIMIT_ARGUMENT')
end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local interval = 1000 / rate
local tat = tonumber(redis.call('GET', KEYS[1])) or now
local candidateTat = math.max(now, tat) + cost * interval
local earliest = candidateTat - burst * interval
local allowed = now >= earliest
if allowed then
  tat = candidateTat
  redis.call('SET', KEYS[1], tat, 'PX', math.ceil((burst + 1) * interval))
end
local retry = allowed and 0 or math.max(1, math.ceil(earliest - now))
local remaining = allowed and math.max(0, math.floor((now + burst * interval - tat) / interval)) or 0
local resetAfter = math.ceil(math.max(0, (allowed and tat or candidateTat) - now))
return {allowed and 1 or 0, remaining, resetAfter, retry, burst}
