/* 动作骨架动画数据（离线，零依赖）
   角度约定：0=上，90=右(前)，180=下，-90=左(后)；正值为顺时针
   关节链：hip(髋) → t(躯干角) → 肩/头；sa/ea=近侧上臂/前臂；hk/ka=近侧大腿/小腿；带 2 后缀为远侧
   at=脚尖方向，hd=头角，hox=头水平偏移（下巴后收用） */
(function(){
  const G = '<path d="M14 137 H186" class="prop"/>';
  const MAT = '<rect x="12" y="114" width="176" height="3" rx="1.5" class="prop"/>';
  const st = (hx, o) => Object.assign({ hip:[hx,86], t:0, sa:178, ea:182, hk:173, ka:177, at:90, sa2:178, ea2:182, hk2:187, ka2:183, at2:90 }, o || {});
  const A = {};
  const add = (k, v) => { A[k] = v; };

  add('p1_breath', { dur:4, props: MAT + '<rect x="104" y="90" width="62" height="5" rx="2" class="prop"/><path d="M108 95 v42 M162 95 v42" class="prop"/>',
    kf: [{ hip:[75,107], t:-88, sa:100, ea:70, hk:62, ka:82, at:85, sa2:100, ea2:70, hk2:66, ka2:82, at2:85 }],
    extra: '<circle cx="74" cy="100" r="5" fill="none" stroke="#2567B5" stroke-width="2"><animate attributeName="r" values="4;8;4" dur="4s" repeatCount="indefinite"/><animate attributeName="opacity" values=".35;.8;.35" dur="4s" repeatCount="indefinite"/></circle><text x="96" y="108" class="lab">吸气鼓肚子 ↔ 呼气放松</text>' });

  add('p2_chintuck', { dur:2.6, props: G,
    kf: [ st(95), st(95, { hox:-6, hd:6 }), st(95) ],
    extra: '<path d="M34 47 h13 m-4 -4 l4 4 -4 4" stroke="#2567B5" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><text x="22" y="38" class="lab">下巴水平后收</text>' });

  add('p3_wallslide', { dur:3, front:true, props:'<rect x="22" y="10" width="156" height="112" fill="#EDF1F8" rx="6"/>' + G,
    kf: [
      { hip:[100,88], t:0, sa:75, ea:5, sa2:-75, ea2:-5, hk:155, ka:175, hk2:-155, ka2:-175, at:90, at2:-90 },
      { hip:[100,88], t:0, sa:25, ea:3, sa2:-25, ea2:-3, hk:155, ka:175, hk2:-155, ka2:-175, at:90, at2:-90 },
      { hip:[100,88], t:0, sa:75, ea:5, sa2:-75, ea2:-5, hk:155, ka:175, hk2:-155, ka2:-175, at:90, at2:-90 } ],
    extra: '<text x="122" y="26" class="lab">贴墙 · 手臂 W→Y 上滑</text>' });

  add('p4_heelslide', { dur:3.2, props: MAT,
    kf: [
      { hip:[72,110], t:-88, sa:100, ea:100, hk:55, ka:130, at:70, hk2:55, ka2:125, at2:90 },
      { hip:[72,110], t:-88, sa:100, ea:100, hk:92, ka:93, at:90, hk2:55, ka2:125, at2:90 },
      { hip:[72,110], t:-88, sa:100, ea:100, hk:55, ka:130, at:70, hk2:55, ka2:125, at2:90 } ],
    extra: '<path d="M128 108 h16 m-4 -3 l4 3 -4 3" stroke="#2567B5" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><text x="118" y="128" class="lab">脚跟贴垫慢慢滑出</text>' });

  add('p5_pelvictilt', { dur:3.2, props: MAT,
    kf: [
      { hip:[78,110], t:-88, sa:100, ea:100, hk:55, ka:125, at:90, hk2:58, ka2:125, at2:90 },
      { hip:[78,108], t:-94, sa:100, ea:100, hk:55, ka:125, at:90, hk2:58, ka2:125, at2:90 },
      { hip:[78,110], t:-88, sa:100, ea:100, hk:55, ka:125, at:90, hk2:58, ka2:125, at2:90 } ],
    extra: '<text x="20" y="96" class="lab">腰部轻轻压向垫子</text><path d="M54 94 q-6 9 2 13" stroke="#2567B5" fill="none" stroke-width="2"/>' });

  add('s1_sitstand', { dur:3.5, props:'<rect x="62" y="108" width="58" height="4" rx="2" class="prop"/><path d="M66 112 v25 M116 112 v25" class="prop"/>' + G,
    kf: [
      { hip:[92,112], t:16, sa:70, ea:72, hk:92, ka:180, at:90, sa2:70, ea2:72, hk2:96, ka2:180, at2:90 },
      st(92),
      { hip:[92,112], t:16, sa:70, ea:72, hk:92, ka:180, at:90, sa2:70, ea2:72, hk2:96, ka2:180, at2:90 } ],
    extra: '<text x="24" y="56" class="lab">臀部发力站起 · 缓慢坐下</text>' });

  add('s2_glutebridge', { dur:3, props: MAT,
    kf: [
      { hip:[98,106], t:-88, sa:95, ea:100, hk:60, ka:170, at:95, sa2:95, ea2:100, hk2:64, ka2:170, at2:95 },
      { hip:[104,83], t:-135, sa:95, ea:100, hk:113, ka:175, at:95, sa2:95, ea2:100, hk2:113, ka2:175, at2:95 },
      { hip:[98,106], t:-88, sa:95, ea:100, hk:60, ka:170, at:95, sa2:95, ea2:100, hk2:64, ka2:170, at2:95 } ],
    extra: '<path d="M112 72 v-10 m-3 4 l3 -4 l3 4" stroke="#2567B5" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><text x="120" y="70" class="lab">髋部上顶，肩胛贴垫</text>' });

  add('s3_bandwalk', { dur:4, front:true, props: G,
    kf: [
      { hip:[86,90], t:0, sa:160, ea:150, sa2:-160, ea2:-150, hk:150, ka:165, hk2:-150, ka2:-165, at:90, at2:-90 },
      { hip:[96,90], t:0, sa:160, ea:150, sa2:-160, ea2:-150, hk:160, ka:168, hk2:-160, ka2:-168, at:90, at2:-90 },
      { hip:[106,90], t:0, sa:160, ea:150, sa2:-160, ea2:-150, hk:150, ka:165, hk2:-150, ka2:-165, at:90, at2:-90 },
      { hip:[96,90], t:0, sa:160, ea:150, sa2:-160, ea2:-150, hk:160, ka:168, hk2:-160, ka2:-168, at:90, at2:-90 } ],
    lines: [{ a:'knee', b:'knee2' }],
    extra: '<text x="54" y="32" class="lab">半蹲侧向迈步 · 膝撑开弹力带</text>' });

  add('s4_row', { dur:2.6, props:'<rect x="184" y="36" width="6" height="101" class="prop" fill="#EDF1F8"/>' + G,
    kf: [
      st(100, { t:2, sa:85, ea:88, sa2:85, ea2:88 }),
      st(100, { t:7, sa:195, ea:130, sa2:195, ea2:130 }),
      st(100, { t:2, sa:85, ea:88, sa2:85, ea2:88 }) ],
    lines: [{ a:'hand', xy:[186,72] }, { a:'hand2', xy:[186,72] }],
    extra: '<text x="30" y="38" class="lab">肘贴身向后拉 · 肩胛夹紧</text>' });

  add('s5_pulldown', { dur:2.8, front:true, props:'<rect x="93" y="4" width="14" height="8" rx="2" class="prop"/>' + G,
    kf: [
      { hip:[100,86], t:0, sa:30, ea:15, sa2:-30, ea2:-15, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:0, sa:115, ea:-120, sa2:-115, ea2:120, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:0, sa:30, ea:15, sa2:-30, ea2:-15, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 } ],
    lines: [{ a:'hand', xy:[100,12] }, { a:'hand2', xy:[100,12] }],
    extra: '<text x="122" y="30" class="lab">肘拉向下 · 沉肩</text>' });

  add('s6_wallpushup', { dur:2.8, props:'<rect x="150" y="14" width="5" height="123" class="prop"/>' + G,
    kf: [
      { hip:[99,96], t:35, sa:96, ea:96, sa2:96, ea2:96, hk:215, ka:215, at:75, hk2:218, ka2:218, at2:75 },
      { hip:[106,98], t:42, sa:110, ea:104, sa2:110, ea2:104, hk:220, ka:222, at:70, hk2:223, ka2:225, at2:70 },
      { hip:[99,96], t:35, sa:96, ea:96, sa2:96, ea2:96, hk:215, ka:215, at:75, hk2:218, ka2:218, at2:75 } ],
    extra: '<text x="24" y="58" class="lab">身体一条直线 · 屈肘靠近墙</text>' });

  add('s7_deadbug', { dur:3, props: MAT,
    kf: [
      { hip:[95,107], t:-88, sa:8, ea:8, sa2:8, ea2:8, hk:55, ka:130, at:80, hk2:55, ka2:130, at2:80 },
      { hip:[95,107], t:-88, sa:-30, ea:-25, sa2:8, ea2:8, hk:92, ka:92, at:90, hk2:55, ka2:130, at2:80 },
      { hip:[95,107], t:-88, sa:8, ea:8, sa2:8, ea2:8, hk:55, ka:130, at:80, hk2:55, ka2:130, at2:80 } ],
    extra: '<text x="18" y="94" class="lab">对侧手脚伸远 · 腰部贴垫</text>' });

  add('s8_pallof', { dur:2.8, props:'<rect x="4" y="64" width="6" height="18" rx="2" class="prop"/>' + G,
    kf: [
      st(100, { sa:75, ea:115, sa2:75, ea2:115 }),
      st(100, { sa:90, ea:90, sa2:90, ea2:90 }),
      st(100, { sa:75, ea:115, sa2:75, ea2:115 }) ],
    lines: [{ a:'hand', xy:[10,73] }, { a:'hand2', xy:[10,73] }],
    extra: '<text x="30" y="34" class="lab">核心收紧 · 抵抗侧向拉力</text>' });

  add('s9_splitsquat', { dur:3, props: G,
    kf: [
      { hip:[100,94], t:6, sa:172, ea:176, hk:150, ka:150, at:90, sa2:172, ea2:176, hk2:210, ka2:210, at2:270 },
      { hip:[100,108], t:12, sa:165, ea:172, hk:102, ka:181, at:90, sa2:165, ea2:172, hk2:251, ka2:190, at2:270 },
      { hip:[100,94], t:6, sa:172, ea:176, hk:150, ka:150, at:90, sa2:172, ea2:176, hk2:210, ka2:210, at2:270 } ],
    extra: '<text x="24" y="50" class="lab">后膝下沉 · 前膝对准脚尖</text>' });

  add('s10_farmer', { dur:3.6, props: G,
    kf: [
      { hip:[58,86], t:0, sa:165, ea:168, sa2:195, ea2:198, hk:150, ka:155, at:90, hk2:210, ka2:215, at2:90 },
      { hip:[82,86], t:0, sa:178, ea:181, sa2:182, ea2:185, hk:175, ka:180, at:90, hk2:185, ka2:190, at2:90 },
      { hip:[106,86], t:0, sa:195, ea:198, sa2:165, ea2:168, hk:210, ka:215, at:90, hk2:150, ka2:155, at2:90 },
      { hip:[130,86], t:0, sa:178, ea:181, sa2:182, ea2:185, hk:175, ka:180, at:90, hk2:185, ka2:190, at2:90 } ],
    marks: [{ j:'hand', r:5 }, { j:'hand2', r:5 }],
    extra: '<text x="58" y="38" class="lab">挺直行走 · 核心收紧</text>' });

  const SWIM_ARM = [-70, 0, 70, 140, 210, 290];
  const swimKF = SWIM_ARM.map(function(a, i){
    const fa = SWIM_ARM[(i + 3) % 6];
    return { hip:[105,70], t:-90, hd:(i === 3 ? -118 : -105), sa:a, ea:a + 8, sa2:fa, ea2:fa + 8,
      hk:(i % 2 ? 95 : 103), ka:(i % 2 ? 92 : 100), hk2:(i % 2 ? 103 : 95), ka2:(i % 2 ? 100 : 92), at:90, at2:90 };
  });
  const WATER = '<rect x="0" y="60" width="200" height="80" fill="#E3EEFA"/><path d="M0 60 H200" stroke="#9CC4EE" stroke-width="2" stroke-dasharray="7 5" fill="none"/>';
  add('swim', { dur:3, props: WATER, kf: swimKF, extra: '<text x="20" y="34" class="lab">手臂轮转 · 双腿轻快打水</text>' });
  add('freestyle', { dur:3, props: WATER, kf: swimKF, extra: '<text x="20" y="34" class="lab">转头换气 · 手臂风车轮转</text>' });

  add('walk', { dur:3.2, props: G + '<circle cx="36" cy="42" r="13" class="prop"/><path d="M33 55 v10 M39 55 v10" class="prop"/>',
    kf: [
      { hip:[56,86], t:2, sa:162, ea:167, sa2:198, ea2:203, hk:150, ka:154, at:90, hk2:210, ka2:214, at2:90 },
      { hip:[80,86], t:2, sa:178, ea:183, sa2:180, ea2:185, hk:176, ka:180, at:90, hk2:184, ka2:188, at2:90 },
      { hip:[104,86], t:2, sa:198, ea:203, sa2:162, ea2:167, hk:210, ka:214, at:90, hk2:150, ka2:154, at2:90 },
      { hip:[128,86], t:2, sa:178, ea:183, sa2:180, ea2:185, hk:176, ka:180, at:90, hk2:184, ka2:188, at2:90 } ],
    extra: '<text x="56" y="34" class="lab">抬头挺胸 · 手臂自然摆动</text>' });

  add('stretch_chest', { dur:3.4, props:'<rect x="118" y="10" width="4" height="127" class="prop"/><path d="M122 10 h34 M122 137 h34" class="prop"/>' + G,
    kf: [
      st(100, { t:-7, sa:90, ea:0, hk2:192 }),
      st(100, { t:5, sa:88, ea:2, hk2:192 }),
      st(100, { t:-7, sa:90, ea:0, hk2:192 }) ],
    extra: '<text x="24" y="42" class="lab">前臂贴门框 · 身体前倾开胸</text>' });

  add('stretch_calf', { dur:3.4, props:'<rect x="150" y="12" width="5" height="125" class="prop"/>' + G,
    kf: [
      { hip:[105,104], t:24, sa:99, ea:97, sa2:99, ea2:97, hk:110, ka:178, at:90, hk2:229, ka2:229, at2:75 },
      { hip:[107,102], t:27, sa:100, ea:98, sa2:100, ea2:98, hk:110, ka:178, at:90, hk2:229, ka2:229, at2:75 },
      { hip:[105,104], t:24, sa:99, ea:97, sa2:99, ea2:97, hk:110, ka:178, at:90, hk2:229, ka2:229, at2:75 } ],
    extra: '<text x="28" y="50" class="lab">后腿伸直 · 脚跟踩地</text>' });

  add('stretch_quad', { dur:3.4, props: G,
    kf: [
      st(100, { sa:190, ea:120, sa2:95, ea2:95, hk:175, ka:178, hk2:205, ka2:-17, at2:250 }),
      st(100, { sa:192, ea:126, sa2:95, ea2:95, hk:175, ka:178, hk2:205, ka2:-28, at2:250 }),
      st(100, { sa:190, ea:120, sa2:95, ea2:95, hk:175, ka:178, hk2:205, ka2:-17, at2:250 }) ],
    extra: '<text x="116" y="42" class="lab">脚跟贴臀 · 膝盖向后</text>' });

  add('stretch_glute', { dur:3.6, props:'<path d="M40 137 H170" class="prop"/>',
    kf: [
      { hip:[80,112], t:20, sa:110, ea:100, sa2:110, ea2:100, hk:35, ka:155, at:100, hk2:95, ka2:183, at2:90 },
      { hip:[80,112], t:35, sa:105, ea:100, sa2:105, ea2:100, hk:35, ka:155, at:100, hk2:95, ka2:183, at2:90 },
      { hip:[80,112], t:20, sa:110, ea:100, sa2:110, ea2:100, hk:35, ka:155, at:100, hk2:95, ka2:183, at2:90 } ],
    extra: '<text x="112" y="70" class="lab">脚踝搭膝 · 身体前压</text>' });

  add('stretch_back', { dur:4, props:'<path d="M50 137 H180" class="prop"/>',
    kf: [
      { hip:[75,128], t:35, hd:25, sa:70, ea:72, sa2:70, ea2:72, hk:92, ka:92, at:90, hk2:95, ka2:95, at2:90 },
      { hip:[75,128], t:60, hd:48, sa:80, ea:85, sa2:80, ea2:85, hk:92, ka:92, at:90, hk2:95, ka2:95, at2:90 },
      { hip:[75,128], t:35, hd:25, sa:70, ea:72, sa2:70, ea2:72, hk:92, ka:92, at:90, hk2:95, ka2:95, at2:90 } ],
    extra: '<text x="96" y="58" class="lab">从髋部折叠向前 · 背放长</text>' });

  add('stretch_shoulder', { dur:3.2, front:true, props: G,
    kf: [
      { hip:[100,86], t:0, sa:145, ea:-80, sa2:-110, ea2:70, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:0, sa:145, ea:-100, sa2:-110, ea2:70, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:0, sa:145, ea:-80, sa2:-110, ea2:70, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 } ],
    extra: '<text x="112" y="34" class="lab">手臂横过胸前 · 轻压</text>' });

  add('sh_circles', { dur:3, front:true, props: G,
    kf: [90, 30, -30, -90, -150, -210, -270].map(function(a){
      return { hip:[100,86], t:0, sa:a, ea:a - 10, sa2:-a, ea2:-(a - 10), hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 };
    }),
    extra: '<text x="120" y="32" class="lab">双肩向后绕环</text>' });

  add('side_stretch', { dur:3.4, front:true, props: G,
    kf: [
      { hip:[100,86], t:0, hd:0, sa:20, ea:10, sa2:-170, ea2:-178, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:20, hd:20, sa:25, ea:18, sa2:-170, ea2:-178, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:0, hd:0, sa:20, ea:10, sa2:-170, ea2:-178, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 } ],
    extra: '<text x="116" y="28" class="lab">手臂贴耳 · 向侧拉伸</text>' });

  add('chest_open', { dur:3.4, front:true, props: G,
    kf: [
      { hip:[100,86], t:0, sa:145, ea:158, sa2:-145, ea2:-158, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:-4, sa:132, ea:150, sa2:-132, ea2:-150, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:0, sa:145, ea:158, sa2:-145, ea2:-158, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 } ],
    extra: '<text x="110" y="32" class="lab">双手背后交扣 · 胸腔上提</text>' });

  add('triceps_stretch', { dur:3.4, front:true, props: G,
    kf: [
      { hip:[100,86], t:0, sa:30, ea:-140, sa2:60, ea2:60, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:0, sa:28, ea:-155, sa2:62, ea2:62, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 },
      { hip:[100,86], t:0, sa:30, ea:-140, sa2:60, ea2:60, hk:173, ka:177, hk2:-173, ka2:-177, at:90, at2:-90 } ],
    extra: '<text x="114" y="26" class="lab">肘上提 · 手沿背后下滑</text>' });

  window.EX_ANIM = A;
})();
