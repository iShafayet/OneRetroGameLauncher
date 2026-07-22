# One Retro Game Launcher — common developer tasks
#
# Usage: make <target>
# Run `make help` (or plain `make`) for the full list.
#
# Store flavors: FLAVOR=fdroid (default) or FLAVOR=play

.DEFAULT_GOAL := help

APP_ID        := com.sayemshafayet.onereogamelauncher
MAIN_ACTIVITY := $(APP_ID)/.MainActivity
AVD           ?= Medium_Phone_API_36.1
FLAVOR        ?= fdroid

# Capitalize first letter for Gradle task names (fdroid -> Fdroid, play -> Play)
FLAVOR_CAP    := $(shell printf '%s' "$(FLAVOR)" | sed 's/^./\U&/')
APK_DEBUG     := app/build/outputs/apk/$(FLAVOR)/debug/app-$(FLAVOR)-debug.apk
VERSION_NAME  := $(shell grep '^VERSION_MAJOR=' version.properties | cut -d= -f2).$(shell grep '^VERSION_MINOR=' version.properties | cut -d= -f2).$(shell grep '^VERSION_PATCH=' version.properties | cut -d= -f2)-$(shell grep '^VERSION_PRERELEASE=' version.properties | cut -d= -f2)
LOCAL_APK     := .local/apk/orgl-$(FLAVOR)-debug-$(VERSION_NAME).apk

ANDROID_HOME  ?= $(HOME)/Android/Sdk
ADB           := $(ANDROID_HOME)/platform-tools/adb
EMULATOR      := $(ANDROID_HOME)/emulator/emulator

# Prefer system Temurin 21, then portable JDK under .local
ifneq ($(wildcard /usr/lib/jvm/temurin-21-jdk),)
  export JAVA_HOME := /usr/lib/jvm/temurin-21-jdk
else
  LOCAL_JDK := $(firstword $(wildcard .local/jdk/jdk-21*))
  ifneq ($(LOCAL_JDK),)
    export JAVA_HOME := $(abspath $(LOCAL_JDK))
  endif
endif
export ANDROID_HOME

GRADLEW := ./gradlew

.PHONY: help build assemble release test build-play build-fdroid \
	install uninstall reinstall \
	emulator emulator-list devices wait-device run launch run-play run-fdroid logcat \
	clean deep-clean doctor compile

help: ## Show this help
	@echo "One Retro Game Launcher — make targets"
	@echo
	@grep -E '^[a-zA-Z0-9_-]+:.*?## ' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'
	@echo
	@echo "Variables: FLAVOR=$(FLAVOR)  AVD=$(AVD)"
	@echo "           ANDROID_HOME=$(ANDROID_HOME)"
	@echo "           JAVA_HOME=$${JAVA_HOME:-<system default>}"

doctor: ## Check JDK, SDK, adb, and listed AVDs
	@echo "JAVA_HOME=$${JAVA_HOME:-unset}"
	@if [ -n "$${JAVA_HOME}" ] && [ -x "$${JAVA_HOME}/bin/java" ]; then \
		"$${JAVA_HOME}/bin/java" -version 2>&1 | head -1; \
	else \
		java -version 2>&1 | head -1 || true; \
	fi
	@echo "ANDROID_HOME=$(ANDROID_HOME)"
	@test -x "$(ADB)" && $(ADB) version | head -1 || echo "adb missing"
	@test -x "$(EMULATOR)" && echo "emulator OK" || echo "emulator missing"
	@echo "AVDs:"
	@$(EMULATOR) -list-avds 2>/dev/null || echo "  (none)"

compile: ## Compile Kotlin (debug) for FLAVOR
	$(GRADLEW) :app:compile$(FLAVOR_CAP)DebugKotlin

build: ## Build debug APK for FLAVOR (default: fdroid); copies to .local/apk and bumps patch
	$(GRADLEW) assemble$(FLAVOR_CAP)Debug
	@echo "Local copy: $(LOCAL_APK)"

assemble: build ## Alias for build

build-fdroid: ## Build FOSS / F-Droid debug APK
	$(MAKE) build FLAVOR=fdroid

build-play: ## Build Google Play debug APK
	$(MAKE) build FLAVOR=play

release: ## Build release APK for FLAVOR (unsigned unless signing is configured)
	$(GRADLEW) assemble$(FLAVOR_CAP)Release

install: build ## Build and install debug APK (FLAVOR) on a connected device/emulator
	$(ADB) install -r "$(APK_DEBUG)"

uninstall: ## Uninstall the app from the connected device/emulator
	$(ADB) uninstall $(APP_ID) || true

reinstall: uninstall install ## Uninstall then install fresh

emulator: ## Start the Android Virtual Device (AVD=$(AVD))
	@echo "Starting AVD: $(AVD)"
	@$(EMULATOR) -avd "$(AVD)" -netdelay none -netspeed full >/dev/null 2>&1 &
	@echo "Emulator launching in the background. Use: make wait-device"

emulator-list: ## List available AVDs
	@$(EMULATOR) -list-avds

devices: ## List adb devices
	@$(ADB) devices -l

wait-device: ## Wait until a device/emulator is online
	@$(ADB) wait-for-device
	@echo -n "Waiting for boot… "
	@$(ADB) shell 'while [ -z "$$(getprop sys.boot_completed 2>/dev/null)" ]; do sleep 1; done'
	@echo "ready"

run: install ## Install and launch FLAVOR (default: fdroid)
	$(ADB) shell am start -n "$(MAIN_ACTIVITY)"

launch: run ## Alias for run

run-fdroid: ## Install and launch the F-Droid flavor
	$(MAKE) run FLAVOR=fdroid

run-play: ## Install and launch the Google Play flavor
	$(MAKE) run FLAVOR=play

logcat: ## Follow app logcat (Ctrl+C to stop)
	$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(APP_ID) 2>/dev/null || echo 0) \
		|| $(ADB) logcat | grep -i orgl

clean: ## Clean Gradle build outputs
	$(GRADLEW) clean

deep-clean: clean ## Clean build outputs and local Gradle caches for this project
	rm -rf .gradle build app/build .gradle-home
