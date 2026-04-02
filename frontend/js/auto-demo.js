/**
 * AUTO DEMO ENGINE
 * Tự động chạy một chuỗi các hành động để trình diễn sự khác biệt giữa 3 hệ thống.
 */

const sleep = ms => new Promise(res => setTimeout(res, ms));

async function startAutoDemo() {
  if (!confirm("🚀 Bắt đầu chế độ Auto Demo (30s)? Hệ thống sẽ tự động gửi request và chuyển đổi trạng thái.")) return;

  const btn = $('btn-auto-demo');
  btn.disabled = true;
  btn.innerHTML = "⏳ Đang chạy Demo...";
  
  try {
    // 0. Khởi đầu
    switchViewMode('basic');
    addLog('direct', "🏁 KHỞI ĐỘNG AUTO DEMO - Tự động so sánh 3 kiến trúc", 'submit');
    await sleep(2000);

    // 1. Demo Đồng bộ (Direct) - Cho thấy sự tắc nghẽn
    addLog('direct', "👉 BƯỚC 1: Thử thách hệ thống ĐỒNG BỘ với 20 requests", 'start');
    runSpamSingle('direct', 20, 'AUTO_SYNC');
    await sleep(5000); // Chờ xem nghẽn

    // 2. Demo Bất đồng bộ (Async) - Phản hồi nhanh nhưng rủi ro
    addLog('async', "👉 BƯỚC 2: Hệ thống BẤT ĐỒNG BỘ phản hồi ngay lập tức", 'start');
    runSpamSingle('async', 50, 'AUTO_ASYNC');
    await sleep(3000);
    
    addLog('async', "⚠️ GIẢ LẬP RỦI RO: Tạm dừng Worker Async...", 'error');
    // Call API pause async
    await fetch(`http://:8083/users/pause`, { method: 'POST' });
    runSpamSingle('async', 50, 'DATA_AT_RISK');
    await sleep(4000);
    addLog('async', "🛑 Hàng chờ RAM đang tăng. Nếu server crash lúc này, dữ liệu sẽ MẤT!", 'error');
    await sleep(3000);
    await fetch(`http://:8083/users/resume`, { method: 'POST' });

    // 3. Demo Kafka - Sự bền vững
    switchViewMode('advanced');
    addLog('kafka', "👉 BƯỚC 3: KAFKA - Sức mạnh của sự bền vững (Durable)", 'start');
    addLog('kafka', "⏸️ Tạm dừng Kafka Consumer và đẩy 200 requests...", 'warning');
    await fetch(`http://:8082/users/pause`, { method: 'POST' });
    runSpamSingle('kafka', 200, 'KAFKA_BUFFER');
    await sleep(5000);
    
    addLog('kafka', "🚀 Kích hoạt 10 Workers để xử lý thần tốc!", 'success');
    await scaleKafkaWorkers(10);
    await fetch(`http://:8082/users/resume`, { method: 'POST' });
    
    await sleep(5000);
    addLog('kafka', "✅ Kafka đã xử lý xong toàn bộ hàng chờ an toàn!", 'success');
    
    // Kết thúc
    addLog('direct', "🎉 KẾT THÚC DEMO - Kafka thắng tuyệt đối về độ bền và khả năng mở rộng!", 'submit');

  } catch (e) {
    console.error("Auto demo error", e);
  } finally {
    btn.disabled = false;
    btn.innerHTML = "🚀 Chạy Auto Demo";
    scaleKafkaWorkers(1); // Reset workers
  }
}
