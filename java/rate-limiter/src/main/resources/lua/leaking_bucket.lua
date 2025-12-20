local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local leak_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local last_leak = tonumber(redis.call('HGET', key, 'last_leak') or 0)
local water = tonumber(redis.call('HGET', key, 'water') or 0)

local elapsed = math.max(0, now - last_leak) -- time since last request came in
local leaked = elapsed * leak_rate

if leaked > 0 then
    water = math.max(0, water - leaked)
    last_leak = now
end

if water < capacity then
    water = water + 1
    redis.call('HSET', key, 'last_leak', last_leak, 'water', water)
    redis.call('EXPIRE', key, 60)
    return 1
end
return 0