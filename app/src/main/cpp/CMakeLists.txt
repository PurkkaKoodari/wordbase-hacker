cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS_DEBUG "-O3")
set(CMAKE_CXX_FLAGS_MINSIZEREL "-O3")
set(CMAKE_CXX_FLAGS_RELWITHDEBINFO "-O3")
set(CMAKE_CXX_FLAGS_RELEASE "-O3")

add_library(solver SHARED
            adapter.cpp
            solver.cpp
            util.cpp)

target_link_libraries(solver
                      android
                      log)
