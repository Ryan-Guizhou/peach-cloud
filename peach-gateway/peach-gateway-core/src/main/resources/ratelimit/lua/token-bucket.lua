-- KEYS[1] hash bucket
-- ARGV[1] replenish rate/sec, ARGV[2] capacity, ARGV[3] cost
local rate, capacity, cost = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3])
if not rate or rate <= 0 or not capacity or capacity <= 0 or not cost or cost <= 0 then
  return redis.error_reply('INVALID_RATE_LIMIT_ARGUMENT')
end
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local state = redis.call('HMGET', KEYS[1], 'tokens', 'updated')
local tokens = tonumber(state[1]) or capacity
local updated = tonumber(state[2]) or now
local elapsed = math.max(0, now - updated)
tokens = math.min(capacity, tokens + elapsed * rate / 1000)
local allowed = tokens >= cost
if allowed then tokens = tokens - cost end
redis.call('HSET', KEYS[1], 'tokens', tokens, 'updated', now)
redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / rate * 2000))
local retry = allowed and 0 or math.ceil((cost - tokens) / rate * 1000)
return {allowed and 1 or 0, math.floor(tokens), math.ceil((capacity - tokens) / rate * 1000), retry, capacity}
