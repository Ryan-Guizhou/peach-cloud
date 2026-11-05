-- Rejecting leaky bucket (policing); no unbounded queue.
-- KEYS[1] hash bucket
-- ARGV[1] leak rate/sec, ARGV[2] capacity, ARGV[3] cost
local rate, capacity, cost = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
if not rate or rate <= 0 or not capacity or capacity <= 0 or not cost or cost <= 0 then
  return redis.error_reply('INVALID_RATE_LIMIT_ARGUMENT')
end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local state = redis.call('HMGET', KEYS[1], 'water', 'updated')
local water = tonumber(state[1]) or 0
local updated = tonumber(state[2]) or now
water = math.max(0, water - math.max(0, now - updated) * rate / 1000)
local allowed = water + cost <= capacity
if allowed then water = water + cost end
redis.call('HSET', KEYS[1], 'water', water, 'updated', now)
redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / rate * 2000))
local retry = allowed and 0 or math.ceil((water + cost - capacity) / rate * 1000)
return {allowed and 1 or 0, math.max(0, math.floor(capacity - water)), math.ceil(water / rate * 1000), retry, capacity}
