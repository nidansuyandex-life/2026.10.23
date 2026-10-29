const EXERCISES = {
  'P1 90/90 呼吸': { name:'P1 90/90 呼吸', target:'肋骨与腹部控制', purpose:'激活深层核心与呼吸', how:'2×5 次呼吸，肋骨和腹部轻轻扩张；不耸肩、不憋气。', detail:'1. 仰卧，双腿搭在椅子或墙上，呈 90/90 姿势。\n2. 吸气约 4 秒，肋骨和腹部向两侧轻轻扩张。\n3. 呼气约 6 秒，肚子自然下沉，不强行收腹。\n4. 肩膀完全放松。', breath:'吸 4 秒，呼 6 秒。', mistakes:'耸肩、憋气、强行吸肚子。', equipment:'瑜伽垫/墙面' },
  'P2 下巴轻收': { name:'P2 下巴轻收', target:'颈深屈肌', purpose:'改善头前倾', how:'2×8，停 3 秒，头水平后移。', detail:'1. 坐直或站立，眼睛平视前方。\n2. 下巴水平向后平移（像做双下巴），不要低头。\n3. 保持 3 秒，颈后有轻微拉伸感。\n4. 缓慢回正。', breath:'自然呼吸。', mistakes:'低头、用力过猛、颈痛时强行做。', equipment:'无' },
  'P3 墙面滑手': { name:'P3 墙面滑手', target:'肩胛与躯干协调', purpose:'改善圆肩', how:'2×8-10 次，肋骨不外翻，不耸肩。', detail:'1. 背对墙站立，脚后跟离墙一拳。\n2. 手臂呈“W”型贴墙，手背贴墙。\n3. 沿墙向上滑至“Y”型，肋骨不外翻。\n4. 缓慢回落。', breath:'上滑吸气，下落呼气。', mistakes:'耸肩、腰离开墙面、肋骨外翻。', equipment:'墙面' },
  'P4 仰卧脚跟滑动': { name:'P4 仰卧脚跟滑动', target:'低负荷核心控制', purpose:'骨盆稳定', how:'2×6-8/侧，呼气脚跟滑远，腰背舒适。', detail:'1. 仰卧，双腿屈膝，双脚踩地。\n2. 呼气时一只脚贴地慢慢滑远，腰部保持贴地。\n3. 吸气缓慢收回。\n4. 如果拱腰则缩短范围。', breath:'滑出呼气，收回吸气。', mistakes:'腰部拱起、动作过快。', equipment:'瑜伽垫' },
  'P5 小范围骨盆控制': { name:'P5 小范围骨盆控制', target:'骨盆感知', purpose:'灵活骨盆', how:'1-2×5，幅度很小、无痛、呼吸顺畅。', detail:'1. 仰卧屈膝。\n2. 骨盆非常小范围地前倾和后倾。\n3. 不追求大幅度，感受轻微移动即可。', breath:'顺畅呼吸。', mistakes:'腰部代偿、幅度过大。', equipment:'瑜伽垫' },
  'S1 椅子坐站': { name:'S1 椅子坐站/箱式深蹲', target:'臀大肌、股四头肌', purpose:'下肢力量', how:'2-3×8-12，臀部后移，脚掌踩稳。', detail:'1. 站在椅子前，双脚与肩同宽。\n2. 臀部向后坐，轻触椅面。\n3. 脚掌发力站起。\n4. 膝盖内扣或弓腰时提高座椅。', breath:'下降吸气，起身呼气。', mistakes:'膝盖内扣、弓腰、完全坐死。', equipment:'椅子' },
  'S2 臂桥': { name:'S2 臂桥（臀桥）', target:'臀大肌、骨盆稳定', purpose:'臀部塑形', how:'2-3×8-12，呼气抬髋，不用腰顶。', detail:'1. 仰卧屈膝，脚跟离臀部一掌。\n2. 呼气臀部发力抬起，至肩-髋-膝一条线。\n3. 顶端停 1 秒。\n4. 腰不适则缩小幅度。', breath:'抬髋呼气，下降吸气。', mistakes:'腰部代偿、过度挺腰。', equipment:'瑜伽垫' },
  'S3 弹力带侧向走': { name:'S3 弹力带侧向走', target:'臀中肌', purpose:'骨盆水平控制', how:'2×每侧6-12步，小步、骨盆水平。', detail:'1. 弹力带套在膝盖上方或脚踝。\n2. 半蹲，向一侧迈出小步，另一脚跟随。\n3. 保持骨盆水平，不要左右晃动。', breath:'自然呼吸。', mistakes:'骨盆摇晃、膝盖内扣、牵扯骶髂关节。', equipment:'弹力带' },
  'S4 划船': { name:'S4 划船', target:'菱形肌、中下斜方肌', purpose:'背部线条', how:'2-3×8-12，肘向后拉，肩胛骨向后下。', detail:'1. 弹力带或哑铃。\n2. 背部挺直，拉至腰侧。\n3. 肩胛骨收紧，停 1 秒。', breath:'拉时呼气。', mistakes:'耸肩、腰部借力。', equipment:'弹力带/哑铃' },
  'S5 前方高位下拉': { name:'S5 前方高位下拉', target:'背阔肌', purpose:'背部宽度', how:'2-3×8-12，极轻阻力开始，下拉至锁骨。', detail:'1. 弹力带固定在高处。\n2. 双手下拉至锁骨前方。\n3. 肋骨不外翻。', breath:'下拉呼气。', mistakes:'腰后仰借力、耸肩。', equipment:'弹力带' },
  'S6 墙/高台俯卧撑': { name:'S6 墙/高台俯卧撑', target:'胸、肩、肱三头', purpose:'上肢推力', how:'2-3×8-12，身体一条直线。', detail:'1. 双手撑墙或桌沿。\n2. 屈肘靠近，推起。\n3. 身体保持直线。', breath:'推起呼气。', mistakes:'塌腰、肘部完全张开。', equipment:'墙面/桌子' },
  'S7 死虫式': { name:'S7 死虫式', target:'深层核心', purpose:'躯干稳定', how:'2×6-8/侧，对侧手脚伸展，腰部贴地。', detail:'1. 仰卧，双手双脚朝天。\n2. 对侧手脚下落，腰部紧贴地面。\n3. 缓慢收回换边。', breath:'伸展呼气。', mistakes:'腰部离地、动作太快。', equipment:'瑜伽垫' },
  'S8 Pallof 抗旋转': { name:'S8 Pallof 抗旋转', target:'抗旋转核心', purpose:'核心稳定', how:'2×8-12/侧，抵抗旋转。', detail:'1. 侧对弹力带，双手握在胸前。\n2. 向前推，核心抵抗身体被拉转。', breath:'推出呼气。', mistakes:'身体被拉转、腰部代偿。', equipment:'弹力带' },
  'S9 分腿蹲': { name:'S9 分腿蹲', target:'臀腿单侧力量', purpose:'平衡与下肢稳定', how:'2×6-8/侧，骶髂稳定时尝试。', detail:'1. 弓步姿势。\n2. 下蹲至前腿大腿接近平行。\n3. 前脚掌发力站起。', breath:'下蹲吸气，站起呼气。', mistakes:'膝盖内扣、身体晃动。', equipment:'自重' },
  'S10 农夫行走': { name:'S10 农夫行走', target:'全身核心与握力', purpose:'全身功能性', how:'每手 1-3kg 或更轻，20秒，可选项。', detail:'1. 双手提重物。\n2. 挺胸收腹，小步慢走。\n3. 牵扯腰骶则取消。', breath:'自然呼吸。', mistakes:'身体侧倾、弯腰。', equipment:'哑铃/水瓶' },
  '游泳': { name:'游泳', target:'全身协调', purpose:'心肺与全身', how:'水中适应、缓慢吐气、扶池边练习。', detail:'按阶段练习：水中适应、呼吸、漂浮、扶板打腿、分解练习。', breath:'水下呼气，抬头吸气。', mistakes:'憋气、慌张。', equipment:'泳衣、泳镜' },
  '散步': { name:'散步', target:'主动恢复', purpose:'低强度有氧', how:'轻松步行。', detail:'轻松慢走，放松心情。', breath:'自然。', mistakes:'走太快。', equipment:'运动鞋' },
  '胸肌拉伸': { name:'胸肌拉伸', target:'胸前拉伸', purpose:'改善圆肩', how:'20-30秒。', detail:'1. 站在门框或墙边，单臂贴墙。\n2. 肘与肩同高。\n3. 身体向对侧缓慢前移。', breath:'自然。', mistakes:'拉伸到疼。', equipment:'门框/墙' },
  '小腿拉伸': { name:'小腿拉伸', target:'小腿放松', purpose:'放松', how:'20-30秒。', detail:'1. 弓步站立，后脚跟踩地。\n2. 感受后腿小腿后侧拉伸。', breath:'自然。', mistakes:'后脚跟离地。', equipment:'墙/椅子' }
};

