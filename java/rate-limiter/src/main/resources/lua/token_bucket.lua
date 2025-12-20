local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2]) -- tokens per second
local now = tonumber(ARGV[3])
local requested = 1

local last_refill = tonumber(redis.call('HGET', key, 'last_refill') or 0)
local tokens = tonumber(redis.call('HGET', key, 'tokens') or max_tokens)

if last_refill > 0 then
    local elapsed = math.max(0, now - last_refill)
    local refill = elapsed * refill_rate
    if refill > 0 then
        tokens = math.min(max_tokens, tokens + refill)
        last_refill = now
    end
else
    last_refill = now
end

if tokens >= requested then
    tokens = tokens - requested
    redis.call('HSET', key, 'last_refill', last_refill, 'tokens', tokens)
    redis.call('EXPIRE', key, 60)
    return 1
end
return 0