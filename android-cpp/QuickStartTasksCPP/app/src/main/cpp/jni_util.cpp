#include "jni_util.h"

/// Convert a Java String to a C++ std::string
std::string jstring_to_string(JNIEnv *env, jstring js) {
  if (js == nullptr) {
    throw std::invalid_argument("null jstring passed");
  }
  const char *utf = env->GetStringUTFChars(js, nullptr);
  if (utf == nullptr) {
    throw std::runtime_error("Failed to convert jstring to UTF-8");
  }

  try {
    auto result = std::string(utf);
    env->ReleaseStringUTFChars(js, utf);
    return result;
  } catch (...) {
    env->ReleaseStringUTFChars(js, utf);
    throw;
  }
}

void throw_java_exception(JNIEnv *env, const char *msg,
                          const char *exception_class_name) {
  jclass exception_class = (*env).FindClass(exception_class_name);
  (*env).ThrowNew(exception_class, msg);
}

void throw_java_illegal_state_exception(JNIEnv *env, const char *msg) {
  throw_java_exception(env, msg, "java/lang/IllegalStateException");
}

void throw_java_illegal_argument_exception(JNIEnv *env, const char *msg) {
  throw_java_exception(env, msg, "java/lang/IllegalArgumentException");
}

jobjectArray strings_to_jstrings(JNIEnv *env,
                                 const std::vector<std::string> &strings) {
  const auto count = (int)strings.size();
  TempLocalRef<jclass> stringClass(env, env->FindClass("java/lang/String"));
  jobjectArray stringArray =
      env->NewObjectArray(count, stringClass.get(), nullptr);
  for (auto i = 0; i < count; ++i) {
    TempJString js(env, strings[i]);
    env->SetObjectArrayElement(stringArray, i, js.get());
  }

  return stringArray;
}
