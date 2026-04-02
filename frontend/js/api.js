async function registerSingle(beKey, username, silent = false) {
  const port = BACKENDS[beKey].port;
  const beState = state[beKey];

  beState.stats.received++;
  updateStats(beKey);

  const start = Date.now();
  try {
    await fetch(`http://${window.location.hostname}:${port}/users/register?username=${encodeURIComponent(username)}`, {
      method: 'POST',
    });
    const rt = Date.now() - start;
    beState.responseTimes.push(rt);
    updateStats(beKey);
    
    // Luôn ghi nhận Ping để UI hiển thị số, nhưng KHÔNG ghi Log từng dòng
    recordResponseTime(beKey, rt);
  } catch (e) {
    beState.stats.failed++;
    updateStats(beKey);
    addLog(beKey, 'Exception - Fetch API Lỗi: ' + e.message, 'error');
  }
}

async function registerAll() {
  const username = $('username').value || 'multi_user';
  // Fire all concurrently
  await Promise.all([
    registerSingle('direct', username),
    registerSingle('async', username),
    registerSingle('kafka', username)
  ]);
}
