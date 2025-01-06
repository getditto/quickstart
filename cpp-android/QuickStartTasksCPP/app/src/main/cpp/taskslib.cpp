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

std::atomic<JavaVM *> java_vm{nullptr}; // set in JNI_OnLoad()

// This module maintains a singleton C++ TasksPeer instance which performs all the Ditto-related
// functions, and all methods operate on that singleton.  The mutex must be locked by any thread
// that is accessing the peer.
std::recursive_mutex mtx;
std::shared_ptr<TasksPeer> peer;
jobject javaTasksObserver;
std::shared_ptr<ditto::StoreObserver> tasksStoreObserver;

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

// JNI_OnLoad is called when the native library is loaded
extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  __android_log_print(ANDROID_LOG_INFO, TAG, "JNI_OnLoad called");
  java_vm.store(vm);
  return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_initDitto(JNIEnv *env, jobject thiz, jobject context,
                                                    jstring app_id,
                                                    jstring token,
                                                    jstring persistence_dir) {
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (peer) {
      throw_java_illegal_state_exception(env, "cannot call initDitto multiple times");
      return;
    }
    auto vm = java_vm.load();
    if (vm == nullptr) {
      throw_java_illegal_state_exception(env, "Java VM has not been initialized");
      return;
    }
    auto thread_env = get_JNIEnv_attached_to_current_thread(vm);
    auto app_id_str = jstring_to_string(env, app_id);
    auto token_str = jstring_to_string(env, token);
    auto persistence_dir_str = jstring_to_string(env, persistence_dir);
    peer = std::make_shared<TasksPeer>(thread_env, context, std::move(app_id_str),
                                       std::move(token_str),
                                       true, std::move(persistence_dir_str));
  } catch (const std::exception &err) {
    log_error(std::string("initDitto failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_terminateDitto(JNIEnv *env, jobject thiz) {
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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
    std::lock_guard<std::recursive_mutex> lock(mtx);
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

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_setTasksObserver(JNIEnv *env, jobject thiz,
                                                           jobject observer) {
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
    javaTasksObserver = env->NewGlobalRef(observer);
    JavaVM *vm;
    env->GetJavaVM(&vm);
    tasksStoreObserver = peer->register_tasks_observer(
        [vm](const std::vector<std::string> &tasksJson) {
          try {
            std::lock_guard<std::recursive_mutex> lock(mtx);
            if (javaTasksObserver == nullptr) {
              return;
            }

            TempAttachedThread attached(vm);
            JNIEnv *env = attached.env();

            // Convert C++ tasksJson to a Java String array
            const auto tasksCount = (int) tasksJson.size();
            TempLocalRef<jclass> stringClass(env, env->FindClass("java/lang/String"));
            TempLocalRef<jobjectArray> stringArray(env,
                                                   env->NewObjectArray(tasksCount,
                                                                       stringClass.get(),
                                                                       nullptr));
            for (auto i = 0; i < tasksCount; ++i) {
              TempJString js(env, tasksJson[i]);
              env->SetObjectArrayElement(stringArray.get(), i, js.get());
            }

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
            log_error(std::string("error processing tasks update: ") + err.what());
          }
        });
  } catch (const std::exception &err) {
    log_error(std::string("addTasksObserver failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_live_ditto_quickstart_tasks_TasksLib_removeTasksObserver(JNIEnv *env, jobject thiz) {
  try {
    std::lock_guard<std::recursive_mutex> lock(mtx);
    if (javaTasksObserver != nullptr) {
      env->DeleteGlobalRef(javaTasksObserver);
      javaTasksObserver = nullptr;
    }
    tasksStoreObserver.reset();
  } catch (const std::exception &err) {
    log_error(std::string("removeTasksObserver failed: ") + err.what());
    throw_java_exception(env, err.what());
  }
}
