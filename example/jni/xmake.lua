add_rules("mode.debug", "mode.release")

target("example")
    set_kind("shared")
    add_files("example.cpp")
