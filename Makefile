# One Retro Game Launcher — common developer tasks
#
# Usage: make <target>
# Run `make help` (or plain `make`) for the full list.
#
# Store flavors: FLAVOR=foss (default) or FLAVOR=play

.DEFAULT_GOAL := help

APP_ID        := $(if $(filter play,$(FLAVOR)),com.sayemshafayet.onereogamelauncher,com.sayemshafayet.orglfoss)
# applicationId may differ from the Kotlin namespace; use the fully-qualified activity.
MAIN_ACTIVITY := $(APP_ID)/com.sayemshafayet.onereogamelauncher.MainActivity
AVD           ?= Medium_Phone_API_36.1
FLAVOR        ?= foss

# Capitalize first letter for Gradle task names (foss -> Foss, play -> Play)
FLAVOR_CAP    := $(shell printf '%s' "$(FLAVOR)" | sed 's/^./\U&/')
APK_DEBUG     := app/build/outputs/apk/$(FLAVOR)/debug/app-$(FLAVOR)-debug.apk
APK_RELEASE   := app/build/outputs/apk/$(FLAVOR)/release/app-$(FLAVOR)-release.apk
AAB_RELEASE   := app/build/outputs/bundle/$(FLAVOR)Release/app-$(FLAVOR)-release.aab
VERSION_NAME  := $(shell grep '^VERSION_MAJOR=' version.properties | cut -d= -f2).$(shell grep '^VERSION_MINOR=' version.properties | cut -d= -f2).$(shell grep '^VERSION_PATCH=' version.properties | cut -d= -f2)-$(shell grep '^VERSION_PRERELEASE=' version.properties | cut -d= -f2)+$(shell grep '^VERSION_BUILD=' version.properties | cut -d= -f2)
LOCAL_APK     := .local/apk/orgl-$(FLAVOR)-debug-$(VERSION_NAME).apk
LOCAL_RELEASE_APK := .local/apk/orgl-$(FLAVOR)-release-$(VERSION_NAME).apk
LOCAL_AAB     := .local/bundle/orgl-$(FLAVOR)-release-$(VERSION_NAME).aab
LOCAL_SHA256  := .local/apk/orgl-$(FLAVOR)-release-$(VERSION_NAME).sha256
BUILD_TOOLS_DIR := $(lastword $(sort $(wildcard $(ANDROID_HOME)/build-tools/*)))
APKSIGNER     := $(BUILD_TOOLS_DIR)/apksigner

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

.PHONY: help build assemble release release-foss release-play bundle bundle-foss bundle-play checksum checksum-foss checksum-play cert cert-foss cert-play verify verify-foss verify-play test build-play build-foss build-fdroid bump \
	install uninstall reinstall \
	emulator emulator-list devices wait-device run launch run-play run-foss run-fdroid logcat \
	clean deep-clean doctor compile \
	_release _bundle _checksum _cert _verify

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

build: ## Build debug APK for FLAVOR (default: foss); copies/overwrites .local/apk
	$(GRADLEW) assemble$(FLAVOR_CAP)Debug
	@echo "Local copy: $(LOCAL_APK)"

assemble: build ## Alias for build

build-foss: ## Build FOSS debug APK
	$(MAKE) build FLAVOR=foss

build-fdroid: ## Legacy alias for the FOSS debug APK
	$(MAKE) build FLAVOR=foss

build-play: ## Build Google Play debug APK
	$(MAKE) build FLAVOR=play

bump: ## Increment VERSION_BUILD in version.properties (versionCode / +build)
	$(GRADLEW) :app:bumpVersion

release bundle checksum cert verify:
	$(error Use an explicit flavor suffix, e.g. make release-foss or make release-play)

release-foss: ## Build signed FOSS release APK
	$(MAKE) _release FLAVOR=foss

release-play: ## Build signed Play release APK
	$(MAKE) _release FLAVOR=play

_release:
	@if [ "$(FLAVOR)" = "play" ]; then \
		test -f keystore-play.properties || { \
			echo "Missing keystore-play.properties — copy keystore-play.properties.example and create your Play upload key."; \
			exit 1; \
		}; \
	else \
		test -f keystore-foss.properties || { \
			echo "Missing keystore-foss.properties — copy keystore-foss.properties.example and create orgl-foss.jks."; \
			exit 1; \
		}; \
	fi
	$(GRADLEW) assemble$(FLAVOR_CAP)Release
	@echo "Local copy: $(LOCAL_RELEASE_APK)"

bundle-foss: ## Build signed FOSS release AAB
	$(MAKE) _bundle FLAVOR=foss

bundle-play: ## Build signed Play release AAB (what Google Play expects)
	$(MAKE) _bundle FLAVOR=play

_bundle:
	@if [ "$(FLAVOR)" = "play" ]; then \
		test -f keystore-play.properties || { \
			echo "Missing keystore-play.properties — copy keystore-play.properties.example and create your Play upload key."; \
			exit 1; \
		}; \
	else \
		test -f keystore-foss.properties || { \
			echo "Missing keystore-foss.properties — copy keystore-foss.properties.example and create orgl-foss.jks."; \
			exit 1; \
		}; \
	fi
	$(GRADLEW) bundle$(FLAVOR_CAP)Release
	@echo "Local copy: $(LOCAL_AAB)"

checksum-foss: release-foss ## Write SHA-256 for the signed FOSS release APK
	$(MAKE) _checksum FLAVOR=foss

checksum-play: release-play ## Write SHA-256 for the signed Play release APK
	$(MAKE) _checksum FLAVOR=play

_checksum:
	@mkdir -p .local/apk
	@sha256sum "$(LOCAL_RELEASE_APK)" > "$(LOCAL_SHA256)"
	@echo "Wrote $(LOCAL_SHA256)"

cert-foss: release-foss ## Print signing cert info for the signed FOSS release APK
	$(MAKE) _cert FLAVOR=foss

cert-play: release-play ## Print signing cert info for the signed Play release APK
	$(MAKE) _cert FLAVOR=play

_cert:
	@test -x "$(APKSIGNER)" || { \
		echo "apksigner not found under $(ANDROID_HOME)/build-tools"; \
		exit 1; \
	}
	@"$(APKSIGNER)" verify --print-certs "$(LOCAL_RELEASE_APK)"

verify-foss: release-foss ## Verify the signed FOSS release APK
	$(MAKE) _verify FLAVOR=foss

verify-play: release-play ## Verify the signed Play release APK
	$(MAKE) _verify FLAVOR=play

_verify:
	@test -x "$(APKSIGNER)" || { \
		echo "apksigner not found under $(ANDROID_HOME)/build-tools"; \
		exit 1; \
	}
	@"$(APKSIGNER)" verify --verbose --print-certs "$(LOCAL_RELEASE_APK)"

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

run: install ## Install and launch FLAVOR (default: foss)
	$(ADB) shell am start -n "$(MAIN_ACTIVITY)"

launch: run ## Alias for run

run-foss: ## Install and launch the FOSS flavor
	$(MAKE) run FLAVOR=foss

run-fdroid: ## Legacy alias for the FOSS flavor
	$(MAKE) run FLAVOR=foss

run-play: ## Install and launch the Google Play flavor
	$(MAKE) run FLAVOR=play

logcat: ## Follow app logcat (Ctrl+C to stop)
	$(ADB) logcat --pid=$$($(ADB) shell pidof -s $(APP_ID) 2>/dev/null || echo 0) \
		|| $(ADB) logcat | grep -i orgl

clean: ## Clean Gradle build outputs
	$(GRADLEW) clean

deep-clean: clean ## Clean build outputs and local Gradle caches for this project
	rm -rf .gradle build app/build .gradle-home
