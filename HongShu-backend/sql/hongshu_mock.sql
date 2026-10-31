-- HongShu 独立批量测试数据初始化脚本（MySQL 8.4）。
-- 整份文件独立执行，不需要 SOURCE 其他 SQL、不需要 Java/Python 生成器。
-- 注意：每次执行都会 DROP DATABASE hongshu_springboot，清空并重建整个库。
-- 只用于可重置的测试环境；现有环境执行前应停止应用写入并备份。
-- 执行后清理本项目 Redis，并重启后端完成管理员初始化。
-- 所有图片只保存 https://picsum.photos/seed/... URL，不下载、不登记为 OSS 上传。
-- 测试会员：10000000001 起，统一密码 HongshuDemo2026（库内为 BCrypt）。
-- 超级管理员仍由 HONGSHU_ADMIN_USERNAME / HONGSHU_ADMIN_PASSWORD 初始化。
-- 可修改以下默认值，或在同一 MySQL 会话中先 SET 对应变量再执行整份文件。
-- 固定 mock_now 和 mock_seed 可得到可复现的数据；默认时间相对本次执行日期。
SET @mock_users = COALESCE(@mock_users, 50);       -- 2..10000
SET @mock_notes = COALESCE(@mock_notes, 500);      -- 1..10000
SET @mock_comments = COALESCE(@mock_comments, 1500); -- 0..100000，含二级回复

SET @hongshu_mock_saved_sql_mode = @@SESSION.sql_mode;
SET @hongshu_mock_saved_time_zone = @@SESSION.time_zone;
SET @hongshu_mock_saved_foreign_key_checks = @@SESSION.foreign_key_checks;
SET @hongshu_mock_saved_unique_checks = @@SESSION.unique_checks;
SET @hongshu_mock_saved_autocommit = @@SESSION.autocommit;
SET @hongshu_mock_saved_auto_increment_increment = @@SESSION.auto_increment_increment;
SET @hongshu_mock_saved_auto_increment_offset = @@SESSION.auto_increment_offset;
SET @hongshu_mock_saved_character_set_client = @@SESSION.character_set_client;
SET @hongshu_mock_saved_character_set_connection = @@SESSION.character_set_connection;
SET @hongshu_mock_saved_character_set_results = @@SESSION.character_set_results;
SET @hongshu_mock_saved_collation_connection = @@SESSION.collation_connection;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Docker 初始化客户端可能使用 latin1；统一种子变量的字符集，避免后续 CONCAT 排序规则冲突。
SET @mock_seed = CONVERT(COALESCE(@mock_seed, 'hongshu-demo-v1') USING utf8mb4) COLLATE utf8mb4_unicode_ci; -- URL 安全字符，最多 64 位
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_AUTO_VALUE_ON_ZERO,NO_ENGINE_SUBSTITUTION';
SET SESSION time_zone = '+08:00';
SET SESSION foreign_key_checks = 0;
SET SESSION unique_checks = 0;
SET SESSION autocommit = 1;
SET SESSION auto_increment_increment = 1;
SET SESSION auto_increment_offset = 1;

-- 整库重建才能清除后来新增的表、视图、触发器、存储过程/函数和事件。
DROP DATABASE IF EXISTS `hongshu_springboot`;
CREATE DATABASE `hongshu_springboot`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `hongshu_springboot`;

SET @mock_now = COALESCE(@mock_now, CURRENT_TIMESTAMP);

-- 表结构

-- sys_config
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参数配置表';

