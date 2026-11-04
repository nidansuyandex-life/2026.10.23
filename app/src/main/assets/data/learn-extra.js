// ============ 表达训练 + 英语语法题库（功能题库，供每日轮换） ============
window.EXPRESS_PRE = [
  { q:'你如何看待"失败"？它给你带来了什么？', guide:'结合自己的经历，从认识失败、从失败学到的、以后怎么做三方面回答。' },
  { q:'介绍一个你最近在坚持的习惯，它带来了什么改变？', guide:'先说习惯是什么（P），再说它带来的两个具体改变（E），最后说说你会不会继续（L）。' },
  { q:'如果只能给刚入职的自己一条建议，你会说什么？为什么？', guide:'观点（P）→ 这条建议为什么重要，举一个具体场景（E）→ 总结（L）。' },
  { q:'你觉得"沟通能力"和"专业能力"哪个更重要？', guide:'先亮观点，再给 1-2 个理由或例子，注意别两边都说"都重要"。' },
  { q:'描述一件最近让你有成就感的小事。', guide:'P：是什么事；E：难在哪、你怎么解决的；L：它说明你什么能力。' },
  { q:'你如何安排一天中最重要的三件事？', guide:'P：你的方法一句话；E：具体怎么排序、为什么；L：这样安排的好处。' },
  { q:'有人说"自律才自由"，你同意吗？', guide:'观点明确（同意/不同意/有条件同意）→ 给理由和自身例子 → 收尾。' },
  { q:'介绍一部对你影响最大的书或电影。', guide:'P：是什么；E：核心讲了什么、哪一点打动了你；L：它改变了你什么。' },
  { q:'当你和同事意见不合时，你通常怎么处理？', guide:'P：你的原则一句话；E：具体一次经历，怎么说的、结果如何；L：总结你的沟通方式。' },
  { q:'如果给你一周完全自由的时间，你会怎么过？', guide:'P：一句话计划；E：为什么这样安排，想做而没做的事；L：这样的时间对你的意义。' },
  { q:'你觉得自己的优势是什么？举一个例子证明。', guide:'P：优势一句话；E：一个具体事例（背景-做法-结果）；L：这个优势适合用在哪。' },
  { q:'最近有什么让你焦虑的事吗？你是怎么应对的？', guide:'P：事情是什么；E：你的应对动作（具体做了什么）；L：效果和感悟。' }
];
window.EXPRESS_IMPROV = [
  '如果可以穿越回十年前，你会对那时的自己说什么？',
  '描述你理想中完美的一个周末。',
  '手机里你最常用、最离不开的一个 APP 是什么？为什么？',
  '如果你现在有三天假期，不考虑钱，你会去哪、做什么？',
  '说一个你最近学到的新知识或新词，用最简单的话讲明白。',
  '如果让你教别人一项你的技能，你会教什么？第一课讲什么？',
  '你最近一次开怀大笑是因为什么？',
  '描述你房间里的一个对你有特殊意义的物件。',
  '如果明天下雨，你原本的计划被打乱，你会怎么安排这一天？',
  '你会推荐别人养成什么小习惯？',
  '说一件你小时候特别害怕、现在不怕了的事。',
  '如果请你给今天的自己起一个形容词，会是什么？为什么？'
];
window.EXPRESS_UPGRADE = [
  { cat:'开放表达', icon:'🔓', color:'#2563EB', desc:'敢于开口，把话说出来', topics:[
    { q:'用一分钟做一次自我介绍（名字之外，说两个特点）。', guide:'别背简历：说特点 + 一个小例子 + 你希望别人记住你什么。' },
    { q:'在小组讨论里，你怎么开口说出第一个观点？', guide:'练开场句式：「我先说个初步想法……」把不完美当起点。' },
    { q:'向陌生人请教一个问题，你会怎么开头？', guide:'练礼貌 + 直接：说明身份 → 问题 → 为什么问对方。' }
  ]},
  { cat:'注意逻辑', icon:'🧩', color:'#0E9F8A', desc:'说清因果，别绕圈子', topics:[
    { q:'解释一下你的一天是怎么安排的，用"第一、第二、第三"。', guide:'只用三点，每点一句话，练"结构感"。' },
    { q:'你觉得上班路上时间长的原因有哪些？', guide:'练归纳：先总述（2-3个原因），再各展开一句。' },
    { q:'向别人解释一个流程（比如做一顿饭），要求别人听完能照做。', guide:'步骤编号 + 每步一个动作 + 最后说结果怎么判断。' }
  ]},
  { cat:'优雅表达', icon:'🦢', desc:'措辞得体，有分寸', topics:[
    { q:'同事方案做得不好，你怎么委婉提意见？', guide:'先肯定具体一处 → 用「如果……会不会更好」句式 → 落到事不对人。' },
    { q:'别人帮了你，但你发现有个疏漏，怎么提醒？', guide:'练缓冲句式：「整体特别好，有一个小地方想跟你确认下……」' },
    { q:'你需要拒绝一个不合理的请求，怎么说不伤人？', guide:'共情 + 明确拒绝 + 替代方案，三步各一句。' }
  ]},
  { cat:'情感表达', icon:'💗', color:'#E0537A', desc:'真诚地表达感受', topics:[
    { q:'向家人表达一次感谢，你会怎么说？', guide:'具体事件 + 感受 + 影响，避免只说"谢谢"。' },
    { q:'朋友情绪低落来找你倾诉，你怎么回应？', guide:'先接情绪（我理解/这确实难受）再问需要什么，别急着给建议。' },
    { q:'描述一次你被感动/被暖到的瞬间。', guide:'时间地点 + 别人做了什么 + 你的感受，细节越具体越动人。' }
  ]}
];
window.EXPRESS_STORY = [
  { q:'一次让我成长的决定', hint:'什么决定 → 当时的纠结 → 现在回头看' },
  { q:'一件我搞砸了但学到很多的事', hint:'事情经过 → 哪里出了问题 → 之后怎么改' },
  { q:'一个人对我说过的最重要的一句话', hint:'场景 → 原话 → 为什么记到现在' },
  { q:'我第一次独立完成的一件事', hint:'什么事 → 遇到什么困难 → 完成后的感受' },
  { q:'一个改变了我的习惯的人或物', hint:'是什么 → 怎么开始影响我 → 现在的样子' },
  { q:'一次尴尬到脚趾抠地的经历', hint:'现场还原 → 当时反应 → 现在怎么看' },
  { q:'我最自豪的一件小事', hint:'事情 → 别人可能觉得小、为什么对你大 → 它代表什么' },
  { q:'如果把我这一年拍成电影，片名和预告片会是？', hint:'片名 → 三个镜头（起承转合）→ 结尾定格' }
];
window.EXPRESS_CHECK = [
  '先说结论，再展开，没有绕圈子',
  '有具体例子，不是空说道理',
  '句子简短，没有大段"然后然后"',
  '说完了有收尾，不是戛然而止',
  '语速稳，没有一直"嗯、呃"',
  '注意到了对方的反应并做了调整'
];
window.GRAMMAR_QUIZ = [
  { q:'She ___ to work by bus every day.', o:['go','goes','going','gone'], a:1, why:'一般现在时，第三人称单数动词加 -es。' },
  { q:'I ___ my homework before dinner yesterday.', o:['finish','finishes','finished','finishing'], a:2, why:'yesterday 是过去时间标志，用过去式 finished。' },
  { q:'There ___ many people in the park last Sunday.', o:['was','were','is','are'], a:1, why:'many people 是复数，且 last Sunday 是过去时，用 were。' },
  { q:'He is good ___ playing the guitar.', o:['at','in','on','for'], a:0, why:'be good at (doing) sth 是固定搭配。' },
  { q:'This book is ___ than that one.', o:['interesting','more interesting','most interesting','interestinger'], a:1, why:'多音节形容词比较级用 more + 原级。' },
  { q:'I have lived here ___ 2019.', o:['for','since','from','at'], a:1, why:'since + 时间点（2019），for + 时间段。' },
  { q:'___ you like some coffee?', o:['Could','Do','Are','Would'], a:3, why:'Would you like...? 是礼貌地提供/邀请的固定句型。' },
  { q:'If it ___ tomorrow, we will stay at home.', o:['will rain','rains','rained','rain'], a:1, why:'第一条件句：if 从句用一般现在时表将来。' },
  { q:'She ___ finish the report by 6 pm.', o:['will','is','does','has'], a:0, why:'一般将来时 will + 动词原形。' },
  { q:'I enjoy ___ music in my free time.', o:['listen to','listening to','to listen','listened'], a:1, why:'enjoy 后接动名词 doing。' },
  { q:'Neither of the answers ___ correct.', o:['are','is','were','be'], a:1, why:'neither of + 复数名词作主语时，谓语用单数。' },
  { q:'He asked me where I ___.', o:['live','lived','do live','am living'], a:1, why:'宾语从句主句是过去时，从句时态后移，live → lived。' },
  { q:'The meeting ___ until 5 pm yesterday.', o:['lasted','is lasting','last','lasts'], a:0, why:'yesterday 过去时间，用过去式 lasted。' },
  { q:'You had better ___ a doctor.', o:['to see','seeing','see','saw'], a:2, why:'had better 后接动词原形。' },
  { q:'This is the ___ movie I have ever seen.', o:['good','better','best','most good'], a:2, why:'ever seen 表"到目前为止最"，用最高级 best。' },
  { q:'By the time we arrived, the train ___.', o:['left','has left','had left','leaves'], a:2, why:'过去的过去用过去完成时 had left。' },
  { q:'Neither Tom nor his parents ___ at home.', o:['is','are','was','be'], a:1, why:'neither...nor 就近原则，his parents 是复数，用 are。' },
  { q:'I would rather ___ at home tonight.', o:['to stay','staying','stay','stayed'], a:2, why:'would rather 后接动词原形。' },
  { q:'Look! The children ___ on the playground.', o:['play','played','are playing','were playing'], a:2, why:'Look! 提示动作正在进行，用现在进行时。' },
  { q:'It is no use ___ about it now.', o:['to complain','complaining','complain','complained'], a:1, why:'It is no use doing sth 是固定句型。' }
];
