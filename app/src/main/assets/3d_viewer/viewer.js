// Standalone 3D Mannequin DOM & Kinematic Renderer
(function() {
  let currentMoveName = 'Back Squat';
  let move = moveFor(currentMoveName);
  let isPlaying = true;
  let speed = 1.0;
  let autoOrbit = true;
  let yaw = -35;
  let pitch = -14;
  let clock = 0;
  let lastTime = performance.now();

  const viewport = document.getElementById('viewport');
  const world = document.getElementById('world');
  const titleEl = document.getElementById('title');
  const targetsEl = document.getElementById('targets');
  const cueEl = document.getElementById('cue');

  function createBox(w, h, d, color, glow) {
    const box = document.createElement('div');
    box.className = 'box';
    box.style.width = w + 'px';
    box.style.height = h + 'px';

    const faces = [
      { w: w, h: h, tf: `translateZ(${d / 2}px)`, br: 1 },
      { w: w, h: h, tf: `rotateY(180deg) translateZ(${d / 2}px)`, br: 0.6 },
      { w: d, h: h, tf: `rotateY(90deg) translateZ(${w / 2}px)`, br: 0.82 },
      { w: d, h: h, tf: `rotateY(-90deg) translateZ(${w / 2}px)`, br: 0.72 },
      { w: w, h: d, tf: `rotateX(90deg) translateZ(${h / 2}px)`, br: 1.2 },
      { w: w, h: d, tf: `rotateX(-90deg) translateZ(${h / 2}px)`, br: 0.5 }
    ];

    faces.forEach(f => {
      const face = document.createElement('div');
      face.className = 'face';
      face.style.position = 'absolute';
      face.style.left = ((w - f.w) / 2) + 'px';
      face.style.top = ((h - f.h) / 2) + 'px';
      face.style.width = f.w + 'px';
      face.style.height = f.h + 'px';
      face.style.transform = f.tf;
      face.style.background = color;
      face.style.filter = `brightness(${f.br})`;
      face.style.boxShadow = glow ? 'inset 0 0 10px rgba(255,255,255,0.35)' : 'inset 0 0 0 1px rgba(0,0,0,0.25)';
      box.appendChild(face);
    });

    return box;
  }

  function setTransform(el, tf) {
    if (el) el.style.transform = tf;
  }

  // Build Skeletal Hierarchy
  const els = {};

  function buildMannequin() {
    world.innerHTML = '<div class="floor"></div>';

    // Pelvis
    const pelvis = document.createElement('div');
    pelvis.style.position = 'absolute';
    pelvis.style.transformStyle = 'preserve-3d';
    const pelvisBox = createBox(26, 22, 36, '#27272a');
    pelvisBox.style.left = '-13px';
    pelvisBox.style.top = '-11px';
    pelvis.appendChild(pelvisBox);
    els.pelvis = pelvis;

    // Torso
    const torso = document.createElement('div');
    torso.style.position = 'absolute';
    torso.style.transformStyle = 'preserve-3d';
    const torsoBox = createBox(30, L.torso, 38, move.hl.includes('torso') ? '#c6ff3d' : '#52525b', move.hl.includes('torso'));
    torsoBox.style.left = '-15px';
    torso.appendChild(torsoBox);

    // Head & Shoulders
    const headBar = document.createElement('div');
    headBar.style.position = 'absolute';
    headBar.style.top = L.torso + 'px';
    headBar.style.transformStyle = 'preserve-3d';

    const sBar = createBox(34, 9, 62, '#27272a');
    sBar.style.left = '-17px'; sBar.style.top = '-3px';
    headBar.appendChild(sBar);

    const head = createBox(26, 26, 26, '#d4d4d8');
    head.style.left = '-13px'; head.style.top = '8px';
    headBar.appendChild(head);

    // Arms
    ['L', 'R'].forEach(side => {
      const s = side === 'L' ? -1 : 1;
      const ua = document.createElement('div');
      ua.style.position = 'absolute';
      ua.style.transformStyle = 'preserve-3d';
      const uaBox = createBox(15, L.ua, 15, move.hl.includes('arms') ? '#c6ff3d' : '#52525b', move.hl.includes('arms'));
      uaBox.style.left = '-7.5px';
      ua.appendChild(uaBox);

      const fa = document.createElement('div');
      fa.style.position = 'absolute';
      fa.style.top = L.ua + 'px';
      fa.style.transformStyle = 'preserve-3d';
      const faBox = createBox(13, L.fa, 13, move.hl.includes('arms') ? '#c6ff3d' : '#52525b', move.hl.includes('arms'));
      faBox.style.left = '-6.5px';
      fa.appendChild(faBox);

      ua.appendChild(fa);
      headBar.appendChild(ua);
      els['ua' + side] = ua;
      els['fa' + side] = fa;
    });

    torso.appendChild(headBar);
    pelvis.appendChild(torso);
    els.torso = torso;

    // Legs
    ['L', 'R'].forEach(side => {
      const s = side === 'L' ? -1 : 1;
      const th = document.createElement('div');
      th.style.position = 'absolute';
      th.style.transformStyle = 'preserve-3d';
      const thBox = createBox(20, L.thigh, 19, move.hl.includes('legs') ? '#c6ff3d' : '#52525b', move.hl.includes('legs'));
      thBox.style.left = '-10px';
      th.appendChild(thBox);

      const sh = document.createElement('div');
      sh.style.position = 'absolute';
      sh.style.top = L.thigh + 'px';
      sh.style.transformStyle = 'preserve-3d';
      const shBox = createBox(16, L.shin, 16, move.hl.includes('legs') ? '#c6ff3d' : '#52525b', move.hl.includes('legs'));
      shBox.style.left = '-8px';
      sh.appendChild(shBox);

      const ft = document.createElement('div');
      ft.style.position = 'absolute';
      ft.style.top = L.shin + 'px';
      ft.style.transformStyle = 'preserve-3d';
      const ftBox = createBox(32, L.foot, 16, '#e4e4e7');
      ftBox.style.left = '-8px';
      ft.appendChild(ftBox);

      sh.appendChild(ft);
      th.appendChild(sh);
      pelvis.appendChild(th);
      els['th' + side] = th;
      els['sh' + side] = sh;
      els['ft' + side] = ft;
    });

    world.appendChild(pelvis);
  }

  function updateMove(moveName) {
    currentMoveName = moveName;
    move = moveFor(moveName);
    titleEl.textContent = move.label;
    targetsEl.textContent = move.muscles;
    cueEl.textContent = move.cue;
    buildMannequin();
  }

  // Animation Loop
  function tick(now) {
    const dt = Math.min(0.05, (now - lastTime) / 1000);
    lastTime = now;

    if (isPlaying) {
      clock += dt * speed;
    }
    if (autoOrbit) {
      yaw = (yaw + dt * 18) % 360;
    }

    const u = (clock / (move.period || 3.0)) % 1;
    const sm = x => x * x * (3 - 2 * x);
    const ph = u < 0.45 ? sm(u / 0.45) : u < 0.52 ? 1 : u < 0.9 ? 1 - sm((u - 0.52) / 0.38) : 0;

    const p = { ...P0 };
    Object.keys(P0).forEach(k => {
      p[k] = lerp(move.a[k] ?? 0, move.b[k] ?? 0, ph);
    });

    const solved = solve(p, move);
    setTransform(els.pelvis, `translate(${solved.hip.x}px, ${-solved.hip.y}px)`);

    const a1 = p.h, a2 = p.h - p.k;
    ['L', 'R'].forEach(side => {
      const s = side === 'L' ? -1 : 1;
      setTransform(els['th' + side], `translateZ(${s * 10}px) rotateZ(${-a1}deg)`);
      setTransform(els['sh' + side], `rotateZ(${-(a2 - a1)}deg)`);
      setTransform(els['ft' + side], `rotateZ(${-p.ft}deg)`);
      setTransform(els['ua' + side], `translateZ(${s * 30}px) rotateX(${p.ab * s}deg) rotateZ(${-p.s - 180 - p.t}deg)`);
      setTransform(els['fa' + side], `rotateZ(${-p.e}deg)`);
    });

    setTransform(els.torso, `rotateZ(${180 + p.t}deg)`);
    setTransform(world, `rotateX(${pitch}deg) rotateY(${yaw}deg) translateX(${-(move.cx || 0)}px)`);

    requestAnimationFrame(tick);
  }

  // Touch / Pointer Drag
  let isDragging = false;
  let lastClientX = 0;
  let lastClientY = 0;

  viewport.addEventListener('pointerdown', (e) => {
    isDragging = true;
    autoOrbit = false;
    lastClientX = e.clientX;
    lastClientY = e.clientY;
    viewport.setPointerCapture(e.pointerId);
  });

  viewport.addEventListener('pointermove', (e) => {
    if (!isDragging) return;
    const dx = e.clientX - lastClientX;
    const dy = e.clientY - lastClientY;
    lastClientX = e.clientX;
    lastClientY = e.clientY;

    yaw += dx * 0.6;
    pitch = Math.max(-60, Math.min(60, pitch - dy * 0.4));
  });

  viewport.addEventListener('pointerup', () => { isDragging = false; });
  viewport.addEventListener('pointercancel', () => { isDragging = false; });

  // Expose global interface for Android WebView bridge
  window.Viewer3D = {
    loadExercise: updateMove,
    play: () => { isPlaying = true; },
    pause: () => { isPlaying = false; },
    togglePlay: () => { isPlaying = !isPlaying; return isPlaying; },
    setSpeed: (s) => { speed = s; },
    toggleOrbit: () => { autoOrbit = !autoOrbit; return autoOrbit; },
    setView: (deg) => { yaw = deg; pitch = -14; autoOrbit = false; }
  };

  // Wire up UI buttons
  document.getElementById('btnPlay').onclick = function() {
    isPlaying = !isPlaying;
    this.textContent = isPlaying ? 'Pause' : 'Play';
    this.classList.toggle('active', isPlaying);
  };

  document.querySelectorAll('[data-speed]').forEach(btn => {
    btn.onclick = function() {
      document.querySelectorAll('[data-speed]').forEach(b => b.classList.remove('active'));
      this.classList.add('active');
      speed = parseFloat(this.getAttribute('data-speed'));
    };
  });

  document.getElementById('btnOrbit').onclick = function() {
    autoOrbit = !autoOrbit;
    this.classList.toggle('active', autoOrbit);
  };

  document.querySelectorAll('[data-view]').forEach(btn => {
    btn.onclick = function() {
      const v = parseFloat(this.getAttribute('data-view'));
      yaw = v;
      pitch = -14;
      autoOrbit = false;
    };
  });

  // Initialize
  updateMove('Back Squat');
  requestAnimationFrame(tick);
})();
