const sleep = ms => new Promise(res => setTimeout(res, ms));

async function runSpamSingle(beKey, count, label) {
  const beState = state[beKey];
  const backend = BACKENDS[beKey];
  
  $(`prog-${beKey}`).style.width = '0%';
  addLog(beKey, `━━━ Khởi chạy đẩy ${count} Requests → Tới Hệ thống ${backend.label} / Cổng :${backend.port} ━━━`, 'start');

  let done = 0;
  const promises = [];
  const startTotal = Date.now();
  state.global.stopSpam = false;


  for (let i = 1; i <= count; i++) {
    if (state.global.stopSpam) {
      addLog(beKey, `⏹️ [Dừng] Ngắt tiến trình Spam theo yêu cầu!`, 'error');
      break;
    }
    const username = `${label}_${i}`;

    const p = registerSingle(beKey, username, true).finally(() => {
      done++;
      const percent = Math.floor((done / count) * 100);
      $(`prog-${beKey}`).style.width = percent + '%';
    });
    promises.push(p);
  }

  await Promise.all(promises);

  const elapsed = ((Date.now() - startTotal) / 1000).toFixed(2);
  const avgRt = beState.responseTimes.length > 0
    ? Math.round(beState.responseTimes.reduce((a, b) => a + b) / beState.responseTimes.length)
    : 0;

  addLog(beKey, `━━━ FINISH: Hoàn thành chạy ${count} Request / Time: ${elapsed}s | Độ trễ TB: ${avgRt}ms ━━━`, 'submit');
}