function mkEx(code, sets, reps, load){ return { code, sets, reps, dose: reps, load, ...(EXERCISES[code]||{name:code,purpose:'',how:'',breath:'',target:'',detail:'',mistakes:'',equipment:''}) }; }

function getPlanDetail(weekPhase, dow) {
  const plan = { warmup: [], exercises: [], stretch: [], tips: [] };
  plan.warmup = [mkEx('P1 90/90 呼吸', 1, '5次', '自重'), mkEx('P2 下巴轻收', 1, '8次', '自重'), mkEx('P3 墙面滑手', 1, '8次', '自重'), mkEx('S2 臂桥', 1, '8次', '自重')];
  plan.stretch = [mkEx('P1 90/90 呼吸', 1, '5次', '自重')];
  plan.tips = ['全程 RPE 4-6，不要练到力竭', '如有疼痛或不适，立刻降低幅度或停止'];

  if (dow === 1) { // 周一
    plan.label = '下肢力量+臀部塑形+骨盆控制'; plan.duration = '45-55 分钟';
    if (weekPhase === 1) plan.exercises = [mkEx('S1 椅子坐站', 2, '8次', '自重'), mkEx('S2 臂桥', 2, '8-10次', '自重'), mkEx('S3 弹力带侧向走', 1, '6步/侧', '轻阻力'), mkEx('S7 死虫式', 2, '6次/侧', '自重')];
    else if (weekPhase === 2) plan.exercises = [mkEx('S1 椅子坐站', 3, '8-10次', '自重'), mkEx('S2 臂桥', 3, '10次', '自重'), mkEx('S3 弹力带侧向走', 2, '8步/侧', '轻阻力'), mkEx('S7 死虫式', 2, '8次/侧', '自重')];
    else if (weekPhase === 3) plan.exercises = [mkEx('S1 椅子坐站', 3, '8-12次', '自重'), mkEx('S2 臂桥', 3, '10-12次', '自重'), mkEx('S3 弹力带侧向走', 2, '8-10步/侧', '轻阻力'), mkEx('S7 死虫式', 2, '3x8次/侧', '自重')];
    else plan.exercises = [mkEx('S1 椅子坐站', 3, '8-12次', '自重'), mkEx('S2 臂桥', 3, '10-15次', '自重'), mkEx('S3 弹力带侧向走', 2, '8-12步/侧', '轻阻力'), mkEx('S7 死虫式', 3, '8次/侧', '自重')];
  } else if (dow === 2) { // 周二
    plan.label = '康复控制+短版普拉提'; plan.duration = '20-30 分钟';
    plan.tips = ['不应练到疲惫', '专注呼吸和骨盆小范围控制'];
    if (weekPhase === 1) plan.exercises = [mkEx('P1 90/90 呼吸', 2, '5次', '自重'), mkEx('P2 下巴轻收', 2, '8次', '自重'), mkEx('P3 墙面滑手', 1, '8次', '自重'), mkEx('P4 仰卧脚跟滑动', 1, '6次/侧', '自重')];
    else if (weekPhase === 2) plan.exercises = [mkEx('P1 90/90 呼吸', 2, '5次', '自重'), mkEx('P2 下巴轻收', 2, '8次', '自重'), mkEx('P3 墙面滑手', 2, '8次', '自重'), mkEx('P4 仰卧脚跟滑动', 2, '6次/侧', '自重')];
    else plan.exercises = [mkEx('P1 90/90 呼吸', 2, '5次', '自重'), mkEx('P2 下巴轻收', 2, '8次', '自重'), mkEx('P3 墙面滑手', 2, '8次', '自重'), mkEx('P4 仰卧脚跟滑动', 2, '6次/侧', '自重')];
  } else if (dow === 3) { // 周三
    plan.label = '上肢力量+背部/肩胛线条'; plan.duration = '40-55 分钟';
    if (weekPhase === 1) plan.exercises = [mkEx('S4 划船', 2, '10次', '轻阻力'), mkEx('S5 前方高位下拉', 2, '8次', '极轻'), mkEx('S6 墙/高台俯卧撑', 2, '8次', '自重'), mkEx('P3 墙面滑手', 1, '8次', '自重')];
    else if (weekPhase === 2) plan.exercises = [mkEx('S4 划船', 3, '10次', '轻中阻力'), mkEx('S5 前方高位下拉', 2, '8-10次', '轻'), mkEx('S6 墙/高台俯卧撑', 2, '8-10次', '自重'), mkEx('S4 划船', 1, '10次', '极轻阻力')];
    else plan.exercises = [mkEx('S4 划船', 3, '8-12次', '轻中阻力'), mkEx('S5 前方高位下拉', 2, '3x8-12次', '轻'), mkEx('S6 墙/高台俯卧撑', 3, '8-12次', '自重'), mkEx('S4 划船', 2, '10次', '极轻阻力')];
  } else if (dow === 4) { // 周四
    plan.label = '恢复+温和普拉提'; plan.duration = '20-30 分钟';
    plan.tips = ['步行作为主动恢复', '保持放松'];
    if (weekPhase === 1) plan.exercises = [mkEx('P1 90/90 呼吸', 2, '5次', '自重'), mkEx('P5 小范围骨盆控制', 1, '5次', '自重'), mkEx('散步', 1, '10-15分钟', '自重')];
    else if (weekPhase === 2) plan.exercises = [mkEx('P1 90/90 呼吸', 2, '5次', '自重'), mkEx('P4 仰卧脚跟滑动', 1, '6次/侧', '自重'), mkEx('散步', 1, '15-20分钟', '自重')];
    else plan.exercises = [mkEx('P1 90/90 呼吸', 2, '5次', '自重'), mkEx('P4 仰卧脚跟滑动', 1, '6次/侧', '自重'), mkEx('散步', 1, '15-25分钟', '自重')];
  } else if (dow === 5) { // 周五
    plan.label = '全身功能性力量'; plan.duration = '40-55 分钟';
    if (weekPhase === 1) plan.exercises = [mkEx('S1 椅子坐站', 2, '8次', '自重'), mkEx('S4 划船', 2, '8次', '轻阻力'), mkEx('S8 Pallof 抗旋转', 2, '8次/侧', '轻阻力')];
    else if (weekPhase === 2) plan.exercises = [mkEx('S1 椅子坐站', 2, '10次', '自重'), mkEx('S4 划船', 2, '3x8次', '轻阻力'), mkEx('S8 Pallof 抗旋转', 2, '10次/侧', '轻阻力'), mkEx('S10 农夫行走', 1, '20秒', '1-3kg')];
    else plan.exercises = [mkEx('S1 椅子坐站', 2, '10次', '自重'), mkEx('S9 分腿蹲', 2, '6次/侧', '自重'), mkEx('S4 划船', 3, '8次', '轻阻力'), mkEx('S8 Pallof 抗旋转', 2, '10次/侧', '轻阻力')];
  } else if (dow === 6) { // 周六
    plan.label = '游泳技能学习'; plan.duration = '40-60 分钟';
    plan.tips = ['一定要在有救生员的泳池', '绝对不独自在深水区练漂浮', '前 4-8 次最好请教练'];
    plan.exercises = [mkEx('游泳', 1, '40-60分钟', '水中适应、缓慢吐气、扶池边练习')];
  } else if (dow === 0) { // 周日
    plan.label = '完全休息'; plan.duration = '—'; plan.isTraining = false; plan.exercises = []; plan.tips = ['完全休息', '保证睡眠'];
  }
  return plan;
}

