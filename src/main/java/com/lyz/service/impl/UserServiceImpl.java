package com.lyz.service.impl;

import com.lyz.mapper.UserMapper;
import com.lyz.mapper.UserProfileMapper;
import com.lyz.model.dto.UpdateUserBasicDTO;
import com.lyz.model.dto.WechatSessionDTO;
import com.lyz.model.entity.User;
import com.lyz.model.entity.UserProfile;
import com.lyz.model.vo.UserBasicInfoVO;
import com.lyz.model.vo.UserProfileInfoVO;
import com.lyz.model.vo.UserLoginVO;
import com.lyz.service.UserService;
import com.lyz.util.JwtUtil;
import com.lyz.util.WechatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private WechatUtil wechatUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    private final Random random = new Random();

    // 默认头像列表（可以使用开源头像服务或本地头像）
    private static final List<String> DEFAULT_AVATARS = Arrays.asList(
            "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=2",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=3",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=4",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=5",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=6",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=7",
            "https://api.dicebear.com/7.x/avataaars/svg?seed=8"
    );

    // 昵称前缀列表
    private static final List<String> NICKNAME_PREFIXES = Arrays.asList(
            "健身达人", "运动健将", "活力青年", "健康使者", "能量小子",
            "运动之星", "健身爱好者", "元气满满", "力量战士", "活力四射"
    );

    /**
     * 微信登录
     * @param code
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO wechatLogin(String code) {
        // 1. 调用微信接口获取openid
        WechatSessionDTO sessionDTO = wechatUtil.code2Session(code);
        
        if (sessionDTO.getErrcode() != null && sessionDTO.getErrcode() != 0) {
            throw new RuntimeException("微信登录失败: " + sessionDTO.getErrmsg());
        }

        String openid = sessionDTO.getOpenid();
        if (openid == null || openid.isEmpty()) {
            throw new RuntimeException("获取openid失败");
        }

        // 2. 查询用户是否存在
        User user = userMapper.selectByOpenid(openid);
        boolean isNewUser = false;


        if (user == null) {
            // 新用户，创建账号，设置随机昵称与随机头像
            isNewUser = true;
            user = new User();
            user.setOpenid(openid);
            user.setNickname(generateRandomNickname());
            user.setAvatar(generateRandomAvatar());
            userMapper.insert(user);
            log.info("新用户注册: openid={}, userId={}, nickname={}, avatar={}", 
                    openid, user.getId(), user.getNickname(), user.getAvatar());
        }else{
            //判断用户信息是否完整
            //1.基础信息
            //2.健康档案
            //查询是否存在对应健康档案
            UserProfile userProfile = userProfileMapper.getByUserId(user.getId());
            if(userProfile == null){
                isNewUser = true;
            }
            // 更新最后登录时间
            userMapper.updateLastLoginTime(user.getId());
            log.info("用户登录: openid={}, userId={}", openid, user.getId());
        }
            

        // 3. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getOpenid());


        // 4. 构建返回对象
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getId());
        vo.setToken(token);
        vo.setIsNewUser(isNewUser);
        vo.setSessionKey(sessionDTO.getSession_key());

        return vo;
    }

    /**
     * 更新用户基础信息
     * @param userId
     * @param basicDTO
     * @return
     */
    @Override
    public UserBasicInfoVO updateUserBasic(Long userId, UpdateUserBasicDTO basicDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        BeanUtils.copyProperties(basicDTO, user);

        userMapper.updateById(user);
        log.info("用户信息更新: userId={}, nickname={}, avatar={}", userId, user.getNickname(), user.getAvatar());

        // 返回更新后的用户信息
        return getUserBasicInfo(userId);
    }

    /**
     * 获取用户基础信息
     * @param userId
     * @return
     */
    @Override
    public UserBasicInfoVO getUserBasicInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserBasicInfoVO vo = new UserBasicInfoVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    /**
     * 生成随机昵称
     * 格式：前缀 + 随机6位数字
     * @return 随机昵称
     */
    private String generateRandomNickname() {
        String prefix = NICKNAME_PREFIXES.get(random.nextInt(NICKNAME_PREFIXES.size()));
        int randomNum = 100000 + random.nextInt(900000); // 生成6位随机数字
        return prefix + randomNum;
    }

    /**
     * 生成随机头像
     * 从预设的头像列表中随机选择一个
     * @return 随机头像URL
     */
    private String generateRandomAvatar() {
        return DEFAULT_AVATARS.get(random.nextInt(DEFAULT_AVATARS.size()));
    }

}
