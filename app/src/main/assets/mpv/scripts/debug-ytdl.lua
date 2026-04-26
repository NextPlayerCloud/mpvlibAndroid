function debug(msg)
    mp.msg.info(msg)
    mp.osd_message("YTDL-Debug: " .. msg, 8)
end

function run_test()
    debug("Checking ytdl...")
    local ytdl = mp.get_property("options/ytdl")
    debug("options/ytdl = " .. tostring(ytdl))
    local scriptOpts = mp.get_property("options/script-opts")
    debug("options/script-opts = " .. tostring(scriptOpts))
    local scriptOptsAppend = mp.get_property("options/script-opts-append")
    debug("options/script-opts-append = " .. tostring(scriptOptsAppend))
    local detectedPath = mp.get_property("user-data/mpv/ytdl/path")
    debug("detected ytdl path = " .. (detectedPath or "not detected yet"))
end

run_test()