-- sys_date
DROP TABLE IF EXISTS `sys_date`;
CREATE TABLE `sys_date` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `date_time` datetime DEFAULT NULL COMMENT '日期时间',
  `date_string` varchar(32) NOT NULL DEFAULT '' COMMENT '日期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `date_string` (`date_string`) USING BTREE,
  UNIQUE KEY `date_time` (`date_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日期';

-- sys_dept
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- sys_dict_data
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

-- sys_dict_type
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

-- sys_job
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度表';

-- sys_job_log
DROP TABLE IF EXISTS `sys_job_log`;
CREATE TABLE `sys_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '异常信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务调度日志表';

-- sys_logininfor
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '提示消息',
  `access_time` datetime DEFAULT NULL COMMENT '访问时间',
  `login_time` datetime DEFAULT NULL,
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统访问记录';

-- sys_menu
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由参数',
  `route_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由名称',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- sys_notice
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';

-- sys_oper_log
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志记录';

-- sys_post
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位信息表';

-- sys_role
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色信息表';

-- sys_role_dept
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和部门关联表';

-- sys_role_menu
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色和菜单关联表';

-- sys_user
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- sys_user_post
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与岗位关联表';

-- sys_user_role
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户和角色关联表';

-- web_album
DROP TABLE IF EXISTS `web_album`;
CREATE TABLE `web_album` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(50) DEFAULT NULL COMMENT '专辑标题',
  `uid` varchar(50) DEFAULT NULL COMMENT '用户id',
  `album_cover` varchar(512) DEFAULT NULL COMMENT '专辑封面',
  `type` int DEFAULT '0' COMMENT '专辑类型',
  `sort` int DEFAULT '0' COMMENT '专辑排序',
  `img_count` bigint DEFAULT '0' COMMENT '专辑中的笔记数量',
  `collection_count` bigint DEFAULT '0' COMMENT '收藏数量',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专辑表';

-- web_album_note_relation
DROP TABLE IF EXISTS `web_album_note_relation`;
CREATE TABLE `web_album_note_relation` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `aid` int DEFAULT NULL COMMENT '专辑id',
  `nid` int DEFAULT NULL COMMENT '笔记id',
  `sort` int NOT NULL DEFAULT 0,
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专辑笔记关联表\n';

-- web_chat
DROP TABLE IF EXISTS `web_chat`;
CREATE TABLE `web_chat` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `send_uid` int DEFAULT NULL COMMENT '发送方的用户id',
  `accept_uid` int DEFAULT NULL COMMENT '接收方的用户id',
  `content` longtext COMMENT '聊天内容',
  `msg_type` int DEFAULT '0' COMMENT '消息类型（0通知消息，1是文本消息，2是图片消息，3是语音消息，4是视频消息，5 自定义消息）',
  `chat_type` int DEFAULT '0' COMMENT '聊天类型（0是私聊，1是群聊）',
  `timestamp` bigint DEFAULT NULL COMMENT '时间戳',
  `send_deleted` tinyint NOT NULL DEFAULT 0,
  `accept_deleted` tinyint NOT NULL DEFAULT 0,
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天';

-- web_chat_user_relation
DROP TABLE IF EXISTS `web_chat_user_relation`;
CREATE TABLE `web_chat_user_relation` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `send_uid` varchar(50) DEFAULT NULL COMMENT '发送方的用户id',
  `accept_uid` varchar(50) DEFAULT NULL COMMENT '接收方的用户id',
  `content` longtext COMMENT '聊天内容',
  `count` int DEFAULT '0' COMMENT '未读消息的数量',
  `timestamp` bigint DEFAULT NULL COMMENT '时间戳',
  `chat_type` int DEFAULT '0' COMMENT '聊天类型（0是私聊，1是群聊）',
  `msg_type` int DEFAULT '0' COMMENT '消息类型（0通知消息，1是文本消息，2是图片消息，3是语音消息，4是视频消息，5 自定义消息）',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天用户关联';

-- web_comment
DROP TABLE IF EXISTS `web_comment`;
CREATE TABLE `web_comment` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `nid` int DEFAULT NULL COMMENT '笔记id',
  `note_uid` int DEFAULT NULL COMMENT '发布笔记用户id',
  `uid` int DEFAULT NULL COMMENT '发布评论的用户id',
  `pid` int DEFAULT '0' COMMENT '根评论id',
  `reply_id` int DEFAULT '0' COMMENT '回复的评论id',
  `reply_uid` int DEFAULT '0' COMMENT '回复评论的用户',
  `level` int DEFAULT '0' COMMENT '评论的等级',
  `sort` int DEFAULT '0' COMMENT '排序',
  `content` longtext COMMENT '评论的内容',
  `like_count` bigint DEFAULT '0' COMMENT '评论点赞数量',
  `two_comment_count` bigint DEFAULT '0' COMMENT '二级评论数量',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论';

-- web_comment_sync
DROP TABLE IF EXISTS `web_comment_sync`;
CREATE TABLE `web_comment_sync` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `nid` varchar(50) DEFAULT NULL COMMENT '评论关联的笔记id',
  `note_uid` varchar(50) DEFAULT NULL COMMENT '评论关联的笔记的发布用户',
  `uid` varchar(50) DEFAULT NULL COMMENT '发布评论的用户',
  `pid` varchar(50) DEFAULT '0' COMMENT '根评论id',
  `reply_id` varchar(50) DEFAULT '0' COMMENT '回复的评论id',
  `reply_uid` varchar(50) DEFAULT '0' COMMENT '回复评论的用户',
  `level` int DEFAULT '0' COMMENT '评论的等级',
  `sort` int DEFAULT '0' COMMENT '排序',
  `content` longtext COMMENT '评论的内容',
  `like_count` bigint DEFAULT '0' COMMENT '评论点赞数量',
  `two_comment_count` bigint DEFAULT '0' COMMENT '二级评论数量',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='同步评论';

-- web_follow
DROP TABLE IF EXISTS `web_follow`;
CREATE TABLE `web_follow` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid` int DEFAULT NULL COMMENT '当前用户',
  `fid` int DEFAULT NULL COMMENT '关注的用户',
  `type` int DEFAULT NULL,
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关注';

-- web_like_or_collect
DROP TABLE IF EXISTS `web_like_or_collect`;
CREATE TABLE `web_like_or_collect` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid` int NOT NULL COMMENT '点赞的用户',
  `like_or_collection_id` bigint NOT NULL COMMENT '点赞和收藏的id(可能是图片或者评论)',
  `publish_uid` bigint NOT NULL COMMENT '点赞和收藏通知的用户',
  `type` int NOT NULL COMMENT '点赞收藏类型:1 点赞图片 2点赞评论  3收藏图片 4收藏专辑',
  `timestamp` bigint DEFAULT NULL COMMENT '时间戳',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='点赞收藏';

-- web_login_log
DROP TABLE IF EXISTS `web_login_log`;
CREATE TABLE `web_login_log` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uid` int DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户名',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '登录状态（0失败 1成功）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录信息';

-- web_navbar
DROP TABLE IF EXISTS `web_navbar`;
CREATE TABLE `web_navbar` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(50) DEFAULT NULL COMMENT '分类标题',
  `pid` int DEFAULT NULL COMMENT '分类父id',
  `sort` int DEFAULT '0' COMMENT '分类排序',
  `like_count` bigint DEFAULT '0' COMMENT '喜欢数量',
  `description` longtext COMMENT '分类描述',
  `normal_cover` varchar(512) DEFAULT NULL COMMENT '分类的封面，如果是一级分类就是随便看看的封面，二级分类则是主封面',
  `hot_cover` varchar(255) DEFAULT NULL COMMENT '热门封面',
  `status` int DEFAULT NULL COMMENT '状态',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类表\n';

-- web_note
DROP TABLE IF EXISTS `web_note`;
CREATE TABLE `web_note` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(50) DEFAULT NULL COMMENT '笔记标题',
  `content` longtext COMMENT '笔记内容',
  `note_cover` varchar(512) DEFAULT NULL COMMENT '笔记封面',
  `note_cover_height` int DEFAULT NULL COMMENT '笔记高度',
  `album_id` varchar(255) DEFAULT NULL COMMENT '专辑ID',
  `uid` varchar(50) DEFAULT NULL COMMENT '用户id',
  `author` varchar(50) DEFAULT NULL COMMENT '作者',
  `cid` varchar(50) DEFAULT NULL COMMENT '笔记所属二级分类id',
  `cpid` varchar(50) DEFAULT NULL COMMENT '笔记所属一级分类id',
  `urls` longtext COMMENT '笔记urls',
  `count` int DEFAULT NULL COMMENT '图片数量',
  `sort` int DEFAULT NULL COMMENT '笔记排序',
  `pinned` int DEFAULT '0' COMMENT '是否置顶',
  `status` tinyint DEFAULT '1' COMMENT '笔记状态',
  `audit_status` int DEFAULT NULL COMMENT '审核状态',
  `note_type` int DEFAULT '0' COMMENT '笔记类型（0/1：图文，2：视频）',
  `view_count` bigint DEFAULT '0' COMMENT '笔记浏览次数',
  `like_count` bigint DEFAULT '0' COMMENT '笔记点赞次数',
  `collection_count` bigint DEFAULT '0' COMMENT '笔记收藏次数',
  `comment_count` bigint DEFAULT '0' COMMENT '笔记评论次数',
  `time` varchar(15) DEFAULT NULL COMMENT '时间戳',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `update_date_index` (`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='笔记';

-- web_oper_log
DROP TABLE IF EXISTS `web_oper_log`;
CREATE TABLE `web_oper_log` (
  `id` varchar(50) NOT NULL COMMENT '日志主键',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  `oper_uid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '操作的用户id',
  PRIMARY KEY (`id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户操作';

-- web_picture
DROP TABLE IF EXISTS `web_picture`;
CREATE TABLE `web_picture` (
  `uid` varchar(32) NOT NULL COMMENT '唯一uid',
  `file_uid` varchar(32) DEFAULT NULL COMMENT '图片uid',
  `pic_name` varchar(255) DEFAULT NULL COMMENT '图片名',
  `picture_sort_uid` varchar(32) DEFAULT NULL COMMENT '分类uid',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='图片表';

-- web_picture_sort
DROP TABLE IF EXISTS `web_picture_sort`;
CREATE TABLE `web_picture_sort` (
  `uid` varchar(32) NOT NULL COMMENT '唯一uid',
  `file_uid` varchar(32) DEFAULT NULL COMMENT '分类图片uid',
  `name` varchar(255) DEFAULT NULL COMMENT '分类名',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `parent_uid` varchar(32) DEFAULT NULL,
  `sort` int DEFAULT '0' COMMENT '排序字段，越大越靠前',
  `is_show` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否显示，1：是，0，否',
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='图片分类表';

-- web_tag
DROP TABLE IF EXISTS `web_tag`;
CREATE TABLE `web_tag` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(50) DEFAULT NULL COMMENT '标签标题',
  `like_count` bigint DEFAULT NULL COMMENT '标签使用次数',
  `sort` int DEFAULT NULL COMMENT '排序',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签';

-- web_tag_note_relation
DROP TABLE IF EXISTS `web_tag_note_relation`;
CREATE TABLE `web_tag_note_relation` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `nid` varchar(50) DEFAULT NULL COMMENT '笔记id',
  `tid` varchar(50) DEFAULT NULL COMMENT '标签id',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签笔记';

-- web_user
DROP TABLE IF EXISTS `web_user`;
CREATE TABLE `web_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `hs_id` bigint DEFAULT NULL COMMENT '红薯ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码',
  `avatar` varchar(512) DEFAULT NULL COMMENT '头像',
  `gender` tinyint DEFAULT '0' COMMENT '性别',
  `phone` varchar(50) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT 'email',
  `tags` varchar(255) DEFAULT NULL COMMENT '用户标签',
  `description` longtext COMMENT '描述',
  `status` tinyint DEFAULT '0' COMMENT '用户状态',
  `birthday` varchar(50) DEFAULT NULL COMMENT '生日',
  `address` varchar(50) DEFAULT NULL COMMENT '地址',
  `trend_count` bigint DEFAULT '0' COMMENT '笔记数量',
  `follower_count` bigint DEFAULT '0' COMMENT '关注数量',
  `fan_count` bigint DEFAULT '0' COMMENT '粉丝数量',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `is_online` tinyint DEFAULT '0' COMMENT '是否在线',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `deleted` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户';

-- web_user_note_relation
DROP TABLE IF EXISTS `web_user_note_relation`;
CREATE TABLE `web_user_note_relation` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `nid` varchar(50) DEFAULT NULL COMMENT '笔记id',
  `uid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户id',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-笔记关系表\n';

-- web_visit
DROP TABLE IF EXISTS `web_visit`;
CREATE TABLE `web_visit` (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键',
  `user_uid` varchar(255) DEFAULT NULL COMMENT '用户uid',
  `ip` varchar(255) DEFAULT NULL COMMENT '访问ip地址',
  `behavior` varchar(255) DEFAULT NULL COMMENT '用户行为',
  `module_uid` varchar(255) DEFAULT NULL COMMENT '模块uid（文章uid，标签uid，分类uid）',
  `other_data` varchar(255) DEFAULT NULL COMMENT '附加数据(比如搜索内容)',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `os` varchar(255) DEFAULT NULL COMMENT '操作系统',
  `browser` varchar(255) DEFAULT NULL COMMENT '浏览器',
  `ip_source` varchar(255) DEFAULT NULL COMMENT 'ip来源',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='Web访问记录表';

-- web_search_record
DROP TABLE IF EXISTS `web_search_record`;
CREATE TABLE `web_search_record` (
  `id` varchar(32) NOT NULL COMMENT '服务端生成的无连字符 UUID',
  `uid` varchar(50) DEFAULT NULL COMMENT '用户 ID；匿名记录为空',
  `content` varchar(255) NOT NULL COMMENT '搜索关键词',
  `search_count` bigint NOT NULL DEFAULT 0 COMMENT '搜索次数',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建用户',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(50) DEFAULT NULL COMMENT '修改用户',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_search_record_user_content` (`uid`, `content`),
  KEY `idx_search_record_user_updated` (`uid`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MySQL 搜索历史与热搜统计';

-- t_system_config
DROP TABLE IF EXISTS `t_system_config`;
CREATE TABLE `t_system_config` (
  `uid` varchar(32) NOT NULL COMMENT '配置唯一标识',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：0失效，1生效',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `email_user_name` varchar(255) DEFAULT NULL,
  `email_password` varchar(255) DEFAULT NULL,
  `smtp_address` varchar(255) DEFAULT NULL,
  `smtp_port` varchar(16) DEFAULT NULL,
  `start_email_notification` varchar(1) NOT NULL DEFAULT '0',
  `editor_model` varchar(1) NOT NULL DEFAULT '0',
  `theme_color` varchar(32) DEFAULT NULL,
  `dashboard_notification` text,
  `open_dashboard_notification` varchar(1) NOT NULL DEFAULT '0',
  `open_email_activate` varchar(1) NOT NULL DEFAULT '0',
  `search_model` varchar(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`),
  KEY `idx_system_config_status_created` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本地演示系统配置';
CREATE TABLE web_upload (
  path VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  uid VARCHAR(50) NOT NULL,
  storage_type INT NOT NULL DEFAULT 0 COMMENT '0：历史本地文件，3：阿里云OSS',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (path),
  KEY idx_web_upload_uid (uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE web_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT,
  uid VARCHAR(50) NOT NULL,
  email VARCHAR(254) NOT NULL,
  content TEXT NOT NULL,
  status CHAR(1) NOT NULL DEFAULT '0',
  resolution TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_web_feedback_status (status, create_time),
  KEY idx_web_feedback_uid (uid, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统基础数据：随本文件内置，运行时不读取其他脚本。
-- sys_config
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', @mock_now, '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', @mock_now, '', NULL, '初始化密码 123456');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', @mock_now, '', NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', 'admin', @mock_now, '', NULL, '是否开启验证码功能（true开启，false关闭）');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'true', 'Y', 'admin', @mock_now, 'admin', @mock_now, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '', 'Y', 'admin', @mock_now, '', NULL, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');

-- sys_dict_type
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', @mock_now, '', NULL, '用户性别列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin', @mock_now, '', NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin', @mock_now, '', NULL, '系统开关列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4, '任务状态', 'sys_job_status', '0', 'admin', @mock_now, '', NULL, '任务状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5, '任务分组', 'sys_job_group', '0', 'admin', @mock_now, '', NULL, '任务分组列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin', @mock_now, '', NULL, '系统是否列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin', @mock_now, '', NULL, '通知类型列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin', @mock_now, '', NULL, '通知状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin', @mock_now, '', NULL, '操作类型列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (10, '系统状态', 'sys_common_status', '0', 'admin', @mock_now, '', NULL, '登录状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100, '审核状态', 'audit_status', '0', 'admin', @mock_now, '', NULL, '0:未审核 1：通过 2：拒绝');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101, '笔记类型', 'note_type', '0', 'admin', @mock_now, 'admin', @mock_now, '0：图片，1:视频');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (102, '是否置顶', 'pinned', '0', 'admin', @mock_now, '', NULL, NULL);

-- sys_dict_data
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, 1, '男', '0', 'sys_user_sex', '', 'primary', 'Y', '0', 'admin', @mock_now, 'admin', @mock_now, '性别男');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, 2, '女', '1', 'sys_user_sex', '', 'danger', 'N', '0', 'admin', @mock_now, 'admin', @mock_now, '性别女');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', @mock_now, '', NULL, '性别未知');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', @mock_now, '', NULL, '显示菜单');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', @mock_now, '', NULL, '正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '停用状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (8, 1, '正常', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', @mock_now, '', NULL, '正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9, 2, '暂停', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '停用状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', @mock_now, '', NULL, '默认分组');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', @mock_now, '', NULL, '系统分组');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', @mock_now, '', NULL, '系统默认是');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '系统默认否');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', @mock_now, '', NULL, '通知');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', @mock_now, '', NULL, '公告');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', @mock_now, '', NULL, '正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '关闭状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (18, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 'admin', @mock_now, '', NULL, '其他操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (19, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', @mock_now, '', NULL, '新增操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (20, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', @mock_now, '', NULL, '修改操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (21, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '删除操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (22, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', @mock_now, '', NULL, '授权操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (23, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', @mock_now, '', NULL, '导出操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (24, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', @mock_now, '', NULL, '导入操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (25, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '强退操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (26, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', @mock_now, '', NULL, '生成操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (27, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '清空操作');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (28, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', @mock_now, '', NULL, '正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (29, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', @mock_now, '', NULL, '停用状态');
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100, 0, ' 未审核', '0', 'audit_status', NULL, 'warning', 'N', '0', 'admin', @mock_now, 'admin', @mock_now, NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101, 1, '已通过', '1', 'audit_status', NULL, 'success', 'N', '0', 'admin', @mock_now, '', NULL, NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (102, 2, '已驳回', '2', 'audit_status', NULL, 'danger', 'N', '0', 'admin', @mock_now, 'admin', @mock_now, NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (103, 0, '图文', '1', 'note_type', NULL, 'primary', 'N', '0', 'admin', @mock_now, 'admin', @mock_now, NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (104, 1, '视频', '2', 'note_type', NULL, 'warning', 'N', '0', 'admin', @mock_now, 'admin', @mock_now, NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (105, 1, '是', '1', 'pinned', NULL, 'primary', 'N', '0', 'admin', @mock_now, 'admin', @mock_now, NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (106, 0, '否', '0', 'pinned', NULL, 'danger', 'N', '0', 'admin', @mock_now, '', NULL, NULL);

-- sys_menu
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, '系统管理', 0, 8, 'system', NULL, '', NULL, 1, 0, 'M', '0', '0', '', 'system', 'admin', @mock_now, 'admin', @mock_now, '系统管理目录');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, '系统监控', 0, 10, 'monitor', NULL, '', NULL, 1, 0, 'M', '0', '0', '', 'monitor', 'admin', @mock_now, 'admin', @mock_now, '系统监控目录');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, '系统工具', 0, 9, 'tool', NULL, '', NULL, 1, 0, 'M', '0', '0', '', 'tool', 'admin', @mock_now, 'admin', @mock_now, '系统工具目录');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', NULL, 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', @mock_now, '', NULL, '用户管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', NULL, 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', @mock_now, '', NULL, '角色管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', NULL, 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', @mock_now, '', NULL, '菜单管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', NULL, 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', @mock_now, '', NULL, '部门管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', NULL, 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', @mock_now, '', NULL, '岗位管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', NULL, 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', @mock_now, '', NULL, '字典管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', NULL, 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', @mock_now, '', NULL, '参数设置菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', NULL, 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 'admin', @mock_now, '', NULL, '通知公告菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (108, '日志管理', 1, 9, 'log', '', '', NULL, 1, 0, 'M', '0', '0', '', 'log', 'admin', @mock_now, '', NULL, '日志管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', NULL, 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', @mock_now, '', NULL, '在线用户菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (110, '定时任务', 2, 2, 'job', 'monitor/job/index', '', NULL, 1, 0, 'C', '0', '0', 'monitor:job:list', 'job', 'admin', @mock_now, '', NULL, '定时任务菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', NULL, 1, 0, 'C', '0', '0', 'monitor:druid:list', 'druid', 'admin', @mock_now, '', NULL, '数据监控菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (112, '服务监控', 2, 4, 'server', 'monitor/server/index', '', NULL, 1, 0, 'C', '0', '0', 'monitor:server:list', 'server', 'admin', @mock_now, '', NULL, '服务监控菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', NULL, 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 'admin', @mock_now, '', NULL, '缓存监控菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (114, '缓存列表', 2, 6, 'cacheList', 'monitor/cache/list', '', NULL, 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis-list', 'admin', @mock_now, '', NULL, '缓存列表菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (117, '系统接口', 3, 3, 'swagger', 'tool/swagger/index', '', NULL, 1, 0, 'C', '0', '0', 'tool:swagger:list', 'swagger', 'admin', @mock_now, '', NULL, '系统接口菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', NULL, 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', @mock_now, 'admin', @mock_now, '操作日志菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', NULL, 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', @mock_now, 'admin', @mock_now, '登录日志菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1000, '用户查询', 100, 1, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1001, '用户新增', 100, 2, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1002, '用户修改', 100, 3, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1003, '用户删除', 100, 4, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1004, '用户导出', 100, 5, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1005, '用户导入', 100, 6, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1006, '重置密码', 100, 7, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1007, '角色查询', 101, 1, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1008, '角色新增', 101, 2, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1009, '角色修改', 101, 3, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1010, '角色删除', 101, 4, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1011, '角色导出', 101, 5, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1012, '菜单查询', 102, 1, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1013, '菜单新增', 102, 2, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1014, '菜单修改', 102, 3, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1015, '菜单删除', 102, 4, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1016, '部门查询', 103, 1, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1017, '部门新增', 103, 2, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1018, '部门修改', 103, 3, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1019, '部门删除', 103, 4, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1020, '岗位查询', 104, 1, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1021, '岗位新增', 104, 2, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1022, '岗位修改', 104, 3, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1023, '岗位删除', 104, 4, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1024, '岗位导出', 104, 5, '', '', '', NULL, 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1025, '字典查询', 105, 1, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1026, '字典新增', 105, 2, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1027, '字典修改', 105, 3, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1028, '字典删除', 105, 4, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1029, '字典导出', 105, 5, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1030, '参数查询', 106, 1, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1031, '参数新增', 106, 2, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1032, '参数修改', 106, 3, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1033, '参数删除', 106, 4, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1034, '参数导出', 106, 5, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1035, '公告查询', 107, 1, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:notice:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1036, '公告新增', 107, 2, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:notice:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1037, '公告修改', 107, 3, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1038, '公告删除', 107, 4, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1039, '操作查询', 500, 1, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1040, '操作删除', 500, 2, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1041, '日志导出', 500, 3, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1042, '登录查询', 501, 1, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1043, '登录删除', 501, 2, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1044, '日志导出', 501, 3, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1045, '账户解锁', 501, 4, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1046, '在线查询', 109, 1, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1047, '批量强退', 109, 2, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1048, '单条强退', 109, 3, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1049, '任务查询', 110, 1, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:job:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1050, '任务新增', 110, 2, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:job:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1051, '任务修改', 110, 3, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:job:edit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1052, '任务删除', 110, 4, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:job:remove', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1053, '状态修改', 110, 5, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1054, '任务导出', 110, 6, '#', '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:job:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2000, '门户管理', 0, 0, 'web', NULL, NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'example', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2001, '导航管理', 2000, 0, 'web', 'web/navbar/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'list', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2002, '会员中心', 0, 1, 'member', NULL, NULL, NULL, 1, 0, 'M', '0', '0', '', 'peoples', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2003, '会员管理', 2002, 1, 'member', 'web/member/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'peoples', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2004, '会员用户列表', 2003, 1, '', NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'web:user:list', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2005, '会员用户查询', 2003, 2, '', NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'web:user:query', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2006, '会员用户新增', 2003, 3, '', NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'web:user:add', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2007, '会员用户修改', 2003, 4, '', NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'web:user:update', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2008, '会员用户删除', 2003, 5, '', NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'web:user:delete', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2009, '会员用户审核', 2003, 6, '', NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'web:user:audit', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2010, '会员用户导出', 2003, 7, '', NULL, NULL, NULL, 1, 0, 'F', '0', '0', 'web:user:export', '#', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2011, '笔记管理', 0, 2, 'note', NULL, NULL, NULL, 1, 0, 'M', '0', '0', '', 'education', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2017, '笔记管理', 2011, 1, 'note', 'web/note/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'dict', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2019, '审核管理', 2011, 2, 'audit', 'web/audit/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'checkbox', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2020, '专辑管理', 2011, 4, 'album', 'web/album/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'button', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2021, '消息管理', 0, 5, 'message', NULL, NULL, NULL, 1, 0, 'M', '0', '0', '', 'email', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2022, '数据中心', 0, -1, 'dashboard', 'dashboard/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'dashboard', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2023, '评论管理', 2021, 0, 'comment', 'web/comment/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'email', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2025, '标签管理', 2011, 3, 'tag', 'web/tag/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'icon', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2028, '日志管理', 0, 6, 'loginlog', NULL, NULL, NULL, 1, 0, 'M', '0', '0', '', 'log', 'admin', @mock_now, 'admin', @mock_now, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2029, '登录日志', 2028, 1, 'loginlog', 'web/loginlog/index', NULL, NULL, 1, 0, 'C', '0', '0', '', 'logininfor', 'admin', @mock_now, 'admin', @mock_now, '');

-- sys_post
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, 'ceo', '董事长', 1, '0', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, 'se', '项目经理', 2, '0', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, 'hr', '人力资源', 3, '0', 'admin', @mock_now, '', NULL, '');
INSERT INTO `sys_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4, 'user', '普通员工', 4, '0', 'admin', @mock_now, '', NULL, '');

-- sys_role
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', @mock_now, '', NULL, '超级管理员');
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', @mock_now, 'admin', @mock_now, '普通角色');
UPDATE sys_menu SET perms = 'web:navbar:list' WHERE menu_id = 2001 AND (perms IS NULL OR perms = '');

UPDATE sys_menu SET perms = 'web:member:list' WHERE menu_id = 2003 AND (perms IS NULL OR perms = '');

UPDATE sys_menu SET perms = 'web:note:list' WHERE menu_id = 2017 AND (perms IS NULL OR perms = '');

UPDATE sys_menu SET perms = 'web:note:list' WHERE menu_id = 2019 AND (perms IS NULL OR perms = '');

UPDATE sys_menu SET perms = 'web:album:list' WHERE menu_id = 2020 AND (perms IS NULL OR perms = '');

UPDATE sys_menu SET perms = 'web:comment:list' WHERE menu_id = 2023 AND (perms IS NULL OR perms = '');

UPDATE sys_menu SET perms = 'web:tag:list' WHERE menu_id = 2025 AND (perms IS NULL OR perms = '');

UPDATE sys_menu SET perms = 'web:logininfor:list' WHERE menu_id = 2029 AND (perms IS NULL OR perms = '');

UPDATE sys_menu SET perms = 'web:member:list' WHERE menu_id = 2004 AND perms LIKE 'web:user:%';

UPDATE sys_menu SET perms = 'web:member:query' WHERE menu_id = 2005 AND perms LIKE 'web:user:%';

UPDATE sys_menu SET perms = 'web:member:add' WHERE menu_id = 2006 AND perms LIKE 'web:user:%';

UPDATE sys_menu SET perms = 'web:member:edit' WHERE menu_id = 2007 AND perms LIKE 'web:user:%';

UPDATE sys_menu SET perms = 'web:member:remove' WHERE menu_id = 2008 AND perms LIKE 'web:user:%';

UPDATE sys_menu SET perms = 'web:member:export' WHERE menu_id = 2010 AND perms LIKE 'web:user:%';

UPDATE sys_menu SET status = '1' WHERE menu_id = 2009 AND perms = 'web:user:audit';

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '导航查看', 2001, 1, '#', 1, 0, 'F', '0', '0', 'web:navbar:query', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2001 AND perms = 'web:navbar:query');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '导航新增', 2001, 2, '#', 1, 0, 'F', '0', '0', 'web:navbar:add', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2001 AND perms = 'web:navbar:add');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '导航修改', 2001, 3, '#', 1, 0, 'F', '0', '0', 'web:navbar:edit', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2001 AND perms = 'web:navbar:edit');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '导航删除', 2001, 4, '#', 1, 0, 'F', '0', '0', 'web:navbar:remove', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2001 AND perms = 'web:navbar:remove');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '会员查看', 2003, 1, '#', 1, 0, 'F', '0', '0', 'web:member:query', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2003 AND perms = 'web:member:query');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '会员新增', 2003, 2, '#', 1, 0, 'F', '0', '0', 'web:member:add', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2003 AND perms = 'web:member:add');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '会员修改', 2003, 3, '#', 1, 0, 'F', '0', '0', 'web:member:edit', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2003 AND perms = 'web:member:edit');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '会员删除', 2003, 4, '#', 1, 0, 'F', '0', '0', 'web:member:remove', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2003 AND perms = 'web:member:remove');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '会员导出', 2003, 5, '#', 1, 0, 'F', '0', '0', 'web:member:export', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2003 AND perms = 'web:member:export');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '笔记查看', 2017, 1, '#', 1, 0, 'F', '0', '0', 'web:note:query', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2017 AND perms = 'web:note:query');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '笔记新增', 2017, 2, '#', 1, 0, 'F', '0', '0', 'web:note:add', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2017 AND perms = 'web:note:add');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '笔记修改', 2017, 3, '#', 1, 0, 'F', '0', '0', 'web:note:edit', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2017 AND perms = 'web:note:edit');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '笔记删除', 2017, 4, '#', 1, 0, 'F', '0', '0', 'web:note:remove', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2017 AND perms = 'web:note:remove');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '笔记导出', 2017, 5, '#', 1, 0, 'F', '0', '0', 'web:note:export', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2017 AND perms = 'web:note:export');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '审核查看', 2019, 1, '#', 1, 0, 'F', '0', '0', 'web:note:query', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2019 AND perms = 'web:note:query');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '审核审批', 2019, 2, '#', 1, 0, 'F', '0', '0', 'web:note:audit', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2019 AND perms = 'web:note:audit');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '专辑查看', 2020, 1, '#', 1, 0, 'F', '0', '0', 'web:album:query', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2020 AND perms = 'web:album:query');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '专辑新增', 2020, 2, '#', 1, 0, 'F', '0', '0', 'web:album:add', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2020 AND perms = 'web:album:add');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '专辑修改', 2020, 3, '#', 1, 0, 'F', '0', '0', 'web:album:edit', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2020 AND perms = 'web:album:edit');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '专辑删除', 2020, 4, '#', 1, 0, 'F', '0', '0', 'web:album:remove', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2020 AND perms = 'web:album:remove');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '评论查看', 2023, 1, '#', 1, 0, 'F', '0', '0', 'web:comment:query', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2023 AND perms = 'web:comment:query');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '评论删除', 2023, 2, '#', 1, 0, 'F', '0', '0', 'web:comment:remove', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2023 AND perms = 'web:comment:remove');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '标签查看', 2025, 1, '#', 1, 0, 'F', '0', '0', 'web:tag:query', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2025 AND perms = 'web:tag:query');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '标签新增', 2025, 2, '#', 1, 0, 'F', '0', '0', 'web:tag:add', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2025 AND perms = 'web:tag:add');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '标签修改', 2025, 3, '#', 1, 0, 'F', '0', '0', 'web:tag:edit', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2025 AND perms = 'web:tag:edit');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '标签删除', 2025, 4, '#', 1, 0, 'F', '0', '0', 'web:tag:remove', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2025 AND perms = 'web:tag:remove');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '会员登录日志删除', 2029, 1, '#', 1, 0, 'F', '0', '0', 'web:logininfor:remove', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 2029 AND perms = 'web:logininfor:remove');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '用户反馈', 2021, 1, 'feedback', 'web/feedback/index', 1, 0, 'C', '0', '0', 'web:feedback:list', 'message', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE component = 'web/feedback/index');
SET @hongshu_feedback_menu = (SELECT MIN(menu_id) FROM sys_menu WHERE component = 'web/feedback/index');

