// Offline Kinematics & Routine Database for 3D Viewer
const P0 = { h: 0, k: 0, t: 0, s: 0, e: 0, ab: 0, dy: 0, ft: 0 };
const LIE = { h: 90, k: 90, t: -90 };

const M = {
  bench: { label: 'Bench press', tool: 'barbell', bench: true, hl: ['arms', 'torso'], cx: -110, muscles: 'Chest, front delts, triceps', cue: 'Lower to mid-chest, drive up and lock out.',
    a: {...LIE, s: 180, e: 0}, b: {...LIE, s: 70, e: 110} },
  incline: { label: 'Incline press', tool: 'dumbbell', bench: true, hl: ['arms', 'torso'], cx: -80, muscles: 'Upper chest, front delts', cue: 'Press up and slightly together over the upper chest.',
    a: { h: 90, k: 90, t: -60, s: 180, e: 0 }, b: { h: 90, k: 90, t: -60, s: 95, e: 95 } },
  fly: { label: 'Cable fly', tool: 'dumbbell', hl: ['arms', 'torso'], muscles: 'Chest, front delts', cue: 'Soft elbows, hug a barrel and squeeze at the centre.',
    a: { t: 8, s: 20, e: 15, ab: 80 }, b: { t: 8, s: 70, e: 15, ab: 8 } },
  skull: { label: 'Skull crusher', tool: 'barbell', bench: true, hl: ['arms'], cx: -110, muscles: 'Triceps', cue: 'Elbows fixed, lower the bar towards the forehead.',
    a: { ...LIE, s: 180, e: 0 }, b: { ...LIE, s: 180, e: 105 } },
  pushdown: { label: 'Rope pushdown', tool: 'dumbbell', hl: ['arms'], muscles: 'Triceps', cue: 'Pin elbows to your sides and extend fully.',
    a: { t: 6, s: 8, e: 125 }, b: { t: 6, s: 8, e: 5 } },
  deadlift: { label: 'Deadlift', tool: 'barbell', hl: ['legs', 'torso'], muscles: 'Glutes, hamstrings, back', cue: 'Flat back, push the floor away, lock hips at top.',
    a: {h: 70, k: 75, t: 62, s: 5, e: 0}, b: {h: 0, k: 0, t: 0, s: 16, e: 0} },
  rdl: { label: 'Romanian deadlift', tool: 'barbell', hl: ['legs', 'torso'], muscles: 'Hamstrings, glutes', cue: 'Hips back, bar slides down the thighs.',
    a: {h: 40, k: 12, t: 70, s: 0, e: 0}, b: {h: 0, k: 0, t: 0, s: 10, e: 0} },
  pullup: { label: 'Pull-up', tool: 'bar', hang: true, hl: ['arms', 'torso'], muscles: 'Lats, biceps, upper back', cue: 'Start from a dead hang, chin over the bar.',
    a: { h: 0, k: 60, t: 0, s: 180, e: 0 }, b: { h: 0, k: 60, t: -8, s: 25, e: 155 } },
  row: { label: 'Barbell row', tool: 'barbell', hl: ['arms', 'torso'], muscles: 'Lats, rhomboids, rear delts', cue: 'Hinge, brace, pull the bar to your lower ribs.',
    a: {h: 40, k: 45, t: 50, s: 0, e: 0}, b: {h: 40, k: 45, t: 50, s: -45, e: 70} },
  curl: { label: 'Biceps curl', tool: 'dumbbell', hl: ['arms'], muscles: 'Biceps, brachialis', cue: 'Elbows stay still; no swinging.',
    a: { s: 0, e: 0 }, b: { s: 0, e: 140 } },
  icurl: { label: 'Incline curl', tool: 'dumbbell', hl: ['arms'], muscles: 'Biceps (long head)', cue: 'Arms behind the torso for a deep stretch.',
    a: { t: -25, s: -30, e: 0 }, b: { t: -25, s: -30, e: 140 } },
  squat: { label: 'Back squat', tool: 'barbell', hl: ['legs'], muscles: 'Quads, glutes, core', cue: 'Brace, sit between the hips, drive up.',
    a: {s: -33, e: 197}, b: {h: 80, k: 115, t: 35, s: -68, e: 197} },
  front: { label: 'Front squat', tool: 'barbell', hl: ['legs', 'torso'], muscles: 'Quads, upper back, core', cue: 'Elbows high, chest tall.',
    a: {s: 29, e: 155}, b: {h: 85, k: 120, t: 12, s: 17, e: 155} },
  legpress: { label: 'Leg press (standing sim.)', tool: 'none', hl: ['legs'], muscles: 'Quads, glutes', cue: 'Controlled depth, never lock knees hard.',
    a: { s: 60, e: 0 }, b: { h: 70, k: 100, t: 25, s: 60, e: 0 } },
  lunge: { label: 'Lunge (squat sim.)', tool: 'dumbbell', hl: ['legs'], muscles: 'Quads, glutes', cue: 'Long step, vertical shin, drive through the heel.',
    a: { s: 0, e: 0 }, b: { h: 60, k: 100, t: 15, s: 0, e: 0 } },
  calf: { label: 'Calf raise', tool: 'none', hl: ['legs'], muscles: 'Gastrocnemius, soleus', cue: 'Pause at the top, full stretch at the bottom.',
    a: { s: 0 }, b: { s: 0, dy: 14, ft: 35 } },
  ohp: { label: 'Overhead press', tool: 'barbell', hl: ['arms', 'torso'], muscles: 'Shoulders, triceps, core', cue: 'Squeeze glutes, press straight overhead.',
    a: {s: 29, e: 155}, b: {s: 180, e: 0} },
  lateral: { label: 'Lateral raise', tool: 'dumbbell', hl: ['arms'], muscles: 'Side delts', cue: 'Lead with the elbows to shoulder height.',
    a: { s: 0, e: 12, ab: 4 }, b: { s: 0, e: 12, ab: 88 } },
  facepull: { label: 'Face pull', tool: 'dumbbell', hl: ['arms', 'torso'], muscles: 'Rear delts, upper back', cue: 'Pull to the face, elbows high and wide.',
    a: { t: -4, s: 88, e: 0, ab: 10 }, b: { t: -4, s: 70, e: 85, ab: 40 } },
  legraise: { label: 'Hanging leg raise', tool: 'bar', hang: true, hl: ['torso', 'legs'], muscles: 'Abs, hip flexors', cue: 'No swing, curl the pelvis up.',
    a: { h: 0, k: 0, t: 0, s: 180, e: 0 }, b: { h: 95, k: 10, t: 6, s: 180, e: 0 } },
  crunch: { label: 'Crunch', tool: 'none', bench: true, hl: ['torso'], cx: -110, muscles: 'Abs', cue: 'Ribs to pelvis, exhale at the top.',
    a: { ...LIE, s: -100, e: 120 }, b: { h: 90, k: 90, t: -52, s: -100, e: 120 } },
  clean: { label: 'Power clean', tool: 'barbell', hl: ['legs', 'arms', 'torso'], muscles: 'Full body, explosive hips', cue: 'Jump, shrug, catch the bar on the shoulders.',
    a: {h: 70, k: 75, t: 62, s: 5, e: 0}, b: {h: 0, k: 0, t: 0, s: 29, e: 155} },
  pushpress: { label: 'Push press', tool: 'barbell', hl: ['legs', 'arms'], muscles: 'Shoulders, triceps, legs', cue: 'Dip, drive, punch the bar overhead.',
    a: {h: 25, k: 50, t: 0, s: 29, e: 155}, b: {s: 180, e: 0} },
  carry: { label: 'Farmer carry', tool: 'dumbbell', hl: ['arms', 'torso'], muscles: 'Grip, traps, core', cue: 'Tall posture, short quick steps.',
    a: { h: 4, k: 4, s: 0, e: 0, dy: 0 }, b: { h: -3, k: 8, s: 0, e: 0, dy: 3 }, period: 1.2 },
  fold: { label: 'Mobility flow', tool: 'none', hl: ['legs', 'torso'], muscles: 'Hamstrings, lower back, hips', cue: 'Breathe slowly and relax into the stretch.',
    a: { s: 180, e: 0 }, b: { h: 8, k: 0, t: 85, s: 5, e: 0 }, period: 4 },
  rest: { label: 'Recovery', tool: 'none', hl: [], cx: -110, muscles: 'Whole body', cue: 'Sleep 7-9 hours. Muscle is built while you rest.',
    a: { h: 90, k: 0, t: -90, s: 100, e: 0 }, b: { h: 90, k: 0, t: -90, s: 100, e: 0, dy: 2 }, period: 5 },
};

