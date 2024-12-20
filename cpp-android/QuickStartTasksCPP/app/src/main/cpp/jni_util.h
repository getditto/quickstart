#ifndef QUICKSTARTTASKSCPP_JNI_UTIL_H
#define QUICKSTARTTASKSCPP_JNI_UTIL_H

#include <jni.h>

#include <string>

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

#endif //QUICKSTARTTASKSCPP_JNI_UTIL_H
