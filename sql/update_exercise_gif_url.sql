-- =============================================
-- 批量更新 exercise_library 表的 gif_url
-- MinIO 路径: fitness-ai-backend/exercise-gifs/{文件名}.mp4
-- 请确保已将对应视频文件上传到 MinIO 后再执行
-- =============================================

-- === 热身动作 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/开合跳.mp4' WHERE name = '开合跳';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/高抬腿.mp4' WHERE name = '高抬腿';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/臀桥.mp4' WHERE name = '臀桥';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/动态拉伸.mp4' WHERE name = '动态拉伸';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/肩绕环.mp4' WHERE name = '肩绕环';

-- === 胸部力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/俯卧撑.mp4' WHERE name = '俯卧撑';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/上斜俯卧撑.mp4' WHERE name = '上斜俯卧撑';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/杠铃卧推.mp4' WHERE name = '杠铃卧推';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/哑铃卧推.mp4' WHERE name = '哑铃卧推';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/哑铃飞鸟.mp4' WHERE name = '哑铃飞鸟';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/上斜哑铃卧推.mp4' WHERE name = '上斜哑铃卧推';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/双杠臂屈伸.mp4' WHERE name = '双杠臂屈伸';

-- === 背部力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/引体向上.mp4' WHERE name = '引体向上';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/反手引体向上.mp4' WHERE name = '反手引体向上';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/杠铃划船.mp4' WHERE name = '杠铃划船';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/哑铃划船.mp4' WHERE name = '哑铃划船';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/坐姿下拉.mp4' WHERE name = '坐姿下拉';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/直臂下压.mp4' WHERE name = '直臂下压';

-- === 腿部力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/深蹲.mp4' WHERE name = '深蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/杠铃深蹲.mp4' WHERE name = '杠铃深蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/哑铃深蹲.mp4' WHERE name = '哑铃深蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/保加利亚分腿蹲.mp4' WHERE name = '保加利亚分腿蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/箭步蹲.mp4' WHERE name = '箭步蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/腿举.mp4' WHERE name = '腿举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/硬拉.mp4' WHERE name = '硬拉';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/罗马尼亚硬拉.mp4' WHERE name = '罗马尼亚硬拉';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/腿弯举.mp4' WHERE name = '腿弯举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/小腿提踵.mp4' WHERE name = '小腿提踵';

-- === 肩部力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/哑铃推举.mp4' WHERE name = '哑铃推举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/杠铃推举.mp4' WHERE name = '杠铃推举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/哑铃侧平举.mp4' WHERE name = '哑铃侧平举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/哑铃前平举.mp4' WHERE name = '哑铃前平举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/俯身飞鸟.mp4' WHERE name = '俯身飞鸟';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/面拉.mp4' WHERE name = '面拉';

-- === 手臂力量 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/哑铃弯举.mp4' WHERE name = '哑铃弯举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/杠铃弯举.mp4' WHERE name = '杠铃弯举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/锤式弯举.mp4' WHERE name = '锤式弯举';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/三头下压.mp4' WHERE name = '三头下压';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/颈后臂屈伸.mp4' WHERE name = '颈后臂屈伸';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/窄距俯卧撑.mp4' WHERE name = '窄距俯卧撑';

-- === 核心训练 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/平板支撑.mp4' WHERE name = '平板支撑';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/卷腹.mp4' WHERE name = '卷腹';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/俄罗斯转体.mp4' WHERE name = '俄罗斯转体';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/登山跑.mp4' WHERE name = '登山跑';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/死虫式.mp4' WHERE name = '死虫式';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/悬垂举腿.mp4' WHERE name = '悬垂举腿';

-- === 有氧运动 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/波比跳.mp4' WHERE name = '波比跳';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/跳绳.mp4' WHERE name = '跳绳';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/开合跳蹲.mp4' WHERE name = '开合跳蹲';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/原地慢跑.mp4' WHERE name = '原地慢跑';

-- === 拉伸/放松 ===
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/站立体前屈.mp4' WHERE name = '站立体前屈';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/股四头拉伸.mp4' WHERE name = '股四头拉伸';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/鸽子式.mp4' WHERE name = '鸽子式';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/猫牛式.mp4' WHERE name = '猫牛式';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/婴儿式.mp4' WHERE name = '婴儿式';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/仰卧脊柱扭转.mp4' WHERE name = '仰卧脊柱扭转';
UPDATE exercise_library SET gif_url = 'http://192.168.234.100:9000/fitness-ai-backend/exercise-gifs/泡沫轴放松.mp4' WHERE name = '泡沫轴放松';
