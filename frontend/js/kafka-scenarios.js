// ─── KAFKA ADVANCED SCENARIOS ────────────────────────────────────────────────
const kafkaKey = 'kafka';

async function testKafkaError() {
  addLog(kafkaKey, '--- 💀 SÁT THỦ: Gửi tin nhắn chứa mã lỗi để kích hoạt Dead Letter Topic ---', 'error');
  await registerSingle(kafkaKey, 'user_error_demo');
  closeControlModal();
}

async function testKafkaReplay() {
  addLog(kafkaKey, '--- ⏪ TRIGGER: Kích Hoạt Nút Phát Lại Dữ Liệu Hàng Loạt (Replay Time Machine) ---', 'start');
  closeControlModal();
  try {
    await fetch(`http://localhost:8082/users/replay`, { method: 'POST' });
  } catch (e) {
    addLog(kafkaKey, 'Exception API Tua Lại (Replay): ' + e.message, 'error');
  }
}

async function scaleKafkaWorkers(workers) {
  addLog(kafkaKey, `--- 🔧 TRIGGER BOOSTER: Bơm thêm Công nhân xử lý lên [ ${workers} ] ---`, 'start');
  try {
    await fetch(`http://localhost:8082/users/scale?workers=${workers}`, { method: 'POST' });
  } catch (e) {
    addLog(kafkaKey, 'Lỗi Scale: ' + e.message, 'error');
  }
}

async function clearKafkaQueue() {
  if (!confirm('Bạn có chắc muốn XÓA SẠCH mọi tin nhắn cũ trong Kafka?')) return;
  state.global.stopSpam = true;
  addLog(kafkaKey, '--- 🗑️ CORTEX: Đang ra lệnh Kafka Broker hủy diệt Topic và làm mới... ---', 'error');
  closeControlModal();

  try {
    await fetch(`http://localhost:8082/users/clear`, { method: 'POST' });
  } catch (e) {
    addLog(kafkaKey, 'Lỗi Xóa Queue: ' + e.message, 'error');
  }
}
