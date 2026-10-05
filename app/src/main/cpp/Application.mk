APP_ABI := arm64-v8a
APP_STL := c++_static
APP_OPTIM := release
APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true
APP_LDFLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384
