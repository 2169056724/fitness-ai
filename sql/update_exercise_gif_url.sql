-- =============================================
-- 批量更新 exercise_library 表的 gif_url
-- MinIO 路径: fitness-ai-backend/exercises/{文件名}.gif
-- 请确保已将对应 GIF 文件上传到 MinIO 后再执行
-- =============================================

-- === 热身动作 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/开合跳.gif' WHERE name = '开合跳';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/高抬腿.gif' WHERE name = '高抬腿';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/臀桥.gif' WHERE name = '臀桥';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/动态拉伸.gif' WHERE name = '动态拉伸';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/肩绕环.gif' WHERE name = '肩绕环';

-- === 胸部力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/俯卧撑.gif' WHERE name = '俯卧撑';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/上斜俯卧撑.gif' WHERE name = '上斜俯卧撑';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/杠铃卧推.gif' WHERE name = '杠铃卧推';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/哑铃卧推.gif' WHERE name = '哑铃卧推';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/哑铃飞鸟.gif' WHERE name = '哑铃飞鸟';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/上斜哑铃卧推.gif' WHERE name = '上斜哑铃卧推';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/双杠臂屈伸.gif' WHERE name = '双杠臂屈伸';

-- === 背部力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/引体向上.gif' WHERE name = '引体向上';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/反手引体向上.gif' WHERE name = '反手引体向上';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/杠铃划船.gif' WHERE name = '杠铃划船';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/哑铃划船.gif' WHERE name = '哑铃划船';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/坐姿下拉.gif' WHERE name = '坐姿下拉';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/直臂下压.gif' WHERE name = '直臂下压';

-- === 腿部力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/深蹲.gif' WHERE name = '深蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/杠铃深蹲.gif' WHERE name = '杠铃深蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/哑铃深蹲.gif' WHERE name = '哑铃深蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/保加利亚分腿蹲.gif' WHERE name = '保加利亚分腿蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/箭步蹲.gif' WHERE name = '箭步蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/腿举.gif' WHERE name = '腿举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/硬拉.gif' WHERE name = '硬拉';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/罗马尼亚硬拉.gif' WHERE name = '罗马尼亚硬拉';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/腿弯举.gif' WHERE name = '腿弯举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/小腿提踵.gif' WHERE name = '小腿提踵';

-- === 肩部力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/哑铃推举.gif' WHERE name = '哑铃推举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/杠铃推举.gif' WHERE name = '杠铃推举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/哑铃侧平举.gif' WHERE name = '哑铃侧平举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/哑铃前平举.gif' WHERE name = '哑铃前平举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/俯身飞鸟.gif' WHERE name = '俯身飞鸟';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/面拉.gif' WHERE name = '面拉';

-- === 手臂力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/哑铃弯举.gif' WHERE name = '哑铃弯举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/杠铃弯举.gif' WHERE name = '杠铃弯举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/锤式弯举.gif' WHERE name = '锤式弯举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/三头下压.gif' WHERE name = '三头下压';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/颈后臂屈伸.gif' WHERE name = '颈后臂屈伸';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/窄距俯卧撑.gif' WHERE name = '窄距俯卧撑';

-- === 核心训练 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/平板支撑.gif' WHERE name = '平板支撑';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/卷腹.gif' WHERE name = '卷腹';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/俄罗斯转体.gif' WHERE name = '俄罗斯转体';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/登山跑.gif' WHERE name = '登山跑';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/死虫式.gif' WHERE name = '死虫式';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/悬垂举腿.gif' WHERE name = '悬垂举腿';

-- === 有氧运动 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/波比跳.gif' WHERE name = '波比跳';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/跳绳.gif' WHERE name = '跳绳';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/开合跳蹲.gif' WHERE name = '开合跳蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/原地慢跑.gif' WHERE name = '原地慢跑';

-- === 拉伸/放松 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/站立体前屈.gif' WHERE name = '站立体前屈';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/股四头拉伸.gif' WHERE name = '股四头拉伸';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/鸽子式.gif' WHERE name = '鸽子式';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/猫牛式.gif' WHERE name = '猫牛式';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/婴儿式.gif' WHERE name = '婴儿式';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/仰卧脊柱扭转.gif' WHERE name = '仰卧脊柱扭转';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercises/泡沫轴放松.gif' WHERE name = '泡沫轴放松';
