-- KEYS: scheduled, processingLease, deadLetter, completed
return {redis.call('ZCARD', KEYS[1]), redis.call('ZCARD', KEYS[2]),
        redis.call('HLEN', KEYS[3]), redis.call('ZCARD', KEYS[4])}
