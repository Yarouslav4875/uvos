cmake_minimum_required(VERSION 3.22.1)
project("uvos_terminal_native")

add_library(
    uvos_pty
    SHARED
    pty_bridge.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    uvos_pty
    ${log-lib}
)