const BY_NAME = {
  'Barbell Bench Press': 'bench', 'Incline Dumbbell Press': 'incline', 'Cable Fly': 'fly', 'Skull Crushers': 'skull', 'Rope Pushdown': 'pushdown',
  'Deadlift': 'deadlift', 'Weighted Pull-up': 'pullup', 'Barbell Row': 'row', 'Incline Curl': 'icurl', 'Hammer Curl': 'curl',
  'Back Squat': 'squat', 'Romanian Deadlift': 'rdl', 'Leg Press': 'legpress', 'Walking Lunge': 'lunge', 'Calf Raise': 'calf',
  'Foam Rolling': 'fold', 'Hip Mobility Flow': 'fold', 'Light Walk': 'carry',
  'Overhead Press': 'ohp', 'Lateral Raise': 'lateral', 'Face Pull': 'facepull', 'Hanging Leg Raise': 'legraise', 'Cable Crunch': 'crunch',
  'Power Clean': 'clean', 'Push Press': 'pushpress', 'Front Squat': 'front', 'Farmer Carry': 'carry', 'Sleep, eat, repeat': 'rest',
  // V-Taper PPL synonyms
  'Wide-Grip Lat Pulldowns': 'pullup', 'Lat Pulldowns': 'pullup', 'Seated Cable Rows': 'row', 'Dumbbell Hammer Curls': 'curl',
  'Goblet Squats': 'squat', 'Bodyweight Squats': 'squat', 'Lying Leg Curls': 'rdl', 'Plank': 'crunch',
  'Dumbbell Lateral Raises': 'lateral', 'Incline Push-ups': 'bench', 'Tricep Rope Pushdowns': 'pushdown',
};