function buildPlanByContent(weekPhase, dow) {
  const plan = getPlanDetail(weekPhase, dow);
  return { key: 'd' + dow, label: plan.label, icon: plan.isTraining === false ? '😴' : '✅', duration: plan.duration, summary: plan.label, isTraining: plan.isTraining !== false, warmup: plan.warmup || [], stretch: plan.stretch || [], exercises: plan.exercises || [], tips: plan.tips || [] };
}

function currentPlan() {
  const planInfo = getPlanDayInfo(new Date());
  const ov = getOverride(new Date());
  
  if (ov === 'leave' || ov === 'period' || ov === 'rest') return buildRestPlan(ov);
  if (ov === 'swim') return { key:'swim', label:'游泳代替今日计划', icon:'🏊', duration:'40–60 分钟', summary:'游泳代替今日计划 · 原训练顺延', isTraining:true, warmup:[], stretch:[mkEx('胸肌拉伸',1,'20–30秒','自重')], exercises:[mkEx('游泳',4,'25m','自由'), mkEx('自由泳',4,'25m','单臂')], tips:['以水感与技术为主'] };
  if (ov === 'reduced'){ const base = buildPlanByContent(1, 1); return { ...base, label:'身体不适（减量）', icon:'🔻', duration:'20–30 分钟', summary:'减量 · 组数减少', exercises: base.exercises.map(x=>({...x,sets:Math.max(1,x.sets-1)})) }; }
  
  if (!planInfo) return buildRestPlan('rest');
  
  let weekPhase = 1;
  const totalWeeks = Math.floor(planInfo.effective / 7) + 1;
  if (totalWeeks <= 4) weekPhase = 1;
  else if (totalWeeks <= 8) weekPhase = 2;
  else if (totalWeeks <= 12) weekPhase = 3;
  else weekPhase = 4; 

  const dow = (planInfo.dayOfWeek === 6) ? 0 : planInfo.dayOfWeek + 1;
  const plan = buildPlanByContent(weekPhase, dow);
  plan.weekLabel = `第 ${totalWeeks} 周`;
  return plan;
}