SET @hongshu_fix_menu = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT @hongshu_fix_menu, '处理反馈', @hongshu_feedback_menu, 1, '#', 1, 0, 'F', '0', '0', 'web:feedback:edit', '#', 'admin', @mock_now
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = @hongshu_feedback_menu AND perms = 'web:feedback:edit');

UPDATE sys_menu SET menu_name = '导航管理' WHERE menu_id = 2001;
UPDATE sys_menu SET menu_name = '登录日志' WHERE menu_id = 2029;
UPDATE sys_menu SET order_num = -1 WHERE menu_id = 2022;

-- 全新基础组织和待初始化管理员，不携带历史用户、日志或本地图片路径。
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, status, del_flag, create_by, create_time)
VALUES (100, 0, '0', '测试社区', 0, '0', '0', 'mock', @mock_now),
       (101, 100, '0,100', '运营中心', 1, '0', '0', 'mock', @mock_now),
       (103, 101, '0,100,101', '研发部门', 1, '0', '0', 'mock', @mock_now),
       (105, 101, '0,100,101', '测试部门', 2, '0', '0', 'mock', @mock_now);
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, avatar, password, status, del_flag, create_by, create_time)
VALUES (1, 103, 'admin', '管理员', '00', CONCAT('https://picsum.photos/seed/', @mock_seed, '-admin/200/200'),
        '{ADMIN_INIT_REQUIRED}', '1', '0', 'mock', @mock_now);
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO sys_user_post (user_id, post_id) VALUES (1, 1);
INSERT INTO sys_role_dept (role_id, dept_id) VALUES (2, 100), (2, 101), (2, 105);
-- 普通角色默认无业务权限，由管理员在角色管理中分配。
INSERT INTO t_system_config (uid, status, create_time, update_time, start_email_notification,
                            editor_model, open_dashboard_notification, open_email_activate, search_model)
