/* 养成 · 星光小铺数据
 * 想加新东西：往下面数组里加一条即可，index.html 会自动渲染（房间 / 商店 / 衣柜都读这里）
 * kind: furniture 家具家电 | clothes 衣服 | story 故事书
 * 照片物品: img=assets/img/room/ 下的 png 名, pw=房间内显示宽, x=左边缘, by=底边 y（房间画布 900x320，墙 y<220，地板 220~320）
 * 插画物品: draw=SVG 片段, pos=[x,y] 左上角坐标
 * z:    图层，-1 地毯，0 墙面，1 地板家具，2 人，3 前景小物
 * group: 同类升级款，拥有多个时只画最贵的那个（如 蓬松大床 > 小木床）
 * needs: 需要先拥有某件
 * slot/style: 衣服部位 head/body/neck/face 与画法
 */
window.GROW_SHOP = [
  /* ================= 家具（实拍照片款） ================= */
  { id:'bed', kind:'furniture', group:'bed', icon:'🛏️', name:'小木床', price:20, cat:'家具', desc:'终于不用睡地板了', img:'bed.png', pw:240, x:8, by:304 },
  { id:'bigbed', kind:'furniture', group:'bed', icon:'🛌', name:'蓬松大床', price:46, cat:'家具', desc:'怎么滚都不会掉下去', img:'bigbed.png', pw:205, x:8, by:308 },
  { id:'table', kind:'furniture', group:'table', icon:'🪑', name:'小桌椅', price:22, cat:'家具', desc:'可以坐这儿发呆', img:'table.png', pw:150, x:296, by:308 },
  { id:'desk', kind:'furniture', group:'table', icon:'💻', name:'书桌', price:40, cat:'家具', desc:'有电脑的那种书桌', img:'desk.png', pw:190, x:292, by:290 },
  { id:'nightstand', kind:'furniture', icon:'🗄️', name:'床头柜', price:18, cat:'家具', desc:'放杯水和小书', img:'nightstand.png', pw:90, x:238, by:284 },
  { id:'shelf', kind:'furniture', icon:'📚', name:'小书架', price:25, cat:'家具', desc:'慢慢把书填满', img:'shelf.png', pw:148, x:6, by:252 },
  { id:'wardrobe', kind:'furniture', icon:'🚪', name:'双门衣柜', price:44, cat:'家具', desc:'衣服终于有家了', img:'wardrobe.png', pw:140, x:168, by:250 },
  { id:'mirror', kind:'furniture', icon:'🪞', name:'穿衣镜', price:24, cat:'家具', desc:'出门前看一眼自己', img:'mirror.png', pw:72, x:314, by:252 },
  { id:'lamp', kind:'furniture', group:'lamp', icon:'💡', name:'落地小灯', price:14, cat:'家具', desc:'晚上留一盏给自己', img:'lamp.png', pw:66, x:394, by:256 },
  { id:'projector', kind:'furniture', group:'lamp', icon:'✨', name:'星空投影灯', price:42, cat:'家具', desc:'把天花板变成夜空', img:'projector.png', pw:64, x:394, by:252 },
  { id:'guitar', kind:'furniture', icon:'🎸', name:'木吉他', price:38, cat:'家具', desc:'弹不好但很好看', img:'guitar.png', pw:80, x:96, by:292 },
  { id:'vacuum', kind:'furniture', icon:'🧹', name:'吸尘器', price:28, cat:'家具', desc:'房间干净心情就好', img:'vacuum.png', pw:84, x:6, by:290 },
  { id:'plant', kind:'furniture', icon:'🪴', name:'绿植盆栽', price:10, cat:'家具', desc:'房间里有点生气', img:'plant.png', pw:100, x:760, by:288 },

  /* ================= 家电（实拍照片款） ================= */
  { id:'tv', kind:'furniture', icon:'📺', name:'电视柜套装', price:58, cat:'家电', desc:'窝在沙发看片的快乐', img:'tv.png', pw:215, x:508, by:294 },
  { id:'fridge', kind:'furniture', icon:'🧊', name:'复古小冰箱', price:45, cat:'家电', desc:'冰西瓜和酸奶都在里面', img:'fridge.png', pw:100, x:700, by:308 },
  { id:'washer', kind:'furniture', icon:'🌀', name:'滚筒洗衣机', price:40, cat:'家电', desc:'把生活洗得干干净净', img:'washer.png', pw:104, x:588, by:306 },
  { id:'ac', kind:'furniture', icon:'❄️', name:'壁挂空调', price:36, cat:'家电', desc:'夏天救命冬天续命', img:'ac.png', pw:170, x:700, by:160, z:0 },
  { id:'microwave', kind:'furniture', icon:'🍱', name:'微波炉', price:26, cat:'家电', desc:'热剩饭也需要仪式感', img:'microwave.png', pw:100, x:750, by:302 },
  { id:'coffee', kind:'furniture', icon:'☕', name:'复古咖啡机', price:22, cat:'家电', desc:'早上有咖啡香了', img:'coffee.png', pw:62, x:518, by:254 },
  { id:'fan', kind:'furniture', icon:'🍃', name:'薄荷小风扇', price:16, cat:'家电', desc:'呼呼吹走坏心情', img:'fan.png', pw:64, x:648, by:258 },
  { id:'humidifier', kind:'furniture', icon:'💧', name:'加湿器', price:18, cat:'家电', desc:'干燥季节的小云朵', img:'humidifier.png', pw:60, x:584, by:256 },
  { id:'speaker', kind:'furniture', icon:'🔊', name:'蓝牙音箱', price:20, cat:'家电', desc:'房间里要有音乐', img:'speaker.png', pw:52, x:462, by:252 },

  /* ================= 沙发区（实拍照片款） ================= */
  { id:'beanbag', kind:'furniture', group:'sofa', icon:'🫧', name:'懒人沙发', price:18, cat:'家具', desc:'陷进去就不想起来', img:'beanbag.png', pw:130, x:296, by:298 },
  { id:'armchair', kind:'furniture', group:'sofa', icon:'🪑', name:'米色扶手椅', price:36, cat:'家具', desc:'一个人的好位置', img:'armchair.png', pw:160, x:298, by:300 },
  { id:'sofa', kind:'furniture', group:'sofa', icon:'🛋️', name:'蓝色布艺沙发', price:66, cat:'家具', desc:'可以窝一下午的那种', img:'sofa.png', pw:270, x:298, by:312 },

  /* ================= 宠物与小物（实拍照片款） ================= */
  { id:'aquarium', kind:'furniture', icon:'🐠', name:'小鱼缸', price:24, cat:'家具', desc:'看鱼发呆最治愈', img:'aquarium.png', pw:105, x:790, by:296, z:3 },
  { id:'catbed', kind:'furniture', icon:'🐈', name:'猫咪小窝', price:28, cat:'家具', desc:'它自己选的地方才是家', img:'catbed.png', pw:100, x:560, by:314, z:3 },

  /* ================= 墙面与角落（插画款） ================= */
  { id:'rug', kind:'furniture', group:'rug', icon:'🧶', name:'圆地毯', price:12, cat:'装饰', desc:'光脚踩上去软软的', pos:[380,246], z:-1,
    draw:`<ellipse cx="76" cy="22" rx="76" ry="23" fill="#FFD9C7"/>
<ellipse cx="76" cy="22" rx="54" ry="15" fill="#FFC7B0"/>` },
  { id:'bigrug', kind:'furniture', group:'rug', icon:'🟠', name:'羊毛大地毯', price:30, cat:'装饰', desc:'整个房间都暖起来', pos:[360,242], z:-1,
    draw:`<ellipse cx="90" cy="26" rx="90" ry="27" fill="#F9DCC4"/>
<ellipse cx="90" cy="26" rx="70" ry="20" fill="#F6CDB0"/>
<ellipse cx="90" cy="26" rx="42" ry="11" fill="#F9DCC4"/>` },
  { id:'poster', kind:'furniture', group:'poster', icon:'🖼️', name:'墙上贴画', price:10, cat:'装饰', desc:'自己挑的那一幅', pos:[76,48], z:0,
    draw:`<rect x="0" y="0" width="54" height="66" rx="4" fill="#FFFFFF" stroke="#E3D9C9" stroke-width="2"/>
<path d="M6 54 L22 28 L34 46 L42 36 L48 54 Z" fill="#A9C8F5"/>
<circle cx="36" cy="16" r="6" fill="#FFD27A"/>` },
  { id:'gallery', kind:'furniture', group:'poster', icon:'🖼️', name:'照片墙', price:34, cat:'装饰', desc:'三幅画，一面小美术馆', pos:[64,42], z:0,
    draw:`<rect x="0" y="0" width="26" height="34" rx="3" fill="#FFFFFF" stroke="#E3D9C9" stroke-width="2"/>
<path d="M4 26 L12 14 L20 26 Z" fill="#A9C8F5"/>
<rect x="30" y="8" width="24" height="30" rx="3" fill="#FFFFFF" stroke="#E3D9C9" stroke-width="2"/>
<circle cx="42" cy="20" r="6" fill="#F9C4D2"/>
<rect x="10" y="40" width="28" height="32" rx="3" fill="#FFFFFF" stroke="#E3D9C9" stroke-width="2"/>
<path d="M14 66 L24 48 L34 66 Z" fill="#FFD27A"/>` },
  { id:'photos', kind:'furniture', icon:'📷', name:'拍立得照片', price:14, cat:'装饰', desc:'随手贴几张', pos:[352,58], z:0,
    draw:`<g transform="rotate(-6 12 16)"><rect x="0" y="0" width="22" height="26" rx="2" fill="#FFFFFF" stroke="#E3D9C9"/><rect x="3" y="3" width="16" height="13" fill="#CFE0FA"/></g>
<g transform="rotate(4 40 12)"><rect x="28" y="4" width="22" height="26" rx="2" fill="#FFFFFF" stroke="#E3D9C9"/><rect x="31" y="7" width="16" height="13" fill="#F9C4D2"/></g>
<g transform="rotate(-2 22 40)"><rect x="12" y="30" width="24" height="16" rx="2" fill="#FFFFFF" stroke="#E3D9C9"/><rect x="15" y="33" width="18" height="7" fill="#FFE9A8"/></g>` },
  { id:'tapestry', kind:'furniture', icon:'🧵', name:'挂毯', price:16, cat:'装饰', desc:'把墙变得柔软一点', pos:[560,58], z:0,
    draw:`<rect x="10" y="0" width="4" height="8" fill="#C9A87C"/><rect x="28" y="0" width="4" height="8" fill="#C9A87C"/>
<path d="M0 8 h42 v34 l-21 6 l-21 -6 Z" fill="#CFE0FA"/>
<path d="M0 8 h42 v7 h-42 Z" fill="#A9C8F5"/>
<circle cx="21" cy="27" r="7" fill="#F9C4D2"/>` },
  { id:'clock', kind:'furniture', icon:'🕰️', name:'挂钟', price:14, cat:'装饰', desc:'让时间有个样子', pos:[256,116], z:0,
    draw:`<circle cx="16" cy="16" r="15" fill="#FFFFFF" stroke="#D6DEEB" stroke-width="2.5"/>
<path d="M16 16 V7 M16 16 L23 20" stroke="#5B6B85" stroke-width="2" stroke-linecap="round"/>
<circle cx="16" cy="16" r="1.6" fill="#5B6B85"/>` },
  { id:'map', kind:'furniture', icon:'🗺️', name:'世界地图', price:12, cat:'装饰', desc:'提醒自己世界很大', pos:[16,120], z:0,
    draw:`<rect x="0" y="0" width="40" height="30" rx="2" fill="#EAF7F3" stroke="#CFE8DE"/>
<path d="M6 9 q8 -4 12 3 t10 2" stroke="#8FD6B0" stroke-width="2.4" fill="none"/>
<circle cx="12" cy="21" r="3" fill="#8FD6B0"/>
<circle cx="29" cy="11" r="2.4" fill="#8FD6B0"/>` },
  { id:'string', kind:'furniture', icon:'🎐', name:'串灯', price:14, cat:'装饰', desc:'一串小暖光', pos:[0,12], z:0,
    draw:`<path d="M0 8 Q60 24 120 10 Q180 24 238 8" stroke="#C9B79C" stroke-width="1.6" fill="none"/>
<g fill="#FFDF8A"><circle cx="24" cy="15" r="3"/><circle cx="56" cy="19" r="3"/><circle cx="88" cy="17" r="3"/><circle cx="120" cy="11" r="3"/><circle cx="152" cy="17" r="3"/><circle cx="184" cy="19" r="3"/><circle cx="216" cy="14" r="3"/></g>` },
  { id:'string2', kind:'furniture', icon:'🎐', name:'右墙串灯', price:14, cat:'装饰', desc:'墙上再多一点光', pos:[402,14], z:0,
    draw:`<path d="M0 8 Q60 24 120 10 Q180 24 238 8" stroke="#C9B79C" stroke-width="1.6" fill="none"/>
<g fill="#FFDF8A"><circle cx="24" cy="15" r="3"/><circle cx="56" cy="19" r="3"/><circle cx="88" cy="17" r="3"/><circle cx="120" cy="11" r="3"/><circle cx="152" cy="17" r="3"/><circle cx="184" cy="19" r="3"/><circle cx="216" cy="14" r="3"/></g>` },
  { id:'hungplant', kind:'furniture', icon:'🌿', name:'吊兰', price:12, cat:'装饰', desc:'从天花板垂下来', pos:[660,20], z:0,
    draw:`<path d="M14 0 v8" stroke="#C9A87C" stroke-width="1.6"/>
<path d="M6 8 h16 l-3 12 h-10 Z" fill="#E9A97D"/>
<path d="M14 20 Q8 30 4 36" stroke="#5FBF90" stroke-width="2" fill="none"/>
<path d="M14 20 Q20 30 24 34" stroke="#4CAF82" stroke-width="2" fill="none"/>
<ellipse cx="4" cy="38" rx="4" ry="3" fill="#5FBF90"/>
<ellipse cx="25" cy="36" rx="4" ry="3" fill="#4CAF82"/>` },
  { id:'flower', kind:'furniture', icon:'🌷', name:'窗台花瓶', price:8, cat:'装饰', desc:'摆一点颜色进来', pos:[622,126], z:0,
    draw:`<path d="M0 14 h18 l-3 14 h-12 Z" fill="#9EC8F0"/>
<path d="M9 14 Q9 4 9 0" stroke="#5FBF90" stroke-width="2" fill="none"/>
<circle cx="9" cy="-2" r="4.5" fill="#F472B6"/>
<circle cx="3" cy="3" r="3.6" fill="#F9A8C2"/>
<circle cx="15" cy="3" r="3.6" fill="#F9A8C2"/>` },
  { id:'curtain', kind:'furniture', icon:'🪟', name:'窗帘', price:16, cat:'装饰', desc:'风一吹会动', pos:[434,26], z:0,
    draw:`<rect x="0" y="0" width="16" height="152" rx="7" fill="#F9C4D2"/>
<rect x="152" y="0" width="16" height="152" rx="7" fill="#F9C4D2"/>
<path d="M8 6 v138 M160 6 v138" stroke="#F0A8BC" stroke-width="1.4"/>` },
  { id:'candle', kind:'furniture', icon:'🕯️', name:'香薰蜡烛', price:10, cat:'装饰', desc:'点起来是安心的味道', pos:[474,214], z:1,
    draw:`<rect x="0" y="6" width="20" height="18" rx="4" fill="#F6E3CE"/>
<path d="M10 6 V-2" stroke="#8A6D4F" stroke-width="1.6"/>
<ellipse cx="10" cy="-5" rx="2.6" ry="4" fill="#FFC94D"/>
<ellipse cx="10" cy="-4" rx="1.3" ry="2.2" fill="#FFF2C2"/>` },
  { id:'windchime', kind:'furniture', icon:'🎐', name:'风铃', price:12, cat:'装饰', desc:'风来的时候叮叮响', pos:[196,24], z:0,
    draw:`<path d="M14 0 V10" stroke="#C9A87C" stroke-width="1.4"/>
<path d="M6 10 h16 l-3 8 h-10 Z" fill="#D9B88C"/>
<path d="M14 18 V28 M8 18 V24 M20 18 V24" stroke="#C9A87C" stroke-width="1.2"/>
<g fill="#9EC8F0"><circle cx="14" cy="30" r="3"/><circle cx="8" cy="26" r="2.2"/><circle cx="20" cy="26" r="2.2"/></g>` },
  { id:'trophy', kind:'furniture', icon:'🏆', name:'小奖杯', price:15, cat:'装饰', desc:'送给自己的一座小山', pos:[280,240], z:1,
    draw:`<path d="M4 4 h16 v8 a8 8 0 0 1 -16 0 Z" fill="#F5C542"/>
<path d="M4 6 q-6 0 -6 4 q0 5 7 5 M20 6 q6 0 6 4 q0 5 -7 5" stroke="#E3A81E" stroke-width="2" fill="none"/>
<rect x="9" y="20" width="6" height="5" fill="#E3A81E"/>
<rect x="5" y="25" width="14" height="4" rx="2" fill="#D9A520"/>
<circle cx="12" cy="11" r="2.4" fill="#FFE9A8"/>` },
  { id:'yogamat', kind:'furniture', icon:'🧘', name:'瑜伽垫', price:14, cat:'装饰', desc:'偶尔也要动一动', pos:[186,266], z:1,
    draw:`<ellipse cx="26" cy="16" rx="26" ry="8" fill="#F9C4D2"/>
<rect x="0" y="8" width="52" height="8" rx="4" fill="#F7AEC4"/>
<ellipse cx="26" cy="8" rx="26" ry="8" fill="#FBD7E2"/>
<circle cx="26" cy="8" r="4" fill="#E9849F"/>` },
  { id:'teddy', kind:'furniture', icon:'🧸', name:'毛绒玩具', price:15, cat:'装饰', desc:'一直想要的那只', pos:[206,242], z:1,
    draw:`<circle cx="6" cy="4" r="4" fill="#C99A6A"/>
<circle cx="18" cy="4" r="4" fill="#C99A6A"/>
<circle cx="12" cy="11" r="9" fill="#DDB183"/>
<ellipse cx="12" cy="23" rx="8" ry="7" fill="#DDB183"/>
<circle cx="12" cy="13.5" r="3.4" fill="#F3DDC2"/>
<circle cx="9" cy="9" r="1.3" fill="#5B4636"/>
<circle cx="15" cy="9" r="1.3" fill="#5B4636"/>` },
  { id:'slippers', kind:'furniture', icon:'🩴', name:'小拖鞋', price:6, cat:'装饰', desc:'回家第一件事', pos:[452,306], z:3,
    draw:`<ellipse cx="6" cy="10" rx="6" ry="8" fill="#F9C4D2"/>
<ellipse cx="20" cy="10" rx="6" ry="8" fill="#F9C4D2"/>
<path d="M2 6 q4 -3 8 0" stroke="#E9849F" stroke-width="1.6" fill="none"/>
<path d="M16 6 q4 -3 8 0" stroke="#E9849F" stroke-width="1.6" fill="none"/>` },

  /* ================= 衣服 ================= */
  { id:'bow', kind:'clothes', slot:'head', style:'bow', icon:'🎀', name:'蝴蝶结', price:10, cat:'衣服', desc:'戴在头上，很小声的可爱' },
  { id:'cap', kind:'clothes', slot:'head', style:'cap', icon:'🧢', name:'棒球帽', price:14, cat:'衣服', desc:'今天想酷一点' },
  { id:'beanie', kind:'clothes', slot:'head', style:'beanie', icon:'👒', name:'毛线帽', price:14, cat:'衣服', desc:'冷了就把自己包起来' },
  { id:'beret', kind:'clothes', slot:'head', style:'beret', icon:'🎨', name:'贝雷帽', price:16, cat:'衣服', desc:'艺术家的那顶红' },
  { id:'crown', kind:'clothes', slot:'head', style:'crown', icon:'👑', name:'小皇冠', price:30, cat:'衣服', desc:'今天你就是主角' },
  { id:'glasses', kind:'clothes', slot:'face', style:'glasses', icon:'👓', name:'圆框眼镜', price:12, cat:'衣服', desc:'看起来认真又温柔' },
  { id:'sunglasses', kind:'clothes', slot:'face', style:'sunglasses', icon:'🕶️', name:'墨镜', price:15, cat:'衣服', desc:'不开心的时候戴上' },
  { id:'scarf', kind:'clothes', slot:'neck', style:'scarf', icon:'🧣', name:'围巾', price:12, cat:'衣服', desc:'出门不怕风' },
  { id:'bowtie', kind:'clothes', slot:'neck', style:'bowtie', icon:'🎩', name:'小领结', price:14, cat:'衣服', desc:'正式一点，也很可爱' },
  { id:'dress', kind:'clothes', slot:'body', style:'dress', icon:'👗', name:'小裙子', price:18, cat:'衣服', desc:'想穿就穿' },
  { id:'hoodie', kind:'clothes', slot:'body', style:'hoodie', icon:'🧥', name:'宽大卫衣', price:20, cat:'衣服', desc:'像被人抱住一整天' },
  { id:'pajamas', kind:'clothes', slot:'body', style:'pajamas', icon:'🩱', name:'睡衣', price:16, cat:'衣服', desc:'回家就换上' },
  { id:'apron', kind:'clothes', slot:'body', style:'apron', icon:'🧑‍🍳', name:'小围裙', price:18, cat:'衣服', desc:'做饭 baking 都像模像样' },

  /* ================= 故事 ================= */
  { id:'book', kind:'story', icon:'📖', name:'睡前故事书', price:12, cat:'故事', desc:'解锁每晚一个晚安故事' }
];
