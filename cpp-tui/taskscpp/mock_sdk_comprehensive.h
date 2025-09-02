// Comprehensive Mock Ditto SDK for CI build - NOT FOR PRODUCTION USE
#ifndef DITTO_COMPREHENSIVE_MOCK_H
#define DITTO_COMPREHENSIVE_MOCK_H

#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <set>
#include <iostream>
#include <initializer_list>

// Mock nlohmann::json with brace-enclosed initializer list support
namespace nlohmann {
  struct json {
    // Default constructor
    json() {}
    
    // Brace initializer constructor - this is key for {{"key", value}} syntax
    json(std::initializer_list<std::pair<std::string, json>> init) {}
    
    // Additional constructors for different value types to support brace initialization
    json(const std::string& s) {}
    json(bool b) {}
    json(int i) {}
    json(const char* s) {}
    
    // Copy constructor and assignment
    json(const json& other) {}
    json& operator=(const json& other) { return *this; }
    json& operator=(const std::string& s) { return *this; }
    json& operator=(bool b) { return *this; }
    json& operator=(int i) { return *this; }
    
    // Value access methods
    template<typename T> T get() const { return T{}; }
    template<typename T> T value(const std::string& key, const T& default_value) const { return default_value; }
    
    // Static methods
    static json parse(const std::string& s) { return json{}; }
    std::string dump(int indent = -1) const { return "{}"; }
    
    // Operators for JSON access
    struct mock_value {
      // Comparison operators
      bool operator==(const std::string& s) const { return true; }
      bool operator==(bool b) const { return true; }
      bool operator==(int i) const { return true; }
      
      // Assignment operators
      mock_value& operator=(const std::string& s) { return *this; }
      mock_value& operator=(bool b) { return *this; }
      mock_value& operator=(int i) { return *this; }
      
      // Conversion operators
      operator std::string() const { return "mock-string"; }
      operator bool() const { return true; }
      operator int() const { return 0; }
      
      // Method access
      template<typename T> T get() const { return T{}; }
      std::string dump() const { return "{}"; }
    };
    
    mock_value operator[](const std::string& key) const { return mock_value{}; }
    mock_value& operator[](const std::string& key) { static mock_value v; return v; }
    
    // Iterator support (basic)
    typedef mock_value* iterator;
    iterator begin() { static mock_value v; return &v; }
    iterator end() { static mock_value v; return &v; }
    
    // Size and empty
    size_t size() const { return 0; }
    bool empty() const { return true; }
  };
}

// Mock Ditto SDK with comprehensive API coverage
namespace ditto {
  // Log levels
  enum class LogLevel {
    Error = 0,
    Warning = 1,
    Info = 2,
    Debug = 3,
    Verbose = 4
  };
  
  // Mock logging with both single and double argument versions
  struct Log {
    // Single argument versions (for tasks_log.cpp)
    static void e(const std::string& message) {
      std::cerr << "[ERROR] " << message << std::endl;
    }
    static void w(const std::string& message) {
      std::cerr << "[WARN] " << message << std::endl;
    }
    static void i(const std::string& message) {
      std::cout << "[INFO] " << message << std::endl;
    }
    static void d(const std::string& message) {
      std::cout << "[DEBUG] " << message << std::endl;
    }
    static void v(const std::string& message) {
      std::cout << "[VERBOSE] " << message << std::endl;
    }
    
    // Two argument versions (tag + message)
    static void e(const std::string& tag, const std::string& message) {
      std::cerr << "[ERROR:" << tag << "] " << message << std::endl;
    }
    static void w(const std::string& tag, const std::string& message) {
      std::cerr << "[WARN:" << tag << "] " << message << std::endl;
    }
    static void i(const std::string& tag, const std::string& message) {
      std::cout << "[INFO:" << tag << "] " << message << std::endl;
    }
    static void d(const std::string& tag, const std::string& message) {
      std::cout << "[DEBUG:" << tag << "] " << message << std::endl;
    }
    static void v(const std::string& tag, const std::string& message) {
      std::cout << "[VERBOSE:" << tag << "] " << message << std::endl;
    }
    
    // Additional methods used by tasks_log.cpp
    static bool get_logging_enabled() { return true; }
    static void set_logging_enabled(bool enabled) {}
    static LogLevel get_minimum_log_level() { return LogLevel::Info; }
    static void set_minimum_log_level(LogLevel level) {}
    static void setMinimumLogLevel(LogLevel level) {}
    static void set_log_file(const std::string& path) {}
    static void disable_log_file() {}
    
    // Export methods (returns future-like object)
    struct MockFuture {
      void get() {}
    };
    static MockFuture export_to_file(const std::string& path) { return MockFuture{}; }
  };
  
  // Document and Query Results
  struct DocumentId {
    std::string to_string() const { return "mock-document-id"; }
    bool operator==(const DocumentId& other) const { return true; }
  };
  
