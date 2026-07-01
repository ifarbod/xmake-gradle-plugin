-- Copyright (C) 2020-present, TBOOX Open Source Group.
-- SPDX-License-Identifier: Apache-2.0

import("core.base.option")
import("core.project.config")
import("core.project.project")
import("core.tool.toolchain")
import("target.action.install")
import("target.action.uninstall")

local options = {
    {'o', "installdir", "kv", nil, "Set the install directory"},
    {'a', "arch", "kv", nil, "Set the installed target architecture"},
    {'c', "clean", "k", false, "Clean artifacts"},
    {nil, "targets", "vs", nil, "Set the targets"}
}

function _get_targets(target_names)
    local targets = {}
    target_names = target_names or {}
    if #target_names > 0 then
        for _, target_name in ipairs(target_names) do
            local target = project.target(target_name)
            if target:get("enabled") ~= false and target:is_shared() then
                table.insert(targets, target)
            else
                raise("invalid target(%s)!", target_name)
            end
        end
    else
        for _, target in ipairs(project.ordertargets()) do
            local default = target:get("default")
            if (default == nil or default == true) and target:is_shared() then
                table.insert(targets, target)
            end
        end
    end
    return targets
end

function _install_artifacts(targets, opt)
    assert(xmake.version():ge("2.9.6"), "please update xmake to >= 2.9.6")
    for _, target in ipairs(targets) do
        install(target, {installdir = opt.installdir, libdir = opt.arch, includedir = "include"})
    end
end

function _install_cxxstl_newer_ndk(opt)
    local installdir = path.join(opt.installdir, opt.arch)
    local ndk = get_config("ndk")
    local ndk_cxxstl = get_config("runtimes") or get_config("ndk_cxxstl")
    if ndk and ndk_cxxstl and ndk_cxxstl:endswith("_shared") and opt.arch then
        local toolchains_archs = {
            ["armeabi-v7a"] = "arm-linux-androideabi",
            ["arm64-v8a"] = "aarch64-linux-android",
            ["riscv64"] = "riscv64-linux-android",
            ["x86"] = "i686-linux-android",
            ["x86_64"] = "x86_64-linux-android"
        }
        local cxxstl_filename
        if ndk_cxxstl == "c++_shared" then
            cxxstl_filename = "libc++_shared.so"
        end
        if toolchains_archs[opt.arch] ~= nil and cxxstl_filename then
            local ndk_toolchain = toolchain.load("ndk", {plat = config.plat(), arch = config.arch()})
            local ndk_sysroot = ndk_toolchain:config("ndk_sysroot")
            local sdkdir = path.translate(format("%s/usr/lib/%s", ndk_sysroot, toolchains_archs[opt.arch]))
            os.vcp(path.join(sdkdir, cxxstl_filename), path.join(installdir, cxxstl_filename))
        end
    end
end

function _install_cxxstl(opt)
    local installdir = path.join(opt.installdir, opt.arch)
    local ndk = get_config("ndk")
    local ndk_cxxstl = get_config("runtimes") or get_config("ndk_cxxstl")
    if ndk and ndk_cxxstl and ndk_cxxstl:endswith("_shared") and opt.arch then
        local llvmstl = path.translate(format("%s/sources/cxx-stl/llvm-libc++", ndk))
        local gnustl
        if get_config("ndk_toolchains_ver") then
            gnustl = path.translate(format("%s/sources/cxx-stl/gnu-libstdc++/%s", ndk,
                                           get_config("ndk_toolchains_ver")))
        end
        local stlport = path.translate(format("%s/sources/cxx-stl/stlport", ndk))
        local sdkdir
        if ndk_cxxstl:startswith("c++") then
            sdkdir = llvmstl
        elseif ndk_cxxstl:startswith("gnustl") then
            sdkdir = gnustl
        elseif ndk_cxxstl:startswith("stlport") then
            sdkdir = stlport
        end
        local toolchains_archs = {
            ["armv5te"] = "armeabi",
            ["armv7-a"] = "armeabi-v7a",
            ["armeabi"] = "armeabi",
            ["armeabi-v7a"] = "armeabi-v7a",
            ["arm64-v8a"] = "arm64-v8a",
            ["riscv64"] = "riscv64",
            i386 = "x86",
            x86 = "x86",
            x86_64 = "x86_64",
            mips = "mips",
            mips64 = "mips64"
        }
        local filename
        if ndk_cxxstl == "c++_shared" then
            filename = "libc++_shared.so"
        elseif ndk_cxxstl == "gnustl_shared" then
            filename = "libgnustl_shared.so"
        elseif ndk_cxxstl == "stlport_shared" then
            filename = "libstlport_shared.so"
        end
        if sdkdir and toolchains_archs[opt.arch] and filename then
            os.vcp(path.join(sdkdir, "libs", toolchains_archs[opt.arch], filename), path.join(installdir, filename))
        end
    end
end

function _clean_artifacts(targets, opt)
    assert(xmake.version():ge("2.9.6"), "please update xmake to >= 2.9.6")
    for _, target in ipairs(targets) do
        uninstall(target, {installdir = opt.installdir, libdir = opt.arch, includedir = "include"})
    end
    local installdir = path.join(opt.installdir, opt.arch)
    local ndk_cxxstl = get_config("runtimes") or get_config("ndk_cxxstl")
    if ndk_cxxstl then
        local filename
        if ndk_cxxstl == "c++_shared" then
            filename = "libc++_shared.so"
        elseif ndk_cxxstl == "gnustl_shared" then
            filename = "libgnustl_shared.so"
        elseif ndk_cxxstl == "stlport_shared" then
            filename = "libstlport_shared.so"
        end
        if filename then
            os.tryrm(path.join(installdir, filename))
        end
    end
    if os.emptydir(installdir) then
        os.rmdir(installdir)
    end
end

function main(...)
    local argv = table.pack(...)
    local opt = option.parse(argv, options, "Install the target artifacts.", "",
                             "Usage: xmake l install_artifacts.lua [options]")
    assert(opt.installdir)
    config.load()
    local targets = _get_targets(opt.targets)
    assert(targets and #targets > 0,
           "no targets provided, make sure xmake.lua has at least one shared target or provide one")
    opt.arch = opt.arch or get_config("arch")
    if not opt.clean then
        _install_artifacts(targets, opt)
        if get_config("ndkver") >= 25 then
            _install_cxxstl_newer_ndk(opt)
        else
            _install_cxxstl(opt)
        end
    else
        _clean_artifacts(targets, opt)
    end
end
