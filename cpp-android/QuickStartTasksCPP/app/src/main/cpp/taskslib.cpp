#include <jni.h>

#include "jni_util.h"
#include "task.h"
#include "tasks_log.h"
#include "tasks_peer.h"

#include <string>
#include <memory>
#include <mutex>

namespace {

// This module maintains a singleton C++ TasksPeer instance which performs all the Ditto-related
// functions, and all methods operate on that singleton.  The mutex must be locked by any thread
// that is accessing the peer.
std::mutex mtx;
std::shared_ptr<TasksPeer> peer;

// Create a live.ditto.quickstart.tasks.data.Task object from a C++ Task object.
jobject native_task_to_java_task(JNIEnv *env, const Task &native_task) {
  jclass taskClass = env->FindClass("live/ditto/quickstart/tasks/data/Task");
  if (taskClass == nullptr) {
    return nullptr;
  }

  jmethodID constructor = env->GetMethodID(taskClass, "<init>",
                                           "(Ljava/lang/String;Ljava/lang/String;ZZ)V");
  if (constructor == nullptr) {
    return nullptr; // Constructor not found
  }

  jstring id = env->NewStringUTF(native_task._id.c_str());
  jstring title = env->NewStringUTF(native_task.title.c_str());
  jboolean done = bool_to_jboolean(native_task.done);
  jboolean deleted = bool_to_jboolean(native_task.deleted);

  auto java_task = env->NewObject(taskClass, constructor, id, title, done, deleted);

  env->DeleteLocalRef(id);
  env->DeleteLocalRef(title);

  return java_task;
}

} // end anonymous namespace

// These native C++ functions are called by the Kotlin live.ditto.quickstart.tasks.TasksLib object

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_initDitto(JNIEnv *env, jobject thiz, jstring app_id,
                                                    jstring token) {
  try {
    std::lock_guard<std::mutex> lock(mtx);
    if (peer) {
      throw_java_illegal_state_exception(env, "cannot call initDitto multiple times");
      return;
    }
    auto app_id_str = jstring_to_string(env, app_id);
    auto token_str = jstring_to_string(env, token);
    peer = std::make_shared<TasksPeer>(std::move(app_id_str), std::move(token_str), true, "");
  } catch (const std::exception &err) {
    log_error(std::string("initDitto failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_terminateDitto(JNIEnv *env, jobject thiz) {
  try {
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    // TODO: perform any necessary cleanup before the TasksPeer is destroyed
    peer.reset();
  } catch (const std::exception &err) {
    log_error(std::string("terminateDitto failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_isSyncActive(JNIEnv *env, jobject thiz) {
  try {
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return JNI_FALSE;
    }
    return peer->is_sync_active();
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
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    peer->start_sync();
  } catch (const std::exception &err) {
    log_error(std::string("startSync failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_stopSync(JNIEnv *env, jobject thiz) {
  try {
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    peer->stop_sync();
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
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return nullptr;
    }
    const auto task_id_str = jstring_to_string(env, task_id);
    const auto task = peer->get_task(task_id_str);
    return native_task_to_java_task(env, task);
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
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    auto title_str = jstring_to_string(env, title);
    peer->add_task(title_str, done);
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
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    Task task(jstring_to_string(env, task_id), jstring_to_string(env, title), done);
    peer->update_task(task);
  } catch (const std::exception &err) {
    log_error(std::string("updateTask failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_deleteTask(JNIEnv *env, jobject thiz, jstring task_id) {
  try {
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    peer->delete_task(jstring_to_string(env, task_id));
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
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    const auto task_id_str = jstring_to_string(env, task_id);
    const auto task = peer->get_task(task_id_str);
    peer->mark_task_complete(task_id_str, !task.done);
  } catch (const std::exception &err) {
    log_error(std::string("toggleDoneState failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_insertInitialDocuments(JNIEnv *env, jobject thiz) {
  try {
    std::lock_guard<std::mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    peer->insert_initial_tasks();
  } catch (const std::exception &err) {
    log_error(std::string("insertInitialDocument failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}