VALUES ('mock-system-config', 1, @mock_now, @mock_now, '0', '0', '0', '0', '0');

-- 集合式批量造数。临时序列表只在本次连接存在，生成过程执行后删除。
DELIMITER $$
CREATE PROCEDURE hongshu_generate_mock_data()
BEGIN
    DECLARE image_index INT DEFAULT 2;
    DECLARE root_comments INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS mock_numbers;
        RESIGNAL;
    END;

    IF CAST(@mock_users AS CHAR) NOT REGEXP '^[0-9]+$' OR @mock_users < 2 OR @mock_users > 10000
       OR CAST(@mock_notes AS CHAR) NOT REGEXP '^[0-9]+$' OR @mock_notes < 1 OR @mock_notes > 10000
       OR CAST(@mock_comments AS CHAR) NOT REGEXP '^[0-9]+$' OR @mock_comments < 0 OR @mock_comments > 100000 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid mock counts: users 2..10000, notes 1..10000, comments 0..100000';
    END IF;
    IF @mock_seed NOT REGEXP '^[A-Za-z0-9_-]{1,64}$' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'mock_seed must contain 1..64 ASCII letters, digits, underscores or hyphens';
    END IF;
    SET root_comments = CEIL(@mock_comments * 0.75);

    DROP TEMPORARY TABLE IF EXISTS mock_numbers;
    CREATE TEMPORARY TABLE mock_numbers (n INT PRIMARY KEY) ENGINE=MEMORY;
    INSERT INTO mock_numbers (n)
    SELECT a.d + b.d * 10 + c.d * 100 + d.d * 1000 + e.d * 10000 + 1 AS n
    FROM JSON_TABLE('[0,1,2,3,4,5,6,7,8,9]', '$[*]' COLUMNS (d INT PATH '$')) a
    CROSS JOIN JSON_TABLE('[0,1,2,3,4,5,6,7,8,9]', '$[*]' COLUMNS (d INT PATH '$')) b
    CROSS JOIN JSON_TABLE('[0,1,2,3,4,5,6,7,8,9]', '$[*]' COLUMNS (d INT PATH '$')) c
    CROSS JOIN JSON_TABLE('[0,1,2,3,4,5,6,7,8,9]', '$[*]' COLUMNS (d INT PATH '$')) d
    CROSS JOIN JSON_TABLE('[0,1,2,3,4,5,6,7,8,9]', '$[*]' COLUMNS (d INT PATH '$')) e
    WHERE a.d + b.d * 10 + c.d * 100 + d.d * 1000 + e.d * 10000 < GREATEST(@mock_users, @mock_notes, @mock_comments, 731);

    START TRANSACTION;

    INSERT INTO sys_date (id, date_time, date_string)
    SELECT n, DATE_ADD(DATE(@mock_now), INTERVAL (n - 366) DAY),
           DATE_FORMAT(DATE_ADD(DATE(@mock_now), INTERVAL (n - 366) DAY), '%Y-%m-%d')
    FROM mock_numbers WHERE n <= 731 ORDER BY n;

    INSERT INTO web_navbar (id, title, pid, sort, like_count, description, normal_cover, hot_cover, status, creator, create_time, update_time)
    SELECT seq.n, ELT(seq.n, '生活', '旅行', '美食', '摄影'), 0, seq.n, 0, '测试数据分类',
           CONCAT('https://picsum.photos/seed/', @mock_seed, '-category-', seq.n, '/600/400'),
           CONCAT('https://picsum.photos/seed/', @mock_seed, '-category-hot-', seq.n, '/600/400'), 0, 'mock', @mock_now, @mock_now
    FROM mock_numbers seq WHERE seq.n <= 4 ORDER BY seq.n;
    INSERT INTO web_navbar (id, title, pid, sort, like_count, description, normal_cover, hot_cover, status, creator, create_time, update_time)
    SELECT 100 + n, ELT(n, '日常记录', '家居好物', '城市漫游', '山野露营', '家庭料理', '咖啡甜点', '街头光影', '自然风景'),
           CEIL(n / 2), n, 0, '可用于分类筛选的测试笔记',
           CONCAT('https://picsum.photos/seed/', @mock_seed, '-subcategory-', n, '/600/400'),
           CONCAT('https://picsum.photos/seed/', @mock_seed, '-subcategory-hot-', n, '/600/400'), 0, 'mock', @mock_now, @mock_now
    FROM mock_numbers WHERE n <= 8 ORDER BY n;

    INSERT INTO web_tag (id, title, like_count, sort, creator, create_time, update_time)
    SELECT seq.n, ELT(seq.n, '日常记录', '家居好物', '城市漫游', '山野露营', '家庭料理', '咖啡甜点', '街头光影', '自然风景',
                        '周末计划', '生活灵感', '今日分享', '慢慢生活'), 0, seq.n, 'mock', @mock_now, @mock_now
    FROM mock_numbers seq WHERE seq.n <= 12 ORDER BY seq.n;

    INSERT INTO web_user (id, hs_id, username, password, avatar, gender, phone, email, tags, description, status,
                          birthday, address, trend_count, follower_count, fan_count, creator, create_time, update_time, remark)
    SELECT n, 8000000000 + n,
           CONCAT(ELT(1 + MOD(n - 1, 10), '小林', '阿橙', '木木', '小鹿', '南风', '山茶', '青禾', '小满', '星野', '云朵'), LPAD(n, 5, '0')),
           '$2b$10$bz3E4zxNGxvSQOmti8xs1.uWNEI1uOsq5u3pjsD3iAoQM25ag6jta',
           CONCAT('https://picsum.photos/seed/', @mock_seed, '-user-', n, '/200/200'), MOD(n, 3),
           CAST(10000000000 + n AS CHAR), CONCAT('demo', n, '@example.com'), '生活,旅行,摄影',
           CONCAT('这是第 ', n, ' 位测试会员，喜欢记录生活里的小发现。'), 0,
           DATE_FORMAT(DATE_ADD('1990-01-01', INTERVAL MOD(n * 37, 6000) DAY), '%Y-%m-%d'),
           ELT(1 + MOD(n - 1, 6), '上海', '杭州', '成都', '北京', '广州', '南京'), 0, 0, 0, 'mock',
           DATE_SUB(@mock_now, INTERVAL (120 + MOD(n, 30)) DAY), DATE_SUB(@mock_now, INTERVAL (120 + MOD(n, 30)) DAY),
           CONCAT('测试数据：', @mock_seed)
    FROM mock_numbers WHERE n <= @mock_users ORDER BY n;

    -- 每篇 1..6 张图；封面尺寸为 800x600 / 800x800 / 800x1000。
    -- note_cover_height 是前端 240px 宽卡片的显示高度，而不是原图像素高度。
    INSERT INTO web_note (id, title, content, note_cover, note_cover_height, album_id, uid, author, cid, cpid,
                          urls, count, sort, pinned, status, audit_status, note_type, like_count, collection_count,
                          comment_count, view_count, time, creator, create_time, update_time)
    SELECT s.n,
           CONCAT(ELT(1 + MOD(s.n - 1, 8), '把普通日子过得有趣', '让房间更舒服的小改变', '周末城市散步路线', '山野露营的一天',
                       '下班后的简单晚餐', '发现一家安静的咖啡店', '记录街角的光与影', '把自然风景装进相册'), ' · ', s.n),
           CONCAT('<p>', ELT(1 + MOD(s.n - 1, 8), '今天留了一点时间给自己，记录几个平凡又开心的瞬间。',
                       '整理了书桌和窗边的小角落，空间清爽了，心情也放松了。', '沿着熟悉的街道慢慢走，发现了平时忽略的店铺与风景。',
                       '带上简单装备去山野走走，在树荫下吃午饭，听风经过。', '用冰箱里常见的食材做一顿热饭，简单搭配也很满足。',
                       '点一杯咖啡，翻几页书，把周末下午留给自己。', '傍晚的光线很柔和，随手记录街头的小细节。',
                       '远处的山和近处的树都有不同的颜色，值得停下来看看。'),
                       '</p><p>这是一篇用于列表、详情、搜索和互动测试的模拟笔记。图片由 Picsum 提供，与文字主题不一定对应。</p>'),
           CONCAT('https://picsum.photos/seed/', @mock_seed, '-note-', s.n, '-1/800/', ELT(1 + MOD(s.n - 1, 3), '600', '800', '1000')),
           ELT(1 + MOD(s.n - 1, 3), 180, 240, 300), CAST(u.id AS CHAR), CAST(u.id AS CHAR), u.username,
           CAST(101 + MOD(s.n - 1, 8) AS CHAR), CAST(1 + FLOOR(MOD(s.n - 1, 8) / 2) AS CHAR),
           JSON_ARRAY(CONCAT('https://picsum.photos/seed/', @mock_seed, '-note-', s.n, '-1/800/', ELT(1 + MOD(s.n - 1, 3), '600', '800', '1000'))),
           1 + MOD(s.n - 1, 6), 0, 0, 1, 1, 1, 0, 0, 0, 100 + MOD(s.n * 7919, 20000),
           CAST(CAST(UNIX_TIMESTAMP(DATE_SUB(@mock_now, INTERVAL (1 + MOD(s.n * 13, 90)) DAY)) * 1000 AS UNSIGNED) AS CHAR),
           'mock', DATE_SUB(@mock_now, INTERVAL (1 + MOD(s.n * 13, 90)) DAY), DATE_SUB(@mock_now, INTERVAL (1 + MOD(s.n * 13, 90)) DAY)
    FROM mock_numbers s JOIN web_user u ON u.id = 1 + MOD(s.n - 1, @mock_users)
    WHERE s.n <= @mock_notes ORDER BY s.n;
    WHILE image_index <= 6 DO
        UPDATE web_note SET urls = JSON_ARRAY_APPEND(urls, '$',
            CONCAT('https://picsum.photos/seed/', @mock_seed, '-note-', id, '-', image_index, '/800/',
                   ELT(1 + MOD(id + image_index - 2, 3), '600', '800', '1000')))
        WHERE count >= image_index;
        SET image_index = image_index + 1;
    END WHILE;

    INSERT INTO web_user_note_relation (nid, uid, creator, create_time, update_time)
    SELECT id, uid, 'mock', create_time, update_time FROM web_note ORDER BY id;
    INSERT INTO web_tag_note_relation (nid, tid, creator, create_time, update_time)
    SELECT id, 1 + MOD(id - 1, 8), 'mock', create_time, update_time FROM web_note ORDER BY id;
    INSERT INTO web_tag_note_relation (nid, tid, creator, create_time, update_time)
    SELECT id, 9 + MOD(id - 1, 4), 'mock', create_time, update_time FROM web_note ORDER BY id;
    UPDATE web_tag t SET like_count = (SELECT COUNT(*) FROM web_tag_note_relation r WHERE r.tid = t.id);

    INSERT INTO web_album (id, title, uid, album_cover, type, sort, img_count, collection_count, creator, create_time, update_time)
    SELECT u.id, CONCAT(u.username, '的生活相册'), u.id,
           CONCAT('https://picsum.photos/seed/', @mock_seed, '-album-', u.id, '/800/800'), 0, 0, 0, 0, 'mock',
           DATE_ADD(u.create_time, INTERVAL 1 DAY), DATE_ADD(u.create_time, INTERVAL 1 DAY) FROM web_user u ORDER BY u.id;
    INSERT INTO web_album_note_relation (aid, nid, sort, creator, create_time, update_time)
    SELECT CAST(uid AS UNSIGNED), id, FLOOR((id - 1) / @mock_users), 'mock', create_time, update_time FROM web_note ORDER BY id;
    UPDATE web_album a SET img_count = (SELECT COUNT(*) FROM web_album_note_relation r WHERE r.aid = a.id),
        album_cover = COALESCE((SELECT n.note_cover FROM web_album_note_relation r JOIN web_note n ON n.id = r.nid
                               WHERE r.aid = a.id ORDER BY r.sort, r.id LIMIT 1), a.album_cover);

    -- 一级评论和回复共 mock_comments 条；回复引用同篇笔记下已存在的一级评论。
    INSERT INTO web_comment (id, nid, note_uid, uid, pid, reply_id, reply_uid, level, sort, content,
                             like_count, two_comment_count, creator, create_time, update_time)
    SELECT s.n, n.id, n.uid, 1 + MOD(s.n + FLOOR((s.n - 1) / @mock_notes), @mock_users), 0, 0, 0, 1, 0,
           ELT(1 + MOD(s.n - 1, 8), '这个角度拍得很好看！', '已经收藏，下次也来试试。', '看完很有共鸣，感谢分享。',
               '周末就按这个思路安排。', '喜欢这种简单舒服的感觉。', '细节记录得很用心。', '有空也想去走走。', '期待你的下一篇分享。'),
           0, 0, 'mock', DATE_ADD(n.create_time, INTERVAL (60 + MOD(s.n * 17, 43200)) SECOND),
           DATE_ADD(n.create_time, INTERVAL (60 + MOD(s.n * 17, 43200)) SECOND)
    FROM mock_numbers s JOIN web_note n ON n.id = 1 + MOD(s.n - 1, @mock_notes)
    WHERE s.n <= root_comments ORDER BY s.n;
    INSERT INTO web_comment (id, nid, note_uid, uid, pid, reply_id, reply_uid, level, sort, content,
                             like_count, two_comment_count, creator, create_time, update_time)
    SELECT root_comments + s.n, p.nid, p.note_uid, 1 + MOD(p.uid, @mock_users), p.id, p.id, p.uid, 2, 0,
           ELT(1 + MOD(s.n - 1, 4), '谢谢喜欢，一起记录生活！', '有机会可以试试看。', '下次再分享更多细节。', '很开心遇到同样喜欢这些的人。'),
           0, 0, 'mock', DATE_ADD(p.create_time, INTERVAL 10 MINUTE), DATE_ADD(p.create_time, INTERVAL 10 MINUTE)
    FROM mock_numbers s JOIN web_comment p ON p.id = 1 + MOD(s.n - 1, NULLIF(root_comments, 0))
    WHERE s.n <= @mock_comments - root_comments ORDER BY s.n;
    UPDATE web_comment p LEFT JOIN (SELECT pid, COUNT(*) AS total FROM web_comment WHERE pid <> 0 GROUP BY pid) r ON r.pid = p.id
    SET p.two_comment_count = COALESCE(r.total, 0);

    -- 各笔记生成 1..8 个不重复点赞、1..3 个收藏（受会员数限制），不生成自关注。
    INSERT INTO web_like_or_collect (uid, like_or_collection_id, publish_uid, type, timestamp, creator, create_time, update_time)
    SELECT 1 + MOD(CAST(n.uid AS UNSIGNED) - 1 + s.n, @mock_users), n.id, n.uid, 1,
           UNIX_TIMESTAMP(DATE_ADD(n.create_time, INTERVAL s.n HOUR)) * 1000, 'mock',
           DATE_ADD(n.create_time, INTERVAL s.n HOUR), DATE_ADD(n.create_time, INTERVAL s.n HOUR)
    FROM web_note n JOIN mock_numbers s ON s.n <= LEAST(@mock_users - 1, 1 + MOD(n.id, 8)) ORDER BY n.id, s.n;
    INSERT INTO web_like_or_collect (uid, like_or_collection_id, publish_uid, type, timestamp, creator, create_time, update_time)
    SELECT 1 + MOD(CAST(n.uid AS UNSIGNED) - 1 + s.n, @mock_users), n.id, n.uid, 3,
           UNIX_TIMESTAMP(DATE_ADD(n.create_time, INTERVAL s.n HOUR)) * 1000, 'mock',
           DATE_ADD(n.create_time, INTERVAL s.n HOUR), DATE_ADD(n.create_time, INTERVAL s.n HOUR)
    FROM web_note n JOIN mock_numbers s ON s.n <= LEAST(@mock_users - 1, 1 + MOD(n.id, 3)) ORDER BY n.id, s.n;
    INSERT INTO web_like_or_collect (uid, like_or_collection_id, publish_uid, type, timestamp, creator, create_time, update_time)
    SELECT 1 + MOD(uid, @mock_users), id, uid, 2, UNIX_TIMESTAMP(DATE_ADD(create_time, INTERVAL 15 MINUTE)) * 1000,
           'mock', DATE_ADD(create_time, INTERVAL 15 MINUTE), DATE_ADD(create_time, INTERVAL 15 MINUTE)
    FROM web_comment WHERE MOD(id, 3) = 0 ORDER BY id;
    INSERT INTO web_like_or_collect (uid, like_or_collection_id, publish_uid, type, timestamp, creator, create_time, update_time)
    SELECT 1 + MOD(CAST(uid AS UNSIGNED), @mock_users), id, uid, 4, UNIX_TIMESTAMP(@mock_now) * 1000,
           'mock', @mock_now, @mock_now FROM web_album ORDER BY id;
    INSERT INTO web_follow (uid, fid, type, creator, create_time, update_time)
    SELECT u.id, 1 + MOD(u.id - 1 + s.n, @mock_users), 1, 'mock', DATE_SUB(@mock_now, INTERVAL 91 DAY), DATE_SUB(@mock_now, INTERVAL 91 DAY)
    FROM web_user u JOIN mock_numbers s ON s.n <= LEAST(@mock_users - 1, 1 + MOD(u.id, 5)) ORDER BY u.id, s.n;

    -- 汇总数全部来自关联记录，避免页面数字与实际列表不一致。
    UPDATE web_note n
    LEFT JOIN (SELECT like_or_collection_id AS nid, SUM(type = 1) AS likes, SUM(type = 3) AS collects
               FROM web_like_or_collect WHERE type IN (1, 3) GROUP BY like_or_collection_id) r ON r.nid = n.id
    LEFT JOIN (SELECT nid, COUNT(*) AS total FROM web_comment GROUP BY nid) c ON c.nid = n.id
    SET n.like_count = COALESCE(r.likes, 0), n.collection_count = COALESCE(r.collects, 0), n.comment_count = COALESCE(c.total, 0);
    UPDATE web_comment c
    LEFT JOIN (SELECT like_or_collection_id AS cid, COUNT(*) AS total FROM web_like_or_collect WHERE type = 2 GROUP BY like_or_collection_id) r ON r.cid = c.id
    SET c.like_count = COALESCE(r.total, 0);
    UPDATE web_album a
    LEFT JOIN (SELECT like_or_collection_id AS aid, COUNT(*) AS total FROM web_like_or_collect WHERE type = 4 GROUP BY like_or_collection_id) r ON r.aid = a.id
    SET a.collection_count = COALESCE(r.total, 0);
    UPDATE web_user u
    LEFT JOIN (SELECT uid, COUNT(*) AS total FROM web_note GROUP BY uid) n ON n.uid = u.id
    LEFT JOIN (SELECT uid, COUNT(*) AS total FROM web_follow GROUP BY uid) f ON f.uid = u.id
    LEFT JOIN (SELECT fid, COUNT(*) AS total FROM web_follow GROUP BY fid) fans ON fans.fid = u.id
    SET u.trend_count = COALESCE(n.total, 0), u.follower_count = COALESCE(f.total, 0), u.fan_count = COALESCE(fans.total, 0),
        u.create_time = u.create_time, u.update_time = u.update_time;

    INSERT INTO web_search_record (id, uid, content, search_count, creator, create_time, update_time)
    SELECT MD5(CONCAT(@mock_seed, '-search-', id)), NULL, title, 10 + id * 7, 'mock', @mock_now, @mock_now FROM web_tag ORDER BY id;
    INSERT INTO web_feedback (uid, email, content, status, resolution, create_time, update_time)
    SELECT id, email, CONCAT('测试反馈 ', id, '：希望增加更多分类筛选方式。'), IF(MOD(id, 2) = 0, '1', '0'),
           IF(MOD(id, 2) = 0, '已记录此测试建议。', NULL), @mock_now, @mock_now FROM web_user WHERE id <= 10 ORDER BY id;

    COMMIT;
    DROP TEMPORARY TABLE mock_numbers;
