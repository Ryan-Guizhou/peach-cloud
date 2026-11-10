-- KEYS: processingLease, tokens
-- ARGV: stableId, fencingToken, leaseDeadlineMillis
if redis.call('HGET', KEYS[2], ARGV[1]) ~= ARGV[2] then
  return 0
end
redis.call('ZADD', KEYS[1], ARGV[3], ARGV[1])
return 1
