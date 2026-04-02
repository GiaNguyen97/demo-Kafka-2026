const BACKENDS = {
  direct: { port: '8081', label: '⚡ Đồng Bộ', color: 'blue' },
  async: { port: '8083', label: '🧵 Bất Đồng Bộ', color: 'yellow' },
  kafka: { port: '8082', label: '🚀 Kafka', color: 'green' }
};

const state = {
  direct: {
    stats: { received: 0, processed: 0, failed: 0, isPaused: false },
    responseTimes: [],
    socket: null,
    wsConnected: false
  },
  async: {
    stats: { received: 0, processed: 0, failed: 0, isPaused: false },
    responseTimes: [],
    socket: null,
    wsConnected: false
  },
  kafka: {
    stats: { received: 0, processed: 0, failed: 0, kafkaLag: 0, isPaused: false },
    responseTimes: [],
    socket: null,
    wsConnected: false
  },
  global: {
    stopSpam: false,
    viewMode: 'basic', // 'basic' or 'advanced'
    logFilters: {
      direct: 'all',
      async: 'all',
      kafka: 'all'
    }
  }
};

const $ = id => document.getElementById(id);
