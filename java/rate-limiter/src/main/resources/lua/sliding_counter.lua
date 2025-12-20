local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = tonumber(ARGV[3]) -- Seconds

local current_window = math.floor(now / window) -- bucketing time into windows
local prev_window = current_window - 1

local cur_key = key .. ':' .. current_window
local prev_key = key .. ':' .. prev_window

local cur_count = tonumber(redis.call('GET', cur_key) or 0)
local prev_count = tonumber(redis.call('GET', prev_key) or 0)

local weight = 1 - ((now % window) / window)
local estimate = cur_count + (prev_count * weight)

if estimate < limit then
    redis.call('INCR', cur_key)
    redis.call('EXPIRE', cur_key, window * 2)
    return 1
end
return 0