  struct QueryResultItem {
    std::string json_string() const { return "{}"; }
    nlohmann::json value() const { return nlohmann::json{}; }
    DocumentId id() const { return DocumentId{}; }
  };
  
  struct QueryResult {
    size_t item_count() const { return 0; }
    QueryResultItem get_item(size_t index) const { return QueryResultItem{}; }
    std::vector<QueryResultItem> items() const { return {}; }
    std::vector<std::string> mutated_document_ids() const { return {}; }
    bool empty() const { return true; }
    
    // Iterator support
    typedef std::vector<QueryResultItem>::const_iterator iterator;
    iterator begin() const { static std::vector<QueryResultItem> empty; return empty.begin(); }
    iterator end() const { static std::vector<QueryResultItem> empty; return empty.end(); }
  };
  
  // Store Observer
  struct StoreObserver {
    virtual ~StoreObserver() {}
    virtual void on_next(const QueryResult& result) {}
    virtual void on_error(const std::exception& error) {}
    virtual void on_completed() {}
  };
  
  // Store and subscription
  struct SyncSubscription {
    void cancel() {}
  };
  
  struct Store {
    QueryResult execute(const std::string& query) { return QueryResult{}; }
    QueryResult execute(const std::string& query, const nlohmann::json& args) { return QueryResult{}; }
    
    std::shared_ptr<SyncSubscription> observe(const std::string& query, 
                                             std::shared_ptr<StoreObserver> observer) {
      return std::shared_ptr<SyncSubscription>(new SyncSubscription());
    }
    
    std::shared_ptr<SyncSubscription> observe(const std::string& query, 
                                             const nlohmann::json& args,
                                             std::shared_ptr<StoreObserver> observer) {
      return std::shared_ptr<SyncSubscription>(new SyncSubscription());
    }
    
    // Alternative method name used in some versions
    template<typename Callback>
    std::shared_ptr<SyncSubscription> register_observer(const std::string& query, Callback callback) {
      Log::i("Store", "Registered observer for query: " + query);
      return std::shared_ptr<SyncSubscription>(new SyncSubscription());
    }
  };
  
  // Transport and networking
  struct TransportConfig {
    struct Connect {
      std::set<std::string> websocket_urls;
      bool tcp_listening_enabled = true;
      int tcp_listening_port = 0;
    } connect;
    
    void enable_all_peer_to_peer() {}
    void disable_bluetooth() {}
    void disable_wifi() {}
  };
  
  // Identity management
  struct Identity {
    static std::string OnlinePlayground(const std::string& app_id, 
                                      const std::string& token, 
                                      bool enable_cloud_sync = true,
                                      const std::string& custom_auth_url = "") {
      return "mock-identity-" + app_id;
    }
    
    static std::string OfflinePlayground() {
      return "mock-offline-identity";
    }
  };
  
  // Main Ditto class with all required methods
  struct Ditto {
    bool sync_active = false;
    
    Ditto(const std::string& identity, const std::string& persistence_dir = "") {
      Log::i("Ditto", "Mock Ditto initialized with identity: " + identity);
    }
    
    void update_transport_config(std::function<void(TransportConfig&)> callback) {
      TransportConfig config;
      if (callback) callback(config);
    }
    
    void disable_sync_with_v3() {
      Log::i("Ditto", "Mock: disabled sync with v3");
    }
    
    void start_sync() {
      sync_active = true;
      Log::i("Ditto", "Mock: sync started");
    }
    
    void stop_sync() {
      sync_active = false;
      Log::i("Ditto", "Mock: sync stopped");
    }
    
    Store get_store() { 
      return Store{}; 
    }
    
    std::string site_id() const {
      return "mock-site-id-12345";
    }
    
    // Additional methods used by tasks_peer.cpp
    bool get_is_sync_active() const {
      return sync_active;
    }
    
    // Mock sync() method for subscription management
    struct MockSync {
      std::shared_ptr<SyncSubscription> register_subscription(const std::string& query) {
        Log::i("MockSync", "Registered subscription: " + query);
        return std::shared_ptr<SyncSubscription>(new SyncSubscription());
      }
    };
    
    MockSync sync() {
      return MockSync{};
    }
    
    // Additional methods used by tasks_peer.cpp
    static std::string get_sdk_version() {
      return "mock-sdk-1.0.0";
    }
  };
  
  // Additional utility types
  struct WriteTransaction {
    QueryResult execute(const std::string& query) { return QueryResult{}; }
    QueryResult execute(const std::string& query, const nlohmann::json& args) { return QueryResult{}; }
  };
  
  struct ReadTransaction {
    QueryResult execute(const std::string& query) { return QueryResult{}; }
    QueryResult execute(const std::string& query, const nlohmann::json& args) { return QueryResult{}; }
  };
}
#endif