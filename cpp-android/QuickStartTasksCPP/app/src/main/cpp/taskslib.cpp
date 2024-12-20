#include <jni.h>

#include "jni_util.h"
#include "task.h"
#include "tasks_log.h"
#include "tasks_peer.h"

// These native C++ functions are called by the Kotlin live.ditto.quickstart.tasks.TasksLib object

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_initDitto(JNIEnv *env, jobject thiz, jstring app_id,
                                                    jstring token) {
  try {
    const auto app_id_str = jstring_to_string(env, app_id);
    const auto token_str = jstring_to_string(env, token);

    // TODO: implement initDitto()
    throw_unsupported_operation_exception(env, "initDitto is not implemented");
  } catch (const std::exception &err) {
    log_error(std::string("initDitto failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_isSyncActive(JNIEnv *env, jobject thiz) {
  try {
    // TODO: implement isSyncActive()
    throw_unsupported_operation_exception(env, "isSyncActive is not implemented");
    return JNI_FALSE;
  } catch (const std::exception &err) {
    log_error(std::string("isSyncActive failed: ") + err.what());
    throw_java_exception(env, err.what());
    return JNI_FALSE;
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_startSync(JNIEnv *env, jobject thiz) {
  try {
    // TODO: implement startSync()
    throw_unsupported_operation_exception(env, "startSync is not implemented");
  } catch (const std::exception &err) {
    log_error(std::string("startSync failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_stopSync(JNIEnv *env, jobject thiz) {
  try {
    // TODO: implement stopSync()
    throw_unsupported_operation_exception(env, "stopSync is not implemented");
  } catch (const std::exception &err) {
    log_error(std::string("stopSync failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_getTaskWithId(JNIEnv *env, jobject thiz,
                                                        jstring task_id) {
  try {
    const auto task_id_str = jstring_to_string(env, task_id);

    // TODO: implement getTaskWithId()
    throw_unsupported_operation_exception(env, "getTaskWithId is not implemented");
    return nullptr;
  } catch (const std::exception &err) {
    log_error(std::string("getTaskWithId failed: ") + err.what());
    throw_java_exception(env, err.what());
    return nullptr;
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_createTask(JNIEnv *env, jobject thiz, jstring title,
                                                     jboolean done) {
  try {
    // TODO: implement createTask()
    throw_unsupported_operation_exception(env, "createTask is not implemented");
  } catch (const std::exception &err) {
    log_error(std::string("createTask failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_updateTask(JNIEnv *env, jobject thiz, jstring task_id,
                                                     jstring title, jboolean done) {
  try {
    // TODO: implement updateTask()
    throw_unsupported_operation_exception(env, "updateTask is not implemented");
  } catch (const std::exception &err) {
    log_error(std::string("updateTask failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_deleteTask(JNIEnv *env, jobject thiz, jstring task_id) {
  try {
    // TODO: implement deleteTask()
    throw_unsupported_operation_exception(env, "deleteTask is not implemented");
  } catch (const std::exception &err) {
    log_error(std::string("deleteTask failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_toggleDoneState(JNIEnv *env, jobject thiz,
                                                          jstring task_id) {
  try {
    // TODO: implement toggleDoneState()
    throw_unsupported_operation_exception(env, "toggleDoneState is not implemented");
  } catch (const std::exception &err) {
    log_error(std::string("toggleDoneState failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_insertInitialDocument(JNIEnv *env, jobject thiz,
                                                                jstring task_id, jstring title,
                                                                jboolean done, jboolean deleted) {
  try {
    // TODO: implement insertInitialDocument()
    throw_unsupported_operation_exception(env, "insertInitialDocument is not implemented");
  } catch (const std::exception &err) {
    log_error(std::string("insertInitialDocument failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}