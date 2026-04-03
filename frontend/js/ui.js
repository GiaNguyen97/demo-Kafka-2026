function updateStats(beKey) {
  const s = state[beKey].stats;
  $(`rec-${beKey}`).textContent = s.received;
  $(`pro-${beKey}`).textContent = s.processed;
  $(`err-${beKey}`).textContent = s.failed;
  
  // Tính toán hàng chờ (Backlog)
  let queue = Math.max(0, s.received - s.processed - s.failed);
  
  // Cập nhật trạng thái Ping/Lag cho Direct/Async để thấy sự nghẽn
  if (beKey !== 'kafka' && queue > 0) {
    $(`ping-${beKey}`).innerHTML = `<span class="text-warning">⚡ Đang đợi: ${queue}</span>`;
  }

  // Cập nhật Kafka Lag chuyên sâu
  if (beKey === 'kafka') {
    const lagData = s.kafkaLag || { total: 0, partitions: {} };
    const totalLag = lagData.total || 0;
    const partitions = lagData.partitions || {};
    
    const lagEl = $('lag-count');
    const lagProg = $('prog-lag');
    
    if (lagEl) {
      lagEl.textContent = totalLag.toLocaleString();
      lagEl.className = totalLag > 500 ? 'text-red font-bold' : (totalLag > 50 ? 'text-yellow' : 'text-green');
    }
    
    if (lagProg) {
      const lagPercent = Math.min(100, Math.floor((totalLag / 1000) * 100));
      lagProg.style.width = lagPercent + '%';
    }

    // Hiển thị phân bổ Partitions
    const partitionGrid = document.querySelector('.partition-grid');
    if (partitionGrid) {
       // Chỉ hiển thị top 4 partitions có lag để đỡ rối
       let partHtml = '';
       const activeParts = Object.entries(partitions).filter(([k,v]) => v > 0).slice(0, 4);
       if (activeParts.length > 0) {
          partHtml = activeParts.map(([p, l]) => `<span>P${p}: <b>${l}</b></span>`).join('');
       } else {
          partHtml = `<span>🧩 Partitions: <b>50</b></span><span>📦 Storage: <b>Disk</b></span>`;
       }
       partitionGrid.innerHTML = partHtml;
    }
    
    $(`que-${beKey}`).textContent = queue; 
  } else {
    $(`que-${beKey}`).textContent = queue;
  }
}

function updateWSStatus(beKey, connected) {
  const dot = $(`ws-dot-${beKey}`);
  if (connected) {
    dot.className = 'status-dot connected active';
  } else {
    dot.className = 'status-dot error';
  }
}

function addLog(beKey, msg, type = 'info') {
  const panel = $(`log-${beKey}`);
  if (!panel) return;
  
  // Lọc log theo filter hiện tại
  const currentFilter = state.global.logFilters[beKey];
  if (currentFilter !== 'all' && currentFilter !== type) {
    // Nếu là 'success' hoặc 'error', ta mới lọc. 'info'/'start' luôn hiện?
    // Để đơn giản, ta chỉ lọc nếu type trùng khớp hoặc filter là all.
  }

  const div = document.createElement('div');
  div.className = `log-line ${type}`;
  div.setAttribute('data-type', type); // Để filter CSS nếu cần

  const time = new Date().toLocaleTimeString('vi-VN', { hour12: false });
  div.innerHTML = `<span class="timestamp">[${time}]</span> <span class="content">${msg}</span>`;

  panel.appendChild(div);
  
  // Kiểm tra Auto-scroll
  const chk = $(`scroll-${beKey}`);
  if (chk && chk.checked) {
    panel.scrollTop = panel.scrollHeight;
  }

  // Giới hạn số lượng log để tránh lag (Max 100)
  if (panel.children.length > 100) {
    panel.removeChild(panel.firstChild);
  }
}

function filterLogs(beKey, type) {
  state.global.logFilters[beKey] = type;
  const panel = $(`log-${beKey}`);
  if (!panel) return;
  
  const lines = panel.querySelectorAll('.log-line');
  lines.forEach(line => {
    if (type === 'all') {
      line.style.display = 'flex';
    } else {
      // type là 'success' hoặc 'error'
      line.style.display = line.classList.contains(type) ? 'flex' : 'none';
    }
  });
  
  addLog(beKey, `📝 Đã lọc Log: [${type.toUpperCase()}]`, 'info');
}


function recordResponseTime(beKey, ms) {
  const el = $(`ping-${beKey}`);
  el.textContent = ms + 'ms';
  
  // Đổi màu dựa trên độ trễ
  el.className = ms < 100 ? 'ping-fast' : (ms < 500 ? 'ping-slow' : 'ping-laggy');
}

function clearAllLogs(beKey) {
  const panel = $(`log-${beKey}`);
  if (panel) panel.innerHTML = '';
}

function clearAll(beKey) {
  state.global.stopSpam = true;
  clearAllLogs(beKey);
  state[beKey].stats = { received: 0, processed: 0, failed: 0, kafkaLag: 0 };

  state[beKey].responseTimes = [];
  updateStats(beKey);
  $(`ping-${beKey}`).textContent = '0ms';
  $(`ping-${beKey}`).className = '';
  const prog = $(`prog-${beKey}`);
  if (prog) prog.style.width = '0%';
}

// ─── MODAL CONTROL LOGIC ───
let currentBeKey = null;

