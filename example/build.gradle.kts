plugins {
    java
    id("ir.ifarbod.xmake-gradle-plugin")
}

xmake {
    path = "jni/xmake.lua"
    buildMode = "debug"
    abiFilters("arm64-v8a", "x86_64")
    targets("example")
}
