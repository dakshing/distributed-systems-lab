local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = tonumber(ARGV[3]) -- Milliseconds
local window_start = now - (window * 1000)

redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start) -- Remove old requests
local count = redis.call('ZCARD', key)  -- Get number of requests in the window

if count < limit then
    redis.call('ZADD', key, now, now)
    redis.call('EXPIRE', key, window)
    return 1
end
return 0