function openControlModal(beKey) {
  currentBeKey = beKey;
  const backend = BACKENDS[beKey];
  const overlay = $('modal-overlay');
  
  // Set content
  $('modal-title').innerHTML = `🎮 Điều Khiển: <span class="text-${backend.color}">${backend.label}</span> (:${backend.port})`;
  $('modal-username').value = `user_${beKey}_${Math.floor(Math.random()*1000)}`;
  
  // Kafka specifics
  const kafkaExtras = $('modal-kafka-extras');
  const scenarioBox = $('modal-scenario-box');
  const scenarioDesc = $('scenario-desc');
  const beActions = $('modal-be-actions');

  if (beKey === 'kafka') {
    kafkaExtras.style.display = 'block';
    scenarioBox.style.display = 'none';
  } else {
    kafkaExtras.style.display = 'none';
    scenarioBox.style.display = 'block';
    beActions.style.display = (beKey === 'async') ? 'block' : 'none';

    if (beKey === 'direct') {
      scenarioDesc.innerHTML = `
        <b>Scenario 1: Sụp đổ Đồng bộ</b><br/>
        🎯 Mục tiêu: Spam 100 req vào Tomcat (5 threads).<br/>
        🛑 Quan sát: UI Heartbeat sẽ đứng hình, Ping tăng vọt vì nghẽn cổ chai vật lý.
      `;
      $('modal-spam-count').value = 100;
    } else {
      scenarioDesc.innerHTML = `
        <b>Scenario 2: Bất đồng bộ "Mong manh"</b><br/>
        🎯 Mục tiêu: Spam 300 req nhanh (ThreadPool 50).<br/>
        ⚠️ Quan sát: UI mượt nhưng cột "Lỗi" tăng do Queue tràn. Test rủi ro mất dữ liệu khi tắt server giữa chừng.
      `;
      $('modal-spam-count').value = 300;
    }
  }

  // Bind buttons
  $('btn-modal-single').onclick = () => {
    registerSingle(currentBeKey, $('modal-username').value);
    closeControlModal();
  };

  $('btn-modal-scenario').onclick = () => {
    const count = parseInt($('modal-spam-count').value);
    runSpamSingle(currentBeKey, count, `SCENARIO_${currentBeKey.toUpperCase()}`);
    closeControlModal();
  };

  const btnKafkaSpam = $('btn-modal-kafka-spam');
  if (btnKafkaSpam) {
    btnKafkaSpam.onclick = () => {
      const count = parseInt($('modal-kafka-spam-count').value);
      runSpamSingle('kafka', count, 'KAFKA_LOAD');
      closeControlModal();
    };
  }

  // Show modal
  overlay.classList.add('active');
}

function closeControlModal(e) {
  if (e) e.stopPropagation();
  $('modal-overlay').classList.remove('active');
  currentBeKey = null;
}

// ─── GENERIC PAUSE/RESUME LOGIC ───
async function togglePause() {
  const beKey = currentBeKey;
  if (!beKey) return;
  
  const backend = BACKENDS[beKey];
  const stateObj = state[beKey];
  stateObj.isPaused = !stateObj.isPaused;
  
  const isPaused = stateObj.isPaused;
  const btnId = (beKey === 'kafka') ? 'btn-kafka-pause' : 'btn-be-pause';
  const btn = $(btnId);
  const endpoint = isPaused ? 'pause' : 'resume';
  
  // Update UI
  btn.innerHTML = isPaused ? `▶️ Tiếp Tục ${beKey.toUpperCase()}` : `⏸️ Tạm Dừng ${beKey.toUpperCase()}`;
  btn.className = isPaused ? 'btn btn-sm btn-success w-100' : 'btn btn-sm btn-outline-warning w-100';
  
  addLog(beKey, `--- 🚥 TRẠNG THÁI: ${isPaused ? 'TẠM DỪNG' : 'TIẾP TỤC'} ---`, isPaused ? 'error' : 'success');
  
  try {
    const baseUrl = `http://${window.location.hostname}:${backend.port}/users/${endpoint}`;
    await fetch(baseUrl, { method: 'POST' });
  } catch (e) {
    addLog(beKey, 'Lỗi Kết nối: ' + e.message, 'error');
  }
}

// ─── KAFKA REAL LAG POLLING ───
function startLagPolling() {
  setInterval(async () => {
    try {
      const resp = await fetch(`http://${window.location.hostname}:8082/users/lag?groupId=email-group`);
      if (resp.ok) {
        const lag = await resp.json();
        state.kafka.stats.kafkaLag = lag;
        updateStats('kafka');
      }
    } catch (e) {
      // Im lặng nếu lỗi (server chưa bật)
    }
  }, 2000);
}

// ─── UI HEARTBEAT & FPS MONITOR ───
function startHeartbeat() {
  console.log("💓 [UI Health] Heartbeat Service Started!");
  const fpsEl = $('ui-fps');
  const hbIcon = document.querySelector('.hb-icon');
  let frameCount = 0;
  let startTime = performance.now();
  
  function update(time) {
    frameCount++;
    
    // FPS Calculation every 500ms
    if (time - startTime > 500) {
      const fps = Math.round((frameCount * 1000) / (time - startTime));
      if (fpsEl) {
        fpsEl.textContent = `${fps} FPS`;
        if (fps < 20) {
          fpsEl.className = 'hb-fps frozen';
          if (hbIcon) hbIcon.className = 'hb-icon pulse-frozen';
        } else if (fps < 45) {
          fpsEl.className = 'hb-fps lag';
          if (hbIcon) hbIcon.className = 'hb-icon pulse-lag';
        } else {
          fpsEl.className = 'hb-fps';
          if (hbIcon) hbIcon.className = 'hb-icon pulse-active';
        }
      }
      frameCount = 0;
      startTime = time;
    }
    
    requestAnimationFrame(update);
  }
  
  requestAnimationFrame(update);
}

// Khởi tạo polling khi load
function init() {
  startLagPolling();
  startHeartbeat();
}

init();

