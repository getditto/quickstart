#ifndef QUICKSTARTTASKSCPP_JNI_UTIL_H
#define QUICKSTARTTASKSCPP_JNI_UTIL_H

#include <jni.h>

#include <string>
#include <stdexcept>
#include <vector>

/// Convert a Java String to a C++ std::string
std::string jstring_to_string(JNIEnv *env, jstring js);

/// Convert a C++ bool to JNI boolean
inline jboolean bool_to_jboolean(bool b) { return b ? JNI_TRUE : JNI_FALSE; }

/// Throw a Java exception from a JNI function.
void throw_java_exception(JNIEnv *env, const char *msg,
                          const char *exception_class_name = "java/lang/Exception");

/// Throw a java.lang.UnsupportedOperationException
void throw_java_unsupported_operation_exception(JNIEnv *env, const char *msg);

/// Throw a java.lang.IllegalStateException
void throw_java_illegal_state_exception(JNIEnv *env, const char *msg);

/// Throw a java.lang.IllegalArgumentException
void throw_java_illegal_argument_exception(JNIEnv *env, const char *msg);

/// Convert a collection of C++ strings to an array of Java Strings.
///
/// The caller is responsible for eventually calling env->DeleteLocalRef() on the returned array.
jobjectArray strings_to_jstrings(JNIEnv *env, const std::vector<std::string> &strings);

/// Takes ownership of a local reference to a Java object, and releases it upon destruction
template<class T>
class TempLocalRef {
public:
  /// Take ownership of the specified local reference.
  ///
  /// The env must be the same object that created the local reference, and must remain valid until
  /// this object's destructor is called.
  TempLocalRef(JNIEnv *env, T localRef) noexcept: env(env), localRef(localRef) {}

  ~TempLocalRef() noexcept {
    if (env != nullptr && localRef != nullptr) {
      env->DeleteLocalRef(localRef);
    }
  }

  /// Get the owned local reference.
  T get() const noexcept { return localRef; }

  TempLocalRef(const TempLocalRef<T> &) = delete;

  TempLocalRef(TempLocalRef<T> &&) = delete;

  TempLocalRef &operator=(const TempLocalRef<T> &) = delete;

  TempLocalRef &operator=(TempLocalRef<T> &&) = delete;

private:
  JNIEnv *env;
  T localRef;
};

/// Converts a C++ string to an owned `jstring`, which will be freed upon destruction.
class TempJString {
public:
  /// Convert the given null-terminated UTF8 string to a `jstring`.
  ///
  /// The `env` pointer must remain valid until the destructor has been called.
  TempJString(JNIEnv *env, const char *c_str) : js(env, env->NewStringUTF(c_str)) {
    if (js.get() == nullptr) {
      throw std::runtime_error("NewStringUTF failed");
    }
  }

  /// Convert the given `std::string` to a `jstring`.
  ///
  /// The `env` pointer must remain valid until the destructor has been called.
  TempJString(JNIEnv *env, const std::string &s) : TempJString(env, s.c_str()) {}

  /// Return converted `jstring`
  ///
  /// The returned object is only valid until this object is destroyed.
  jstring get() const noexcept {
    return js.get();
  }

  TempJString(const TempJString &) = delete;

  TempJString(TempJString &&) = delete;

  TempJString &operator=(const TempJString &) = delete;

  TempJString &operator=(TempJString &&) = delete;

private:
  TempLocalRef<jstring> js;
};

/// Attaches the current thread to Java VM, then detaches on destruction.
class TempAttachedThread {
public:
  /// Attach the current thread to the specified JavaVM.
  explicit TempAttachedThread(JavaVM *vm) : vm(vm), thread_env(nullptr) {
    if (vm->AttachCurrentThread(&thread_env, nullptr) != 0) {
      throw std::runtime_error("AttachCurrentThread failed");
    }
  }

  /// Return JNI environment for the attached thread
  JNIEnv *env() const noexcept {
    return thread_env;
  }

  ~TempAttachedThread() noexcept {
    if (thread_env != nullptr) {
      vm->DetachCurrentThread();
    }
  }

  TempAttachedThread(const TempAttachedThread &) = delete;

  TempAttachedThread(TempAttachedThread &&) = delete;

  TempAttachedThread &operator=(const TempAttachedThread &) = delete;

  TempAttachedThread &operator=(TempAttachedThread &&) = delete;

private:
  JavaVM *vm;
  JNIEnv *thread_env;
};

#endif //QUICKSTARTTASKSCPP_JNI_UTIL_H
