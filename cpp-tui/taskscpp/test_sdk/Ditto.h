// Mock Ditto.h for testing - MORE COMPLETE VERSION
#ifndef DITTO_MOCK_H
#define DITTO_MOCK_H

#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <set>

namespace nlohmann {
  struct json {
    template<typename T> T get() const { return T{}; }
    static json parse(const std::string&) { return json{}; }
    std::string dump() const { return "{}"; }
    
    // Mock operator[] for JSON access
    struct mock_value {
      bool operator==(const std::string&) const { return true; }
      bool operator==(bool) const { return true; }
    };
    mock_value operator[](const std::string&) const { return mock_value{}; }
  };
}

namespace ditto {
  struct QueryResultItem {
    std::string json_string() const { return "{}"; }
  };
  
  struct QueryResult {
    size_t item_count() const { return 0; }
    QueryResultItem get_item(size_t) const { return QueryResultItem{}; }
    std::vector<QueryResultItem> items() const { return {}; }
    std::vector<std::string> mutated_document_ids() const { return {}; }
  };
  
  struct DocumentId {
    std::string to_string() const { return "mock-id"; }
  };
  
  struct TransportConfig {
    struct { std::set<std::string> websocket_urls; } connect;
    void enable_all_peer_to_peer() {}
  };
  
  struct Store {
    QueryResult execute(const std::string&) { return QueryResult{}; }
  };
  
  struct SyncSubscription {};
  
  struct Ditto {
    Ditto(const std::string&, const std::string&) {}
    void update_transport_config(std::function<void(TransportConfig&)>) {}
    void disable_sync_with_v3() {}
    Store get_store() { return Store{}; }
  };
  
  struct Identity {
    static std::string OnlinePlayground(std::string, std::string, bool, std::string) {
      return "mock-identity";
    }
  };
}
#endif
