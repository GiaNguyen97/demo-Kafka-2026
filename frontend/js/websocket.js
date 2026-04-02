function initWebSocket(beKey) {
  const port = BACKENDS[beKey].port;
  if (state[beKey].socket) state[beKey].socket.close();

  try {
    state[beKey].socket = new WebSocket(`ws://localhost:${port}/ws`);

    state[beKey].socket.onopen = () => {
      state[beKey].wsConnected = true;
      updateWSStatus(beKey, true);
      addLog(beKey, `WS Đã kết nối → Theo dõi Cổng ${port}`, 'start');
    };

    state[beKey].socket.onmessage = (e) => {
      const msg = e.data;
      if (msg.includes('✅') || msg.toLowerCase().includes('success') || msg.includes('Sent to') || msg.includes('Done:')) {
        state[beKey].stats.processed++;
        updateStats(beKey);
        addLog(beKey, msg, 'success');
      } else if (msg.includes('⚠️') || msg.toLowerCase().includes('lỗi') || msg.toLowerCase().includes('fail')) {
        state[beKey].stats.failed++;
        updateStats(beKey);
        addLog(beKey, msg, 'error');
      } else {
        addLog(beKey, msg, 'info');
      }
    };

    state[beKey].socket.onclose = () => {
      state[beKey].wsConnected = false;
      updateWSStatus(beKey, false);
      addLog(beKey, 'Kết nối WS bị ngắt.', 'error');
    };

    state[beKey].socket.onerror = () => {
      state[beKey].wsConnected = false;
      updateWSStatus(beKey, false);
      addLog(beKey, `Lỗi kết nối WS – Backend cổng ${port} chưa được chạy!`, 'error');
    };
  } catch (e) {
    addLog(beKey, `WS Exception: ${e.message}`, 'error');
  }
}

function initAllWebSockets() {
  ['direct', 'async', 'kafka'].forEach(beKey => initWebSocket(beKey));
}
