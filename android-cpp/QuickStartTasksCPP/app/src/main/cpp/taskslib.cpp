// JNI Interface to C++ code
//
// These native C++ functions are called from Kotlin code through the Kotlin
// live.ditto.quickstart.tasks.TasksLib object.

#include <android/log.h>
#include <jni.h>

#include "jni_util.h"
#include "task.h"
#include "tasks_log.h"
#include "tasks_peer.h"

#include <atomic>
#include <string>
#include <memory>
#include <mutex>

namespace {

constexpr const char *TAG = "taskslib";

// This module maintains a singleton C++ TasksPeer instance which performs all the Ditto-related
// functions, and all methods operate on that singleton.  The mutex must be locked by any thread
// that is accessing the peer.
std::recursive_mutex mtx;
std::shared_ptr<TasksPeer> peer;
jobject javaTasksObserver;
std::shared_ptr<ditto::StoreObserver> tasksStoreObserver;

void remove_observer(JNIEnv *env) {
  std::lock_guard<std::recursive_mutex> lock(mtx);
  tasksStoreObserver.reset();
  if (javaTasksObserver != nullptr) {
    env->DeleteGlobalRef(javaTasksObserver);
    javaTasksObserver = nullptr;
  }
}

// Create a live.ditto.quickstart.tasks.data.Task object from a C++ Task object.
jobject native_task_to_java_task(JNIEnv *const env, const Task &native_task) {
  jclass taskClass = env->FindClass("live/ditto/quickstart/tasks/data/Task");
  if (taskClass == nullptr) {
    throw std::runtime_error("Java Task class not found");
  }

  jmethodID ctor = env->GetMethodID(taskClass, "<init>",
                                    "(Ljava/lang/String;Ljava/lang/String;ZZ)V");
  if (ctor == nullptr) {
    throw std::runtime_error("Java Task constructor not found");
  }

  TempJString id(env, native_task._id);
  TempJString title(env, native_task.title);
  jboolean done = bool_to_jboolean(native_task.done);
  jboolean deleted = bool_to_jboolean(native_task.deleted);

  auto java_task = env->NewObject(taskClass, ctor, id.get(), title.get(), done, deleted);
  return java_task;
}

} // end anonymous namespace

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_initDitto(JNIEnv *env, jobject thiz, jobject context,
                                                    jstring app_id,
                                                    jstring token,
                                                    jstring persistence_dir,
                                                    jboolean is_running_on_emulator) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_initDitto; SDK version: %s; emulator: %s",
                      TasksPeer::get_ditto_sdk_version().c_str(),
                      is_running_on_emulator ? "true" : "false");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (peer) {
      throw_java_illegal_state_exception(env, "cannot call initDitto multiple times");
      return;
    }
    auto app_id_str = jstring_to_string(env, app_id);
    auto token_str = jstring_to_string(env, token);
    auto persistence_dir_str = jstring_to_string(env, persistence_dir);
    peer = std::make_shared<TasksPeer>(env, context, std::move(app_id_str),
                                       std::move(token_str),
                                       true, std::move(persistence_dir_str),
                                       is_running_on_emulator);
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "initDitto failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_terminateDitto(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_terminateDitto");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    remove_observer(env);
    peer.reset();
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "terminateDitto failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_isSyncActive(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_isSyncActive");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return JNI_FALSE;
    }
    return peer->is_sync_active();
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "isSyncActive failed: %s", err.what());
    throw_java_exception(env, err.what());
    return JNI_FALSE;
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_startSync(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_startSync");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    peer->start_sync();
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "startSync failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_stopSync(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG, "Java_live_ditto_quickstart_tasks_TasksLib_stopSync");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    remove_observer(env);
    peer->stop_sync();
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "stopSync failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_getTaskWithId(JNIEnv *env, jobject thiz,
                                                        jstring task_id) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_getTaskWithId");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return nullptr;
    }
    const auto task_id_str = jstring_to_string(env, task_id);
    const auto task = peer->get_task(task_id_str);
    return native_task_to_java_task(env, task);
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "getTaskWithId failed: %s", err.what());
    throw_java_exception(env, err.what());
    return nullptr;
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_createTask(JNIEnv *env, jobject thiz, jstring title,
                                                     jboolean done) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_createTask");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    auto title_str = jstring_to_string(env, title);
    peer->add_task(title_str, done);
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "createTask failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_updateTask(JNIEnv *env, jobject thiz, jstring task_id,
                                                     jstring title, jboolean done) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_updateTask");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    Task task(jstring_to_string(env, task_id), jstring_to_string(env, title), done);
    peer->update_task(task);
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "updateTask failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_deleteTask(JNIEnv *env, jobject thiz, jstring task_id) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_deleteTask");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    peer->delete_task(jstring_to_string(env, task_id));
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "deleteTask failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_toggleDoneState(JNIEnv *env, jobject thiz,
                                                          jstring task_id) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_toggleDoneState");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    const auto task_id_str = jstring_to_string(env, task_id);
    const auto task = peer->get_task(task_id_str);
    peer->mark_task_complete(task_id_str, !task.done);
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "toggleDoneState failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_insertInitialDocuments(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_insertInitialDocuments");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    peer->insert_initial_tasks();
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "insertInitialDocuments failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_setTasksObserver(JNIEnv *env, jobject thiz,
                                                           jobject observer) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_setTasksObserver");
  try {
    if (observer == nullptr) {
      throw_java_illegal_argument_exception(env, "observer cannot be null");
      return;
    }

    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (!peer) {
      throw_java_illegal_state_exception(env, "TasksLib has not been initialized");
      return;
    }
    if (javaTasksObserver != nullptr || tasksStoreObserver != nullptr) {
      throw_java_illegal_state_exception(env, "a tasks observer is already set");
      return;
    }

    JavaVM *vm;
    if (env->GetJavaVM(&vm) != 0 || vm == nullptr) {
      throw_java_exception(env, "unable to access Java VM");
      return;
    }

    javaTasksObserver = env->NewGlobalRef(observer);
    tasksStoreObserver = peer->register_tasks_observer(
        [vm](const std::vector<std::string> &tasksJson) {
          try {
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "tasks observer callback invoked");
            std::lock_guard<std::recursive_mutex> lock(mtx);
            if (javaTasksObserver == nullptr) {
              return;
            }

            TempAttachedThread attached(vm);
            JNIEnv *env = attached.env();

            // Convert C++ tasksJson string collection to a Java String array
            TempLocalRef<jobjectArray> stringArray(env, strings_to_jstrings(env, tasksJson));

            // Invoke the onTasksUpdated method of the Java observer
            TempLocalRef<jclass> observerClass(env,
                                               env->GetObjectClass(javaTasksObserver));
            jmethodID methodID = env->GetMethodID(observerClass.get(), "onTasksUpdated",
                                                  "([Ljava/lang/String;)V");
            if (methodID == nullptr) {
              throw std::runtime_error("unable to get method ID for onTasksUpdated of observer");
            }
            env->CallVoidMethod(javaTasksObserver, methodID, stringArray.get());
          } catch (const std::exception &err) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "error processing tasks update: %s",
                                err.what());
          }
        });
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "setTasksObserver failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_removeTasksObserver(JNIEnv *env, jobject thiz) {
  __android_log_print(ANDROID_LOG_DEBUG, TAG,
                      "Java_live_ditto_quickstart_tasks_TasksLib_removeTasksObserver");
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    remove_observer(env);
  } catch (const std::exception &err) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "removeTasksObserver failed: %s", err.what());
    throw_java_exception(env, err.what());
  }
}
