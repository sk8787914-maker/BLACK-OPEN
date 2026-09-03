#include "SystemPropertiesHook.h"
#include "IO.h"
#include "BoxCore.h"
#include "JniHook/JniHook.h"
#include "Log.h"

// 🛡️ Add this include to get PROP_VALUE_MAX
#include <sys/system_properties.h>

#include <map>
#include <string>
#include <cstring>

static std::map<std::string, std::string> prop_map;

// ============================================================
// HOOK: SystemProperties.native_get (Java layer)
// ============================================================
HOOK_JNI(jstring, native_get, JNIEnv *env, jobject obj, jstring key, jstring def) {
    // Handle null environment (should never happen)
    if (env == nullptr) {
        return nullptr;
    }

    // If key is null, return default immediately (do NOT call original)
    if (key == nullptr) {
        return def;
    }

    const char *key_str = env->GetStringUTFChars(key, nullptr);
    if (key_str == nullptr) {
        return def;
    }

    const char *def_str = nullptr;
    if (def != nullptr) {
        def_str = env->GetStringUTFChars(def, nullptr);
    }

    auto ret = prop_map.find(key_str);
    if (ret != prop_map.end()) {
        const char *value = ret->second.c_str();
        env->ReleaseStringUTFChars(key, key_str);
        if (def_str != nullptr) {
            env->ReleaseStringUTFChars(def, def_str);
        }
        ALOGD("SystemProperties.native_get: %s => %s", key_str, value);
        return env->NewStringUTF(value);
    }

    env->ReleaseStringUTFChars(key, key_str);
    if (def_str != nullptr) {
        env->ReleaseStringUTFChars(def, def_str);
    }
    return orig_native_get(env, obj, key, def);
}

// ============================================================
// HOOK: __system_property_get (Native layer - libc)
// ============================================================
HOOK_JNI(int, __system_property_get, const char *name, char *value) {
    if (name == nullptr || value == nullptr) {
        return orig___system_property_get(name, value);
    }

    auto ret = prop_map.find(name);
    if (ret != prop_map.end()) {
        const char *fake_value = ret->second.c_str();
        strncpy(value, fake_value, PROP_VALUE_MAX - 1);
        value[PROP_VALUE_MAX - 1] = '\0';
        ALOGD("__system_property_get: %s => %s", name, fake_value);
        return (int) strlen(value);
    }

    return orig___system_property_get(name, value);
}

// ============================================================
// INIT: Setup fake device properties
// ============================================================
void SystemPropertiesHook::init(JNIEnv *env) {
    // =========================================================
    // 🔥 Device spoofed as Google Pixel 4a (sunfish), Android 11
    // =========================================================

    // ---- Device Info ----
    prop_map["ro.product.board"]         = "sunfish";
    prop_map["ro.product.brand"]         = "Google";
    prop_map["ro.product.device"]        = "sunfish";
    prop_map["ro.product.manufacturer"]  = "Google";
    prop_map["ro.product.model"]         = "Pixel 4a";
    prop_map["ro.product.name"]          = "sunfish";

    // ---- Build Info ----
    prop_map["ro.build.id"]              = "RP1A.201005.001";
    prop_map["ro.build.display.id"]      = "RP1A.201005.001 release-keys";
    prop_map["ro.build.host"]            = "abfarm-ubuntu-2004";
    prop_map["ro.build.tags"]            = "release-keys";
    prop_map["ro.build.type"]            = "user";
    prop_map["ro.build.user"]            = "android-build";

    // ---- Additional (X/Twitter specific) ----
    prop_map["ro.build.version.sdk"]     = "30";                // Android 11
    prop_map["ro.build.version.release"] = "11";
    prop_map["ro.build.version.codename"]= "REL";
    prop_map["ro.build.fingerprint"]     = "google/sunfish/sunfish:11/RP1A.201005.001/12345678:user/release-keys";

    // ---- Network / Carrier ----
    prop_map["gsm.operator.alpha"]       = "Airtel";
    prop_map["gsm.operator.numeric"]     = "40410";
    prop_map["gsm.sim.operator.alpha"]   = "Airtel";
    prop_map["gsm.sim.operator.numeric"] = "40410";

    // ---- Security (important for X) ----
    prop_map["ro.boot.verifiedbootstate"]    = "green";
    prop_map["ro.boot.flash.locked"]         = "1";
    prop_map["ro.boot.vbmeta.device_state"]  = "locked";

    ALOGD("SystemPropertiesHook initialized with %zu fake props", prop_map.size());

    // Hook Java method
    JniHook::HookJniFun(
        env,
        "android/os/SystemProperties",
        "native_get",
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
        (void *) new_native_get,
        (void **) (&orig_native_get),
        true
    );

    // Hook native method (libc) - Uncomment if shadowhook is linked
    // shadowhook_hook_sym_name("libc.so", "__system_property_get", (void *) new___system_property_get, (void **) (&orig___system_property_get));
}