END$$
DELIMITER ;
CALL hongshu_generate_mock_data();
DROP PROCEDURE hongshu_generate_mock_data;

SELECT 'mock data ready' AS result, (SELECT COUNT(*) FROM web_user) AS users,
       (SELECT COUNT(*) FROM web_note) AS notes, (SELECT COUNT(*) FROM web_comment) AS comments,
       (SELECT COUNT(*) FROM web_album) AS albums, (SELECT COUNT(*) FROM web_follow) AS follows,
       (SELECT COUNT(*) FROM web_like_or_collect) AS likes_and_collections;

-- 恢复调用方原会话设置；仅在脚本全部成功执行后运行。
SET SESSION sql_mode = @hongshu_mock_saved_sql_mode;
SET SESSION time_zone = @hongshu_mock_saved_time_zone;
SET SESSION foreign_key_checks = @hongshu_mock_saved_foreign_key_checks;
SET SESSION unique_checks = @hongshu_mock_saved_unique_checks;
SET SESSION auto_increment_increment = @hongshu_mock_saved_auto_increment_increment;
SET SESSION auto_increment_offset = @hongshu_mock_saved_auto_increment_offset;
SET SESSION autocommit = @hongshu_mock_saved_autocommit;
SET SESSION character_set_client = @hongshu_mock_saved_character_set_client;
SET SESSION character_set_connection = @hongshu_mock_saved_character_set_connection;
SET SESSION character_set_results = @hongshu_mock_saved_character_set_results;
SET SESSION collation_connection = @hongshu_mock_saved_collation_connection;