const L = { thigh: 70, shin: 68, torso: 84, ua: 48, fa: 44, foot: 8 };
const BAR_Y = 330;
const rad = (d) => (d * Math.PI) / 180;
const lerp = (a, b, t) => a + (b - a) * t;

function solve(p, mv) {
  const a1 = p.h, a2 = p.h - p.k;
  const dx = L.thigh * Math.sin(rad(a1)) + L.shin * Math.sin(rad(a2));
  const dyl = L.thigh * Math.cos(rad(a1)) + L.shin * Math.cos(rad(a2));
  const arm = (hx, hy) => {
    const ex = hx + L.ua * Math.sin(rad(p.s)), ey = hy - L.ua * Math.cos(rad(p.s));
    return { elbow: { x: ex, y: ey }, hand: { x: ex + L.fa * Math.sin(rad(p.s + p.e)), y: ey - L.fa * Math.cos(rad(p.s + p.e)) } };
  };
  let px = -dx, py = dyl + L.foot + p.dy;
  if (mv.hang) {
    const r = arm(L.torso * Math.sin(rad(p.t)), L.torso * Math.cos(rad(p.t)));
    px = -r.hand.x;
    py = BAR_Y - r.hand.y;
  }
  const hip = { x: px, y: py };
  const knee = { x: px + L.thigh * Math.sin(rad(a1)), y: py - L.thigh * Math.cos(rad(a1)) };
  const ankle = { x: knee.x + L.shin * Math.sin(rad(a2)), y: knee.y - L.shin * Math.cos(rad(a2)) };
  const shoulder = { x: px + L.torso * Math.sin(rad(p.t)), y: py + L.torso * Math.cos(rad(p.t)) };
  return { hip, knee, ankle, shoulder, ...arm(shoulder.x, shoulder.y) };
}

function moveFor(name) {
  return M[BY_NAME[name] || name] || M.squat;
}
